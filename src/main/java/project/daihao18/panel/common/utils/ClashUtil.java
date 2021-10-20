package project.daihao18.panel.common.utils;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import project.daihao18.panel.entity.Ss;
import project.daihao18.panel.entity.Trojan;
import project.daihao18.panel.entity.User;
import project.daihao18.panel.entity.V2ray;
import project.daihao18.panel.service.ConfigService;
import project.daihao18.panel.service.SsService;
import project.daihao18.panel.service.TrojanService;
import project.daihao18.panel.service.V2rayService;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

@Component
public class ClashUtil {

    @PostConstruct
    private void init() {
        configService = this.autoConfigService;
        ssService = this.autoSsService;
        v2rayService = this.autoV2rayService;
        trojanService = this.autoTrojanService;
    }

    @Autowired
    private ConfigService autoConfigService;
    private static ConfigService configService;

    @Autowired
    private SsService autoSsService;
    private static SsService ssService;

    @Autowired
    private V2rayService autoV2rayService;
    private static V2rayService v2rayService;

    @Autowired
    private TrojanService autoTrojanService;
    private static TrojanService trojanService;

    /**
     * 获取clash订阅
     *
     * @param user
     * @return
     */
    public static String getSub(User user) throws IOException {
        // 处理订阅
        // ss
        List<Ss> ssNodes = ssService.list(new QueryWrapper<Ss>().le("`class`", user.getClazz()).eq("flag", 1));

        StringBuilder node = new StringBuilder();
        StringBuilder nodeName = new StringBuilder();
        if (ObjectUtil.isNotEmpty(ssNodes)) {
            // ss节点不为空
            for (Ss ss : ssNodes) {
                node.append(getSsLink(ss, user.getPasswd()));
                nodeName.append("      - " + ss.getName() + "\n");
            }
        }
        // v2ray
        List<V2ray> v2rayNodes = v2rayService.list(new QueryWrapper<V2ray>().le("`class`", user.getClazz()).eq("flag", 1));
        if (ObjectUtil.isNotEmpty(v2rayNodes)) {
            // 遍历v2ray节点
            for (V2ray v2ray : v2rayNodes) {
                node.append(getV2rayLink(v2ray, user.getPasswd()));
                nodeName.append("      - " + v2ray.getName() + "\n");
            }
        }
        // trojan
        List<Trojan> trojanNodes = trojanService.list(new QueryWrapper<Trojan>().le("`class`", user.getClazz()).eq("flag", 1));
        // 处理trojan
        if (ObjectUtil.isNotEmpty(trojanNodes)) {
            // 遍历v2ray节点
            for (Trojan trojan : trojanNodes) {
                node.append(getTrojanLink(trojan, user.getPasswd()));
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

    private static String getSsLink(Ss ss, String passwd) {
        String link = "  - ";
        link += "{name: " + ss.getName() + ", ";
        link += "type: ss, ";
        link += "server: " + ss.getSubServer() + ", ";
        link += "port: " + ss.getSubPort() + ", ";
        link += "cipher: " + ss.getMethod() + ", ";
        link += "password: " + passwd + ", ";
        link += "udp: " + true + "}\n";
        return link;
    }

    private static String getV2rayLink(V2ray v2ray, String uuid) {
        String link = "  - ";
        link += "{name: " + v2ray.getName() + ", ";
        link += "type: vmess, ";
        link += "server: " + v2ray.getSubServer() + ", ";
        link += "port: " + v2ray.getSubPort() + ", ";
        link += "uuid: " + uuid + ", ";
        link += "alterId: " + v2ray.getAlterId() + ", ";
        link += "cipher: " + "auto" + ", ";
        if ("ws".equals(v2ray.getNetwork())) {
            String host = ObjectUtil.isNotEmpty(v2ray.getHost()) ? v2ray.getHost() : v2ray.getSubServer();
            link += "network: " + "ws, ";
            link += "ws-path: " + v2ray.getPath() + ", ";
            link += "ws-headers: {Host: " + host + "}, ";
        }
        if ("grpc".equals(v2ray.getNetwork())) {
            link += "network: " + "grpc, ";
            link += "servername: " + v2ray.getSubServer() + ", ";
            // $return['grpc-opts']['grpc-service-name'] = ($item['servicename'] != '' ? $item['servicename'] : "");
            String serviceName = v2ray.getSni();
            if (ObjectUtil.isEmpty(serviceName)) {
                link += "grpc-opts: {grpc-service-name: \"\"}, ";
            } else {
                link += "grpc-opts: {grpc-service-name: " + v2ray.getSni() + "}, ";
            }
        }
        // TODO verify_cert
        if ("tls".equals(v2ray.getSecurity())) {
            link += "tls: " + true + ", ";
            // link += "skip-cert-verify: " + true + ", ";
        }
        link += "udp: " + true + "}\n";
        return link;
    }

    private static String getTrojanLink(Trojan trojan, String passwd) {
        String link = "  - ";
        link += "{name: " + trojan.getName() + ", ";
        link += "type: trojan, ";
        link += "server: " + trojan.getSubServer() + ", ";
        link += "port: " + trojan.getSubPort() + ", ";
        link += "password: " + passwd + ", ";
        link += "sni: " + trojan.getSni() + ", ";
        link += "cipher: " + "auto" + ", ";
        if (trojan.getGrpc()) {
            link += "network: " + "grpc, ";
            // $return['grpc-opts']['grpc-service-name'] = ($item['servicename'] != '' ? $item['servicename'] : "");
            link += "grpc-opts: {grpc-service-name: " + "" + "}, ";
        }
        link += "udp: " + true + "}\n";
        return link;
    }
}
