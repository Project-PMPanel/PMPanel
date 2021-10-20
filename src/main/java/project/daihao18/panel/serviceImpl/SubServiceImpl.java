package project.daihao18.panel.serviceImpl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import project.daihao18.panel.common.utils.*;
import project.daihao18.panel.entity.*;
import project.daihao18.panel.service.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName: SubServiceImpl
 * @Description:
 * @Author: code18
 * @Date: 2020-11-23 14:05
 */
@Service
@Slf4j
public class SubServiceImpl implements SubService {

    @Autowired
    private UserService userService;

    @Autowired
    private SsService ssService;

    @Autowired
    private V2rayService v2rayService;

    @Autowired
    private TrojanService trojanService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private OperateIpService operateIpService;

    @Override
    public String getSubs(String link, String type, HttpServletRequest request) throws IOException {
        // 获取该用户
        User user = userService.getById(CommonUtil.subsDecryptId(link));

        // 无该用户 或者 该用户link不相等 或者 该用户被封禁
        if (ObjectUtil.isEmpty(user) || !user.getLink().equals(CommonUtil.subsLinkDecryptId(link)) || !user.getEnable()) {
            return "";
        }

        // 如果没有uuid,在这里生成并且更新到数据库
        if (ObjectUtil.isEmpty(user.getPasswd())) {
            user.setPasswd(UuidUtil.uuid3(user.getId() + "|" + DateUtil.currentSeconds()));
            userService.updateById(user);
        }
        // 设置订阅链接
        String subUrl = configService.getValueByName("subUrl");
        if (subUrl.endsWith("/")) {
            subUrl += "api/subscription/" + CommonUtil.subsEncryptId(user.getId()) + user.getLink() + "/" + type;
        } else {
            subUrl += "/api/subscription/" + CommonUtil.subsEncryptId(user.getId()) + user.getLink() + "/" + type;
        }
        user.setSubsLink(subUrl);
        // 记录订阅ip
        String ip = IpUtil.getIpAddr(request);
        // 查该ip在不在
        List<OperateIp> lists = operateIpService.list(new QueryWrapper<OperateIp>().eq("ip", ip).eq("user_id", user.getId()));
        if (ObjectUtil.isNotEmpty(lists)) {
            // 存在该ip删除它
            operateIpService.removeByIds(lists.stream().map(OperateIp::getId).collect(Collectors.toList()));
        }
        OperateIp operateIp = new OperateIp();
        operateIp.setIp(ip);
        operateIp.setTime(new Date());
        operateIp.setType(3);
        operateIp.setUserId(user.getId());
        operateIpService.save(operateIp);
        // 根据type开始处理订阅
        switch (type) {
            case "shadowrocket":
                return ShadowrocketUtil.getSub(user);
            case "clash":
                return ClashUtil.getSub(user);
            case "surge4":
                return SurgeUtil.getSub(user);
            case "ss":
                return getSSOriginal(user);
            case "v2ray":
                return getV2rayOriginal(user);
        }
        return null;
    }

    // ##################################################
    // SIP008
    private String getSSOriginal(User user) {
        String subs = "";
        String ssSubs = "";

        // 用户已过期 或者 流量已用完
        if (user.getExpireIn().before(new Date()) || user.getU() + user.getD() > user.getTransferEnable()) {
            Map<String, Object> server = new HashMap<>();
            server.put("id", UuidUtil.uuid3("已过期或流量已用完"));
            server.put("remarks", "已过期或流量已用完");
            server.put("server", "192.168.1.1");
            server.put("server_port", "aes-256-gcm");
            server.put("password", user.getPasswd());
            server.put("method","aes-256-gcm");

            ssSubs += server;
            subs += ssSubs;
            return Base64.getEncoder().encodeToString(subs.getBytes());
        }

        // ss
        List<Ss> ssNodes = ssService.list(new QueryWrapper<Ss>().le("`class`", user.getClazz()).eq("flag", 1));
        // 处理ss
        if (ObjectUtil.isNotEmpty(ssNodes)) {
            Map<String, Object> content = new HashMap<>();
            List<Map<String, Object>> servers = new ArrayList<>();
            content.put("version", "1");
            // 遍历ss节点
            for (Ss ss : ssNodes) {
                Map<String, Object> server = new HashMap<>();
                server.put("id", UuidUtil.uuid3(ss.getName()));
                server.put("remarks", ss.getName());
                server.put("server", ss.getSubServer());
                server.put("server_port", ss.getSubPort());
                server.put("password", user.getPasswd());
                server.put("method",ss.getMethod());

                servers.add(server);
            }
            content.put("servers", servers);
            ssSubs = JSONUtil.toJsonStr(content);
        }
        subs += ssSubs;
        return subs;
    }

    // ##################################################
    // SIP002 vmess
    private String getV2rayOriginal(User user) {
        // v2ray
        List<V2ray> v2rayNodes = v2rayService.list(new QueryWrapper<V2ray>().le("`class`", user.getClazz()).eq("flag", 1));
        String nodes = "";
        String prefix = "vmess://";
        for (V2ray v2ray : v2rayNodes) {
            Map<String, Object> content = new HashMap<>();
            content.put("v", "2");
            content.put("ps", v2ray.getName());
            content.put("add", v2ray.getSubServer());
            content.put("port", v2ray.getSubPort());
            content.put("type", "none");
            content.put("id", user.getPasswd());
            content.put("aid", v2ray.getAlterId());
            content.put("net", v2ray.getNetwork());
            content.put("host", v2ray.getHost());
            content.put("path", v2ray.getPath());
            // 添加tls
            content.put("tls", v2ray.getSecurity() == "none" ? "" : v2ray.getSecurity());
            // 拼接所有节点
            nodes += prefix + Base64.getEncoder().encodeToString(JSONUtil.toJsonStr(content).getBytes()) + "\n";
        }
        return Base64.getEncoder().encodeToString(nodes.getBytes());
    }

    // ##################################################
    // Other
}