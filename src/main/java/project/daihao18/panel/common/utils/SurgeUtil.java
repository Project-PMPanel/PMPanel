package project.daihao18.panel.common.utils;

import cn.hutool.core.date.DateUtil;
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
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SurgeUtil {

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
     * 获取Surge 3,4订阅
     *
     * @param user
     * @return
     */
    public static String getSub(User user) throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("config/surge4");
        BufferedReader bfreader = new BufferedReader(new InputStreamReader(classPathResource.getInputStream(), "UTF-8"));
        StringBuilder builder = new StringBuilder();
        StringBuilder nodeName = new StringBuilder();
        // 开始处理订阅内容
        builder.append("#!MANAGED-CONFIG " + user.getSubsLink() + " interval=43200 strict=true\n\n");
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
                    for (Ss ssNode : ssNodes.stream().sorted(Comparator.comparing(Ss::getSort).thenComparing(Ss::getId)).collect(Collectors.toList())) {
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
                    for (V2ray v2ray : v2rayNodes.stream().sorted(Comparator.comparing(V2ray::getSort).thenComparing(V2ray::getId)).collect(Collectors.toList())) {
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
                    for (Trojan trojan : trojanNodes.stream().sorted(Comparator.comparing(Trojan::getSort).thenComparing(Trojan::getId)).collect(Collectors.toList())) {
                        builder.append(
                                trojan.getName() + " = trojan, " + trojan.getSubServer() + ", " + trojan.getSubPort() + ", password = " + user.getPasswd()
                        );
                        nodeName.append(trojan.getName() + ", ");
                    }
                }
                // 删除nodeName最后的,和空格
                if (nodeName.length() > 1) {
                    nodeName.deleteCharAt(nodeName.length() - 1);
                    nodeName.deleteCharAt(nodeName.length() - 1);
                    nodeName.append("\n");
                }
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
}
