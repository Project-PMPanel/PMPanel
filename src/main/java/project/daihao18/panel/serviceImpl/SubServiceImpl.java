package project.daihao18.panel.serviceImpl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import project.daihao18.panel.common.utils.CommonUtil;
import project.daihao18.panel.common.utils.FlowSizeConverterUtil;
import project.daihao18.panel.common.utils.IpUtil;
import project.daihao18.panel.common.utils.UuidUtil;
import project.daihao18.panel.entity.*;
import project.daihao18.panel.service.*;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
                return getShadowrocketSub(user);
            case "clash":
                return getClashSub(user);
            case "surge4":
                return getSurge4Sub(user);
            case "ss":
                return getSSOriginal(user);
            case "v2ray":
                return getV2rayOriginal(user);
        }
        return null;
    }


    // ##################################################
    // Shadowrocket

    /**
     * 小火箭订阅
     *
     * @param user
     * @return
     */
    private String getShadowrocketSub(User user) throws IOException {
        String subs = "";
        String ssSubs = "";
        String v2raySubs = "";
        String trojanSubs = "";

        // 该节点的group用站点名称
        String group = configService.getValueByName("siteName");
        subs = "STATUS=剩余流量:" + FlowSizeConverterUtil.BytesToGb(user.getTransferEnable() - user.getU() - user.getD()) + "GB.过期时间:" + DateUtil.format(user.getExpireIn(), "yyyy-MM-dd HH:mm:ss") + "\n" + "REMARKS=" + group + "\n";

        // 用户已过期 或者 流量已用完
        if (user.getExpireIn().before(new Date()) || user.getU() + user.getD() > user.getTransferEnable()) {
            Ss ss = new Ss();
            ss.setName("已过期或流量已用完");
            ss.setMethod("aes-256-gcm");
            ss.setSubServer("192.168.1.1");
            ss.setSubPort(8080);
            ssSubs += getShadowrocketSsLink(ss, user.getPasswd());
            subs += ssSubs + v2raySubs + trojanSubs;
            return Base64.getEncoder().encodeToString(subs.getBytes());
        }

        // ss
        List<Ss> ssNodes = ssService.list(new QueryWrapper<Ss>().le("`class`", user.getClazz()).eq("flag", 1));
        // 处理ss
        if (ObjectUtil.isNotEmpty(ssNodes)) {
            // 遍历ss节点
            for (Ss ss : ssNodes) {
                ssSubs += getShadowrocketSsLink(ss, user.getPasswd());
            }
        }
        // v2ray
        List<V2ray> v2rayNodes = v2rayService.list(new QueryWrapper<V2ray>().le("`class`", user.getClazz()).eq("flag", 1));
        // 处理v2ray
        if (ObjectUtil.isNotEmpty(v2rayNodes)) {
            // 遍历v2ray节点
            for (V2ray v2ray : v2rayNodes) {
                v2raySubs += getShadowrocketV2rayLink(v2ray, user.getPasswd());
            }
        }
        // trojan
        List<Trojan> trojanNodes = trojanService.list(new QueryWrapper<Trojan>().le("`class`", user.getClazz()).eq("flag", 1));
        // 处理trojan
        if (ObjectUtil.isNotEmpty(trojanNodes)) {
            // 遍历v2ray节点
            for (Trojan trojan : trojanNodes) {
                trojanSubs += getShadowrocketTrojanLink(trojan, user.getPasswd());
            }
        }
        // 合并订阅
        subs += ssSubs + v2raySubs + trojanSubs;
        return Base64.getEncoder().encodeToString(subs.getBytes());
    }

    private String getShadowrocketSsLink(Ss ss, String passwd) {
        String prefix = Base64.getUrlEncoder().encodeToString((ss.getMethod() + ":" + passwd + "@" + ss.getSubServer() + ":" + ss.getSubPort()).getBytes(StandardCharsets.UTF_8));
        while (prefix.endsWith("=")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        String link = prefix + "?remarks=";
        String suffix = ss.getName();
        link = link + URLUtil.encode(suffix, "UTF-8");
        return "ss://" + link + "\n";
    }

    private String getShadowrocketV2rayLink(V2ray v2ray, String uuid) {
        String prefix = Base64.getUrlEncoder().encodeToString(("chacha20-poly1305:" + uuid + "@" + v2ray.getSubServer() + ":" + v2ray.getSubPort()).getBytes(StandardCharsets.UTF_8));
        while (prefix.endsWith("=")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        String link = prefix + "?remarks=";
        String suffix = v2ray.getName();
        if (ObjectUtil.isNotEmpty(v2ray.getHost())) {
            suffix += "&obfsParam=" + v2ray.getHost();
        }
        if (ObjectUtil.isNotEmpty(v2ray.getPath())) {
            suffix += "&path=" + v2ray.getPath();
        }
        if (ObjectUtil.isNotEmpty(v2ray.getNetwork())) {
            if ("ws".equals(v2ray.getNetwork())) {
                suffix += "&obfs=websocket";
            }
        }
        if (ObjectUtil.isNotEmpty(v2ray.getSecurity())) {
            if ("tls".equals(v2ray.getSecurity())) {
                suffix += "&tls=1";
            }
        }
        link = link + URLUtil.encode(suffix, "UTF-8");
        return "vmess://" + link + "\n";
    }

    private String getShadowrocketTrojanLink(Trojan trojan, String passwd) {
        String prefix = passwd + "@" + trojan.getSubServer() + ":" + trojan.getSubPort();
        String link = prefix + "?remarks=";
        String suffix = trojan.getName();
        link = link + URLUtil.encode(suffix, "UTF-8");
        return "trojan://" + link + "\n";
    }

    // ##################################################
    // Clash

    /**
     * 获取clash订阅
     *
     * @param user
     * @return
     */
    private String getClashSub(User user) throws IOException {
        // 处理订阅
        // ss
        List<Ss> ssNodes = ssService.list(new QueryWrapper<Ss>().le("`class`", user.getClazz()).eq("flag", 1));

        StringBuilder node = new StringBuilder();
        StringBuilder nodeName = new StringBuilder();
        if (ObjectUtil.isNotEmpty(ssNodes)) {
            // ss节点不为空
            for (Ss ss : ssNodes) {
                node.append(getClashSsLink(ss, user.getPasswd()));
                nodeName.append("      - " + ss.getName() + "\n");
            }
        }
        // v2ray
        List<V2ray> v2rayNodes = v2rayService.list(new QueryWrapper<V2ray>().le("`class`", user.getClazz()).eq("flag", 1));
        if (ObjectUtil.isNotEmpty(v2rayNodes)) {
            // 遍历v2ray节点
            for (V2ray v2ray : v2rayNodes) {
                node.append(getClashV2rayLink(v2ray, user.getPasswd()));
                nodeName.append("      - " + v2ray.getName() + "\n");
            }
        }
        // trojan
        List<Trojan> trojanNodes = trojanService.list(new QueryWrapper<Trojan>().le("`class`", user.getClazz()).eq("flag", 1));
        // 处理trojan
        if (ObjectUtil.isNotEmpty(trojanNodes)) {
            // 遍历v2ray节点
            for (Trojan trojan : trojanNodes) {
                node.append(getClashTrojanLink(trojan, user.getPasswd()));
                nodeName.append("      - " + trojan.getName() + "\n");
            }
        }
        ClassPathResource classPathResource = new ClassPathResource("config/clash");
        BufferedReader bfreader = new BufferedReader(new InputStreamReader(classPathResource.getInputStream(), "UTF-8"));
        StringBuilder builder = new StringBuilder();
        String tmpContent = null;
        String rule = "";
        while ((tmpContent = bfreader.readLine()) != null) {
            if (tmpContent.equals("proxies:")) {
                builder.append(tmpContent + "\n");
                builder.append(node);
                continue;
            }
            if (tmpContent.equals("      {node}")) {
                builder.append(nodeName);
                continue;
            }
            builder.append(tmpContent + "\n");
        }
        bfreader.close();
        return builder.toString();
    }

    private String getClashSsLink(Ss ssNode, String passwd) {
        String ss = "  - ";
        ss += "{name: " + ssNode.getName() + ", ";
        ss += "type: ss, ";
        ss += "server: " + ssNode.getSubServer() + ", ";
        ss += "port: " + ssNode.getSubPort() + ", ";
        ss += "cipher: " + ssNode.getMethod() + ", ";
        ss += "password: " + passwd + ", ";
        ss += "udp: " + true + "}\n";
        return ss;
    }

    /*
    private String getClashMuSSRLink(SsNode ssrNode, String obfsParam, String protocolParam) {
        *//*Map<String, Object> ssr = new HashMap<>();
        ssr.put("name", ssrNode.getName());
        ssr.put("type", "ssr");
        ssr.put("server", ssrNode.getServer().split(";")[0]);
        ssr.put("port", ssrNode.getServer().split("#")[1]);
        ssr.put("cipher", ssrNode.getMethod());
        ssr.put("password", ssrNode.getPasswd());
        ssr.put("protocol", ssrNode.getProtocol());
        ssr.put("protocol-param", protocolParam);
        ssr.put("obfs", ssrNode.getObfs());
        ssr.put("obfs-param", obfsParam);
        return "  - " + JSONUtil.toJsonStr(ssr).replace("\"", "").replace("\'", "") + "\n";*//*
        String ssr = "  - \n";
        ssr += "    name: " + ssrNode.getName() + "\n";
        ssr += "    type: " + "ssr\n";
        ssr += "    server: " + ssrNode.getServer().split(";")[0] + "\n";
        ssr += "    port: " + ssrNode.getServer().split("#")[1] + "\n";
        ssr += "    cipher: " + ssrNode.getMethod() + "\n";
        ssr += "    password: " + ssrNode.getPasswd() + "\n";
        ssr += "    protocol: " + ssrNode.getProtocol() + "\n";
        ssr += "    protocol-param: " + protocolParam + "\n";
        ssr += "    obfs: " + ssrNode.getObfs() + "\n";
        ssr += "    obfs-param: " + obfsParam + "\n";
        return ssr;
    }*/

    private String getClashV2rayLink(V2ray v2rayNode, String uuid) {
        String v2ray = "  - ";
        v2ray += "{name: " + v2rayNode.getName() + ", ";
        v2ray += "type: vmess, ";
        v2ray += "server: " + v2rayNode.getSubServer() + ", ";
        v2ray += "port: " + v2rayNode.getSubPort() + ", ";
        v2ray += "uuid: " + uuid + ", ";
        v2ray += "alterId: " + v2rayNode.getAlterId() + ", ";
        v2ray += "cipher: " + "auto" + ", ";
        if (ObjectUtil.isNotEmpty(v2rayNode.getSni()) && !"grpc".equals(v2rayNode.getNetwork())) {
            v2ray += "servername: " + v2rayNode.getSni();
        }
        String host = ObjectUtil.isNotEmpty(v2rayNode.getHost()) ? v2rayNode.getHost() : v2rayNode.getSubServer();
        if ("ws".equals(v2rayNode.getNetwork())) {
            v2ray += "network: " + "ws, ";
            v2ray += "ws-path: " + v2rayNode.getPath() + ", ";
            v2ray += "ws-headers: {Host: " + host + "}, ";
        }
        if ("grpc".equals(v2rayNode.getNetwork())) {
            v2ray += "network: " + "grpc, ";
            v2ray += "servername: " + host + ", ";
            // $return['grpc-opts']['grpc-service-name'] = ($item['servicename'] != '' ? $item['servicename'] : "");
            v2ray += "grpc-opts: {grpc-service-name: " + "" + "}, ";
        }
        // TODO verify_cert
        if ("tls".equals(v2rayNode.getSecurity())) {
            v2ray += "tls: " + true + ", ";
            // v2ray += "skip-cert-verify: " + true + ", ";
        }
        v2ray += "udp: " + true + "}\n";
        return v2ray;
    }

    private String getClashTrojanLink(Trojan trojanNode, String passwd) {
        String trojan = "  - ";
        trojan += "{name: " + trojanNode.getName() + ", ";
        trojan += "type: trojan, ";
        trojan += "server: " + trojanNode.getSubServer() + ", ";
        trojan += "port: " + trojanNode.getSubPort() + ", ";
        trojan += "password: " + passwd + ", ";
        trojan += "sni: " + trojanNode.getSni() + ", ";
        trojan += "cipher: " + "auto" + ", ";
        if (trojanNode.getGrpc()) {
            trojan += "network: " + "grpc, ";
            // $return['grpc-opts']['grpc-service-name'] = ($item['servicename'] != '' ? $item['servicename'] : "");
            trojan += "grpc-opts: {grpc-service-name: " + "" + "}, ";
        }
        trojan += "udp: " + true + "}\n";
        return trojan;
    }

    // ##################################################
    // Surge

    /**
     * 获取Surge 3,4订阅
     *
     * @param user
     * @return
     */
    private String getSurge4Sub(User user) throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("config/surge4");
        BufferedReader bfreader = new BufferedReader(new InputStreamReader(classPathResource.getInputStream(), "UTF-8"));
        StringBuilder builder = new StringBuilder();
        StringBuilder nodeName = new StringBuilder();
        // 开始处理订阅内容
        builder.append("#!MANAGED-CONFIG " + user.getSubsLink() + "\n\n");
        builder.append("#---------------------------------------------------#\n");
        builder.append("## 上次更新于：" + DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + "\n");
        builder.append("#---------------------------------------------------#\n\n");
        String tmpContent = null;
        while ((tmpContent = bfreader.readLine()) != null) {
            if (tmpContent.equals("[Proxy]")) {
                builder.append(tmpContent + "\n");
                builder.append("DIRECT = direct\n");
                // ss
                List<Ss> ssNodes = ssService.list(new QueryWrapper<Ss>().le("`class`", user.getClazz()).eq("flag", 1));
                if (ObjectUtil.isNotEmpty(ssNodes)) {
                    // ss 节点不为空,遍历单端口信息
                    for (Ss ssNode : ssNodes) {
                        builder.append(ssNode.getName() + " = ss, " +
                                ssNode.getSubServer() + ", " +
                                ssNode.getSubPort() + ", " +
                                "encrypt-method=" + ssNode.getMethod() + ", " +
                                "password=" + user.getPasswd() + ", udp-relay=true\n");
                        nodeName.append(ssNode.getName() + ", ");
                    }
                }
                // v2ray
                List<V2ray> v2rayNodes = v2rayService.list(new QueryWrapper<V2ray>().le("`class`", user.getClazz()).eq("flag", 1));
                if (ObjectUtil.isNotEmpty(v2rayNodes)) {
                    // 遍历v2ray节点
                    for (V2ray v2ray : v2rayNodes) {
                        builder.append(
                                v2ray.getName() + " = vmess, " + v2ray.getSubServer() + ", " + v2ray.getSubPort() + ", username = " + user.getPasswd()
                        );
                        if ("tls".equals(v2ray.getSecurity())) {
                            builder.append(", tls=true");
                        }
                        if ("ws".equals(v2ray.getNetwork())) {
                            builder.append(", ws=true, ws-path=" + v2ray.getPath() + ", ws-headers=host:" + v2ray.getHost() + "\n");
                        }
                        nodeName.append(v2ray.getName() + ", ");
                    }
                }
                // trojan
                List<Trojan> trojanNodes = trojanService.list(new QueryWrapper<Trojan>().le("`class`", user.getClazz()).eq("flag", 1));
                // 处理trojan
                if (ObjectUtil.isNotEmpty(trojanNodes)) {
                    // 遍历v2ray节点
                    for (Trojan trojan : trojanNodes) {
                        builder.append(
                                trojan.getName() + " = trojan, " + trojan.getSubServer() + ", " + trojan.getSubPort() + ", password = " + user.getPasswd()
                        );
                        nodeName.append(trojan.getName() + ", ");
                    }
                }
                // 删除nodeName最后的,和空格
                nodeName.deleteCharAt(nodeName.length() - 1);
                nodeName.deleteCharAt(nodeName.length() - 1);
                nodeName.append("\n");
                continue;
            } else if (tmpContent.contains("{node}")) {
                builder.append(tmpContent);
                // 删除{node}
                builder.delete(builder.length() - 6, builder.length());
                // 添加节点名称
                builder.append(nodeName);
                continue;
            }
            builder.append(tmpContent + "\n");
        }
        bfreader.close();
        return builder.toString();
    }

    // ##################################################
    // SIP008
    private String getSSOriginal(User user) {
        String subs = "";
        String ssSubs = "";

        // 用户已过期 或者 流量已用完
        if (user.getExpireIn().before(new Date()) || user.getU() + user.getD() > user.getTransferEnable()) {
            Ss ss = new Ss();
            ss.setName("已过期或流量已用完");
            ss.setMethod("aes-256-gcm");
            ss.setSubServer("192.168.1.1");
            ss.setSubPort(8080);
            ssSubs += getSSOriginal(ss, user.getPasswd());
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
                servers.add(getSSOriginal(ss, user.getPasswd()));
            }
            content.put("servers", servers);
            ssSubs = JSONUtil.toJsonStr(content);
        }
        subs += ssSubs;
        return subs;
    }

    private Map<String, Object> getSSOriginal(Ss ss, String passwd) {
        Map<String, Object> server = new HashMap<>();
        server.put("id", UuidUtil.uuid3(ss.getName()));
        server.put("remarks", ss.getName());
        server.put("server", ss.getSubServer());
        server.put("server_port", ss.getSubPort());
        server.put("password", passwd);
        server.put("method",ss.getMethod());
        return server;

        /*String prefix = Base64.getUrlEncoder().encodeToString((ss.getMethod() + ":" + passwd).getBytes()) + "@" + ss.getSubServer() + ":" + ss.getSubPort() + "#";
        String link = prefix + URLUtil.encode(ss.getName(), "UTF-8");
        return "ss://" + link + "\n";*/
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