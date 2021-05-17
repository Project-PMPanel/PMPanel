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
import org.springframework.util.DigestUtils;
import project.daihao18.panel.common.utils.CommonUtil;
import project.daihao18.panel.common.utils.FlowSizeConverterUtil;
import project.daihao18.panel.common.utils.IpUtil;
import project.daihao18.panel.common.utils.UuidUtil;
import project.daihao18.panel.entity.OperateIp;
import project.daihao18.panel.entity.SsNode;
import project.daihao18.panel.entity.User;
import project.daihao18.panel.service.*;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
    private SsNodeService ssNodeService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private OperateIpService operateIpService;

    @Override
    public String getSubs(String link, String type, HttpServletRequest request) throws IOException {
        // 获取该用户
        User user = userService.getById(CommonUtil.subsDecryptId(link));
        // 无该用户 或者 该用户link不相等 或者 该用户被封禁 用户已过期 或者 流量已用完
        if (ObjectUtil.isEmpty(user) || !user.getLink().equals(CommonUtil.subsLinkDecryptId(link)) || !user.getEnable() || user.getExpireIn().before(new Date()) || user.getU() + user.getD() > user.getTransferEnable()) {
            return null;
        }
        List<SsNode> ssNodeList = getEnableNodes(link);
        if (ObjectUtil.isEmpty(ssNodeList)) {
            return null;
        }
        // 如果没有uuid,在这里生成并且更新到数据库
        if (ObjectUtil.isEmpty(user.getUuid())) {
            user.setUuid(UuidUtil.uuid3(user.getId() + "|" + user.getPasswd()));
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
        // ss
        List<SsNode> ssNodes = ssNodeList.stream().filter(node -> node.getSort() == 0 && user.getNodeGroup().equals(node.getNodeGroup()) && user.getClazz() >= node.getNodeClass()).collect(Collectors.toList());
        // v2ray
        List<SsNode> v2rayNodes = ssNodeList.stream().filter(node -> node.getSort() == 11 && user.getNodeGroup().equals(node.getNodeGroup()) && user.getClazz() >= node.getNodeClass()).collect(Collectors.toList());
        // 根据type开始处理订阅
        switch (type) {
            case "shadowrocket":
                return getShadowrocketSub(ssNodes, v2rayNodes, user);
            case "clash":
                return getClashSub(ssNodes, v2rayNodes, user);
            case "surge4":
                return getSurge4Sub(ssNodes, v2rayNodes, user);
            case "v2ray":
                return getV2rayOriginal(v2rayNodes, user);
        }
        return null;
    }

    @Override
    public List<SsNode> getEnableNodes(String link) {
        // 获取该用户所有可用节点,根据组,等级来获取
        List<SsNode> ssNodeList = ssNodeService.listEnableNodes();
        return ssNodeList;
    }

    @Override
    public List<Map<String, Object>> getSSList(String link) {
        // 获取该用户
        User user = userService.getById(CommonUtil.subsDecryptId(link));
        // 无该用户 或者 该用户link不相等 或者 该用户被封禁 用户已过期 或者 流量已用完
        if (ObjectUtil.isEmpty(user) || !user.getLink().equals(CommonUtil.subsLinkDecryptId(link)) || !user.getEnable() || user.getExpireIn().before(new Date()) || user.getU() + user.getD() > user.getTransferEnable()) {
            return null;
        }
        List<SsNode> nodes = getEnableNodes(link);
        List<SsNode> ssNodes = nodes.stream().filter(node -> node.getSort() == 0 && user.getNodeGroup().equals(node.getNodeGroup()) && user.getClazz() >= node.getNodeClass()).collect(Collectors.toList());
        List<Map<String, Object>> ssList = new ArrayList<>();
        // 该节点的group用站点名称
        String group = configService.getValueByName("siteName");
        // 用户特征码前5位 + 混淆参数后缀域名 -> 单端口用户识别参数
        String obfsParam = DigestUtils.md5DigestAsHex((user.getId().toString() + user.getPasswd() + user.getMethod() + user.getObfs() + user.getProtocol()).getBytes()).substring(0, 5) + user.getId() + "." + configService.getValueByName("muSuffix");
        String protocolParam = user.getId().toString() + ":" + user.getPasswd();
        ssNodes.forEach(ss -> {
            Map<String, Object> map = new HashMap<>();
            map.put("type", "ss");
            map.put("address", ss.getServer().split(";")[0]);
            map.put("port", ss.getServer().split("#")[1]);
            map.put("protocol", ss.getProtocol());
            map.put("protocol_param", protocolParam);
            map.put("obfs", ss.getObfs().split("_")[2]);
            map.put("obfs_param", obfsParam);
            map.put("passwd", ss.getPasswd());
            map.put("method", ss.getMethod());
            map.put("remark", ss.getName());
            map.put("group", group);
            ssList.add(map);
        });
        return ssList;
    }

    @Override
    public List<Map<String, Object>> getV2rayList(String link) {
        // 获取该用户
        User user = userService.getById(CommonUtil.subsDecryptId(link));
        // 无该用户 或者 该用户link不相等 或者 该用户被封禁 用户已过期 或者 流量已用完
        if (ObjectUtil.isEmpty(user) || !user.getLink().equals(CommonUtil.subsLinkDecryptId(link)) || !user.getEnable() || user.getExpireIn().before(new Date()) || user.getU() + user.getD() > user.getTransferEnable()) {
            return null;
        }
        List<SsNode> nodes = getEnableNodes(link);
        // v2ray
        List<SsNode> v2rayNodes = nodes.stream().filter(node -> node.getSort() == 11 && user.getNodeGroup().equals(node.getNodeGroup()) && user.getClazz() >= node.getNodeClass()).collect(Collectors.toList());
        List<Map<String, Object>> v2rayList = new ArrayList<>();
        v2rayNodes.forEach(v2ray -> {
            String[] node = v2ray.getServer().split(";");
            String server = node[0];
            String port = node[1];
            String alterId = node[2];
            String protocol = node[3];
            String path = "";
            String host = "";
            String[] extra = node[5].split("\\|");
            for (int i = 0; i < extra.length; i++) {
                if (extra[i].startsWith("inside_port")) {
                    if (ObjectUtil.isEmpty(port)) {
                        port = extra[i].replace("inside_port=", "");
                    }
                } else if (extra[i].startsWith("outside_port")) {
                    port = extra[i].replace("outside_port=", "");
                } else if (extra[i].startsWith("path")) {
                    path = extra[i].replace("path=", "");
                } else if (extra[i].startsWith("host")) {
                    host = extra[i].replace("host=", "");
                } else if (extra[i].startsWith("server")) {
                    server = extra[i].replace("server=", "");
                }
            }
            Map<String, Object> map = new HashMap<>();
            map.put("type", "vmess");
            map.put("add", server);
            map.put("port", port);
            map.put("aid", alterId);
            map.put("net", "tcp");
            map.put("headerType", "none");
            if (node.length >= 4) {
                map.put("net", protocol);
                if ("ws".equals(map.get("net"))) {
                    map.put("path", "/");
                } else if ("tls".equals(map.get("net"))) {
                    map.put("tls", "tls");
                }
            }
            if (node.length >= 5) {
                List<String> list = Arrays.asList("kcp", "http", "mkcp");
                if (list.contains(map.get("net"))) {
                    map.put("headerType", node[4]);
                } else if ("ws".equals(node[4])) {
                    map.put("net", "ws");
                } else if ("tls".equals(node[4])) {
                    map.put("tls", "tls");
                }
            }
            if (node.length >= 6 && ObjectUtil.isNotEmpty(node[5])) {
                map.put("host", host);
                map.put("path", path);
                map.put("tls", "");
            }
            map.put("remark", v2ray.getName());
            map.put("id", user.getUuid());
            v2rayList.add(map);
        });
        return v2rayList;
    }

    // ##################################################
    // Shadowrocket

    /**
     * 小火箭订阅
     *
     * @param ssNodes
     * @param v2rayNodes
     * @param user
     * @return
     */
    private String getShadowrocketSub(List<SsNode> ssNodes, List<SsNode> v2rayNodes, User user) throws IOException {
        // 存在ss或ssr单端口节点
        String subs = "";
        String ssSubs = "";
        String ssrSubs = "";
        String v2raySubs = "";
        // 该节点的group用站点名称
        String group = configService.getValueByName("siteName");
        // 处理ss或ssr
        if (ObjectUtil.isNotEmpty(ssNodes)) {
            // 用户特征码前5位 + 混淆参数后缀域名 -> 单端口用户识别参数
            String obfsParam = DigestUtils.md5DigestAsHex((user.getId().toString() + user.getPasswd() + user.getMethod() + user.getObfs() + user.getProtocol()).getBytes()).substring(0, 5) + user.getId() + "." + configService.getValueByName("muSuffix");
            String protocolParam = user.getId().toString() + ":" + user.getPasswd();
            // ss 或 ssr节点不为空,遍历单端口信息
            for (SsNode ssNode : ssNodes) {
                // 给该mu计算小火箭的ss订阅链接
                if (ssNode.getObfs().startsWith("simple_obfs") || ObjectUtil.isEmpty(ssNode.getObfs())) {
                    // 该节点是ss单端口节点
                    ssSubs += getShadowrocketMuSSLink(ssNode, obfsParam, group, user.getUuid());
                } else {
                    // 该节点是ssr单端口节点
                    ssrSubs += getShadowrocketMuSSRLink(ssNode, obfsParam, protocolParam, group);
                }
            }
        }
        // 处理v2ray
        if (ObjectUtil.isNotEmpty(v2rayNodes)) {
            // 遍历v2ray节点
            for (SsNode v2ray : v2rayNodes) {
                v2raySubs += getShadowrocketV2rayLink(v2ray, user.getUuid());
            }
        }
        subs = "STATUS=剩余流量:" + FlowSizeConverterUtil.BytesToGb(user.getTransferEnable() - user.getU() - user.getD()) + "GB.过期时间:" + DateUtil.format(user.getExpireIn(), "yyyy-MM-dd HH:mm:ss") + "\n" + "REMARKS=" + group + "\n";
        subs += ssSubs + ssrSubs + v2raySubs;
        return Base64.getEncoder().encodeToString(subs.getBytes());
    }

    /**
     * 获取sslink
     *
     * @param node
     * @param obfsParam
     * @param group
     * @return
     */
    private String getShadowrocketMuSSLink(SsNode node, String obfsParam, String group, String uuid) throws UnsupportedEncodingException {
        String link = "";
        if (node.getObfs().startsWith("simple_obfs")) {
            link = Base64.getUrlEncoder().encodeToString((node.getMethod() + ":" + node.getPasswd()).getBytes(StandardCharsets.UTF_8)) + "@" +
                    node.getServer().split(";")[0] + ":" +
                    node.getServer().split("#")[1] +
                    "/?plugin=obfs-local";
            String suffix = URLEncoder.encode(";obfs=" + node.getObfs().split("_")[2] + ";" +
                    "obfs-host=" + obfsParam, "UTF-8") + "&group=" + Base64.getUrlEncoder().encodeToString(group.getBytes()) + "#" + URLEncoder.encode(node.getName(), "UTF-8");
            link += suffix;
        }
        if (ObjectUtil.isEmpty(node.getObfs())) {
            // 这里passwd用的是和v2ray一样的uuid
            link = Base64.getUrlEncoder().encodeToString((node.getMethod() + ":" + uuid).getBytes(StandardCharsets.UTF_8)) + "@" +
                    node.getServer().split(";")[0] + ":" +
                    node.getServer().split("#")[1];
        }
        return "ss://" + link + "\n";
    }

    /**
     * 获取ssrlink
     *
     * @param node
     * @param obfsParam
     * @param protocolParam
     * @param group
     * @return
     */
    private String getShadowrocketMuSSRLink(SsNode node, String obfsParam, String protocolParam, String group) {
        String link = node.getServer().split(";")[0] + ":" +
                node.getServer().split("#")[1] + ":" +
                node.getProtocol() + ":" +
                node.getMethod() + ":" +
                node.getObfs() + ":" +
                Base64.getUrlEncoder().encodeToString(node.getPasswd().getBytes(StandardCharsets.UTF_8)) +
                "/?remarks=" +
                Base64.getUrlEncoder().encodeToString(node.getName().getBytes(StandardCharsets.UTF_8)) +
                "&group=" +
                Base64.getUrlEncoder().encodeToString(group.getBytes(StandardCharsets.UTF_8));
        // 判断是协议式还是混淆式承载
        if (node.getIsMultiUser() == 1) {
            // 混淆式
            link += "&obfsparam=" + Base64.getUrlEncoder().encodeToString(obfsParam.getBytes(StandardCharsets.UTF_8));
        } else if (node.getIsMultiUser() == 2) {
            // 协议式
            link += "&protoparam=" + Base64.getUrlEncoder().encodeToString(protocolParam.getBytes(StandardCharsets.UTF_8)) +
                    // 协议式最好也填一个混淆参数,可有可无,最好有
                    "&obfsparam=" + Base64.getUrlEncoder().encodeToString(obfsParam.getBytes(StandardCharsets.UTF_8));
        }

        return "ssr://" + Base64.getUrlEncoder().encodeToString(link.getBytes(StandardCharsets.UTF_8)) + "\n";
    }

    private String getShadowrocketV2rayLink(SsNode v2ray, String uuid) {
        String[] node = v2ray.getServer().split(";");
        String server = node[0];
        String port = node[1];
        String alterId = node[2];
        String protocol = node[3];
        String path = "";
        String host = "";
        String[] extra = node[5].split("\\|");
        for (int i = 0; i < extra.length; i++) {
            if (extra[i].startsWith("inside_port")) {
                if (ObjectUtil.isEmpty(port)) {
                    port = extra[i].replace("inside_port=", "");
                }
            } else if (extra[i].startsWith("outside_port")) {
                port = extra[i].replace("outside_port=", "");
            } else if (extra[i].startsWith("path")) {
                path = extra[i].replace("path=", "");
            } else if (extra[i].startsWith("host")) {
                host = extra[i].replace("host=", "");
            } else if (extra[i].startsWith("server")) {
                server = extra[i].replace("server=", "");
            }
        }

        String prefix = Base64.getUrlEncoder().encodeToString(("chacha20-poly1305:" + uuid + "@" + server + ":" + port).getBytes(StandardCharsets.UTF_8));
        while (prefix.endsWith("=")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        String link = prefix + "?remarks=";
        String suffix = v2ray.getName();
        if (ObjectUtil.isNotEmpty(host)) {
            suffix += "&obfsParam=" + host;
        }
        if (ObjectUtil.isNotEmpty(path)) {
            suffix += "&path=" + path;
        }
        if (ObjectUtil.isNotEmpty(protocol)) {
            if ("ws".equals(protocol)) {
                suffix += "&obfs=websocket";
            }
        }
        link = link + URLUtil.encode(suffix, "UTF-8");
        return "vmess://" + link + "\n";
    }

    // ##################################################
    // Clash

    /**
     * 获取clash订阅
     *
     * @param ssNodes
     * @param v2rayNodes
     * @param user
     * @return
     */
    private String getClashSub(List<SsNode> ssNodes, List<SsNode> v2rayNodes, User user) throws IOException {
        /*
                port: 7890
                socks-port: 7891
                allow-lan: false
                mode: Rule
                log-level: silent
                external-controller: '0.0.0.0:9090'
                secret: ''
                proxies:
        * */
        // 处理订阅
        // 获取节点
        // 处理ss或ssr
        StringBuilder node = new StringBuilder();
        StringBuilder nodeName = new StringBuilder();
        if (ObjectUtil.isNotEmpty(ssNodes)) {
            // 用户特征码前5位 + 混淆参数后缀域名 -> 单端口用户识别参数
            String obfsParam = DigestUtils.md5DigestAsHex((user.getId().toString() + user.getPasswd() + user.getMethod() + user.getObfs() + user.getProtocol()).getBytes()).substring(0, 5) + user.getId() + "." + configService.getValueByName("muSuffix");
            String protocolParam = user.getId().toString() + ":" + user.getPasswd();
            // ss 或 ssr节点不为空,遍历单端口信息
            for (SsNode ssNode : ssNodes) {
                // 给该mu计算小火箭的ss订阅链接
                if (ssNode.getObfs().startsWith("simple_obfs") || ObjectUtil.isEmpty(ssNode.getObfs())) {
                    // 该单端口是ss单端口节点
                    node.append(getClashMuSSLink(ssNode, obfsParam, user.getUuid()));
                    nodeName.append("      - " + ssNode.getName() + "\n");
                } else {
                    // 该单端口是ssr单端口节点
                    node.append(getClashMuSSRLink(ssNode, obfsParam, protocolParam));
                    nodeName.append("      - " + ssNode.getName() + "\n");
                }
            }
        }
        if (ObjectUtil.isNotEmpty(v2rayNodes)) {
            // 遍历v2ray节点
            for (SsNode v2ray : v2rayNodes) {
                node.append(getClashV2rayLink(v2ray, user.getUuid()));
                nodeName.append("      - " + v2ray.getName() + "\n");
            }
        }
        ClassPathResource classPathResource = new ClassPathResource("config/clash");
        BufferedReader bfreader = new BufferedReader(new InputStreamReader(classPathResource.getInputStream(), "UTF-8"));
        StringBuilder builder = new StringBuilder();
        String tmpContent = null;
        Boolean flag = true;
        while ((tmpContent = bfreader.readLine()) != null) {
            builder.append(tmpContent + "\n");
            if (tmpContent.equals("proxies:")) {
                builder.append(node);
            }
            if (tmpContent.equals("    name: \uD83D\uDD30国外流量")) {
                flag = false;
            }
            if (tmpContent.equals("    proxies:")) {
                if (flag) {
                    builder.append("      - \uD83D\uDD30国外流量\n");
                }
                builder.append(nodeName);
                flag = true;
            }
        }
        bfreader.close();
        return builder.toString();
    }

    private String getClashMuSSLink(SsNode ssNode, String obfsParam, String uuid) {
        /*Map<String, Object> ss = new HashMap<>();
        ss.put("name", ssNode.getName());
        ss.put("type", "ss");
        ss.put("server", ssNode.getServer().split(";")[0]);
        ss.put("port", ssNode.getServer().split("#")[1]);
        ss.put("cipher", ssNode.getMethod());
        ss.put("password", ssNode.getPasswd());
        ss.put("udp", true);
        ss.put("plugin", "obfs");
        Map<String, Object> pluginOpts = new HashMap<>();
        pluginOpts.put("mode", ssNode.getObfs().split("_")[2]);
        pluginOpts.put("host", obfsParam);
        ss.put("plugin-opts", pluginOpts);
        return "  - " + JSONUtil.toJsonStr(ss).replace("\"", "").replace("\'", "") + "\n";*/
        String ss = "  - \n";
        ss += "    name: " + ssNode.getName() + "\n";
        ss += "    type: " + "ss\n";
        ss += "    server: " + ssNode.getServer().split(";")[0] + "\n";
        ss += "    port: " + ssNode.getServer().split("#")[1] + "\n";
        ss += "    cipher: " + ssNode.getMethod() + "\n";
        if (ObjectUtil.isNotEmpty(ssNode.getObfs())) {
            ss += "    password: " + ssNode.getPasswd() + "\n";
        } else {
            ss += "    password: " + uuid + "\n";
        }
        ss += "    udp: " + true + "\n";
        if (ssNode.getObfs().startsWith("simple_obfs")) {
            ss += "    plugin: " + "obfs\n";
            ss += "    plugin-opts: " + "\n";
            ss += "      mode: " + ssNode.getObfs().split("_")[2] + "\n";
            ss += "      host: " + obfsParam + "\n";
        }
        return ss;
    }

    private String getClashMuSSRLink(SsNode ssrNode, String obfsParam, String protocolParam) {
        /*Map<String, Object> ssr = new HashMap<>();
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
        return "  - " + JSONUtil.toJsonStr(ssr).replace("\"", "").replace("\'", "") + "\n";*/
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
    }

    private String getClashV2rayLink(SsNode v2rayNode, String uuid) {
        String[] node = v2rayNode.getServer().split(";");
        String server = node[0];
        String port = node[1];
        String alterId = node[2];
        String protocol = node[3];
        String path = "";
        String host = "";
        String[] extra = node[5].split("\\|");
        for (int i = 0; i < extra.length; i++) {
            if (extra[i].startsWith("inside_port")) {
                if (ObjectUtil.isEmpty(port)) {
                    port = extra[i].replace("inside_port=", "");
                }
            } else if (extra[i].startsWith("outside_port")) {
                port = extra[i].replace("outside_port=", "");
            } else if (extra[i].startsWith("path")) {
                path = extra[i].replace("path=", "");
            } else if (extra[i].startsWith("host")) {
                host = extra[i].replace("host=", "");
            } else if (extra[i].startsWith("server")) {
                server = extra[i].replace("server=", "");
            }
        }

        /*Map<String, Object> v2ray = new HashMap<>();
        v2ray.put("name", v2rayNode.getName());
        v2ray.put("type", "vmess");
        v2ray.put("server", server);
        v2ray.put("port", port);
        v2ray.put("uuid", uuid);
        v2ray.put("alterId", alterId);
        v2ray.put("cipher", "auto");
        v2ray.put("udp", true);
        if ("ws".equals(protocol)) {
            v2ray.put("network", "ws");
            v2ray.put("ws-path", path);
            Map<String, Object> headers = new HashMap<>();
            headers.put("Host", host);
            v2ray.put("ws-headers", headers);
        }
        if ("tls".equals(protocol)) {
            // TODO
            v2ray.put("tls", true);
        }
        return "  - " + JSONUtil.toJsonStr(v2ray).replace("\"", "").replace("\'", "") + "\n";*/
        String v2ray = "  - \n";
        v2ray += "    name: " + v2rayNode.getName() + "\n";
        v2ray += "    type: " + "vmess\n";
        v2ray += "    server: " + server + "\n";
        v2ray += "    port: " + port + "\n";
        v2ray += "    uuid: " + uuid + "\n";
        v2ray += "    alterId: " + alterId + "\n";
        v2ray += "    cipher: " + "auto" + "\n";
        v2ray += "    udp: " + true + "\n";
        if ("ws".equals(protocol)) {
            v2ray += "    network: " + "ws\n";
            v2ray += "    ws-path: " + path + "\n";
            v2ray += "    ws-headers: " + "\n";
            v2ray += "      Host: " + host + "\n";
        }
        if ("tls".equals(protocol)) {
            // TODO
            v2ray += "    tls: " + true + "\n";
        }
        return v2ray;
    }

    // ##################################################
    // Surge

    /**
     * 获取Surge 3,4订阅
     *
     * @param ssNodes
     * @param v2rayNodes
     * @param user
     * @return
     */
    private String getSurge4Sub(List<SsNode> ssNodes, List<SsNode> v2rayNodes, User user) throws IOException {
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
            builder.append(tmpContent + "\n");
            if (tmpContent.equals("[Proxy]")) {
                // 处理ss或ssr
                if (ObjectUtil.isNotEmpty(ssNodes)) {
                    // 用户特征码前5位 + 混淆参数后缀域名 -> 单端口用户识别参数
                    String obfsParam = DigestUtils.md5DigestAsHex((user.getId().toString() + user.getPasswd() + user.getMethod() + user.getObfs() + user.getProtocol()).getBytes()).substring(0, 5) + user.getId() + "." + configService.getValueByName("muSuffix");
                    // String protocolParam = user.getId().toString() + ":" + user.getPasswd();
                    // ss 节点不为空,遍历单端口信息
                    for (SsNode ssNode : ssNodes) {
                        // 该单端口是ss单端口节点
                        if (ssNode.getObfs().startsWith("simple_obfs")) {
                            builder.append(
                                    ssNode.getName() + " = ss, " +
                                            ssNode.getServer().split(";")[0] + ", " +
                                            ssNode.getServer().split("#")[1] + ", " +
                                            "encrypt-method=" + ssNode.getMethod() + ", " +
                                            "password=" + ssNode.getPasswd() + ", " +
                                            "obfs=" + ssNode.getObfs().split("_")[2] + ", " +
                                            "obfs-host=" + obfsParam + ", udp-relay=true\n"
                            );
                        } else if (ObjectUtil.isEmpty(ssNode.getObfs())) {
                            builder.append(
                                    ssNode.getName() + " = ss, " +
                                            ssNode.getServer().split(";")[0] + ", " +
                                            ssNode.getServer().split("#")[1] + ", " +
                                            "encrypt-method=" + ssNode.getMethod() + ", " +
                                            "password=" + user.getUuid() + ", udp-relay=true\n"
                            );
                        }

                        nodeName.append(ssNode.getName() + ", ");
                    }
                }
                if (ObjectUtil.isNotEmpty(v2rayNodes)) {
                    // 遍历v2ray节点
                    for (SsNode v2ray : v2rayNodes) {
                        String[] node = v2ray.getServer().split(";");
                        String server = node[0];
                        String port = node[1];
                        String alterId = node[2];
                        String protocol = node[3];
                        String path = "";
                        String host = "";
                        String[] extra = node[5].split("\\|");
                        for (int i = 0; i < extra.length; i++) {
                            if (extra[i].startsWith("inside_port")) {
                                if (ObjectUtil.isEmpty(port)) {
                                    port = extra[i].replace("inside_port=", "");
                                }
                            } else if (extra[i].startsWith("outside_port")) {
                                port = extra[i].replace("outside_port=", "");
                            } else if (extra[i].startsWith("path")) {
                                path = extra[i].replace("path=", "");
                            } else if (extra[i].startsWith("host")) {
                                host = extra[i].replace("host=", "");
                            } else if (extra[i].startsWith("server")) {
                                server = extra[i].replace("server=", "");
                            }
                        }
                        builder.append(
                                v2ray.getName() + " = vmess, " + server + ", " + port + ", username = " + user.getUuid() + ", ws=true, ws-path=" + path + ", ws-headers=host:" + host + "\n"
                        );
                        nodeName.append(v2ray.getName() + ", ");
                    }
                }
                // 删除nodeName最后的,和空格
                nodeName.deleteCharAt(nodeName.length() - 1);
                nodeName.deleteCharAt(nodeName.length() - 1);
                nodeName.append("\n");
            } else if (tmpContent.equals("\uD83D\uDD30国外流量 = select,") || tmpContent.endsWith("select, \uD83D\uDD30国外流量,")) {
                // 删除回车
                builder.deleteCharAt(builder.length() - 1);
                // 在🔰国外流量 = select,增加一个空格
                builder.append(" ");
                // 添加节点名称
                builder.append(nodeName);
            }
        }
        bfreader.close();
        return builder.toString();
    }

    // ##################################################
    // SIP002 vmess
    private String getV2rayOriginal(List<SsNode> v2rayNodes, User user) {
        String nodes = "";
        String prefix = "vmess://";
        for (SsNode v2ray : v2rayNodes) {
            String[] node = v2ray.getServer().split(";");
            String[] extra = node[5].split("\\|");
            Map<String, Object> content = new HashMap<>();
            content.put("v", "2");
            content.put("ps", v2ray.getName());
            content.put("add", extra[1].split("=")[1]);
            content.put("port", Integer.parseInt(node[1]));
            content.put("type", "none");
            content.put("id", user.getUuid());
            content.put("aid", Integer.parseInt(node[2]));
            content.put("net", node[3]);
            content.put("path", extra[0].split("=")[1]);
            content.put("host", extra[2].split("=")[1]);
            // TODO 添加tls
            content.put("tls", "");
            // 拼接所有节点
            nodes += prefix + Base64.getEncoder().encodeToString(JSONUtil.toJsonStr(content).getBytes()) + "\n";
        }
        return Base64.getEncoder().encodeToString(nodes.getBytes());
    }



    // ##################################################
    // Other
}