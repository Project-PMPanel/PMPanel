package project.daihao18.panel.common.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.URLUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import project.daihao18.panel.entity.Ss;
import project.daihao18.panel.entity.Trojan;
import project.daihao18.panel.entity.User;
import project.daihao18.panel.entity.V2ray;
import project.daihao18.panel.service.*;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ShadowrocketUtil {

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
     * 小火箭订阅
     *
     * @param user
     * @return
     */
    public static String getSub(User user) {
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
            ssSubs += getSsLink(ss, user.getPasswd());
            subs += ssSubs + v2raySubs + trojanSubs;
            return Base64.getEncoder().encodeToString(subs.getBytes());
        }

        // ss
        List<Ss> ssNodes = ssService.list(new QueryWrapper<Ss>().le("`class`", user.getClazz()).eq("flag", 1));
        // 处理ss
        if (ObjectUtil.isNotEmpty(ssNodes)) {
            // 遍历ss节点
            for (Ss ss : ssNodes.stream().sorted(Comparator.comparing(Ss::getSort).thenComparing(Ss::getId)).collect(Collectors.toList())) {
                ssSubs += getSsLink(ss, user.getPasswd());
            }
        }
        // v2ray
        List<V2ray> v2rayNodes = v2rayService.list(new QueryWrapper<V2ray>().le("`class`", user.getClazz()).eq("flag", 1));
        // 处理v2ray
        if (ObjectUtil.isNotEmpty(v2rayNodes)) {
            // 遍历v2ray节点
            for (V2ray v2ray : v2rayNodes.stream().sorted(Comparator.comparing(V2ray::getSort).thenComparing(V2ray::getId)).collect(Collectors.toList())) {
                v2raySubs += getV2rayLink(v2ray, user.getPasswd());
            }
        }
        // trojan
        List<Trojan> trojanNodes = trojanService.list(new QueryWrapper<Trojan>().le("`class`", user.getClazz()).eq("flag", 1));
        // 处理trojan
        if (ObjectUtil.isNotEmpty(trojanNodes)) {
            // 遍历v2ray节点
            for (Trojan trojan : trojanNodes.stream().sorted(Comparator.comparing(Trojan::getSort).thenComparing(Trojan::getId)).collect(Collectors.toList())) {
                trojanSubs += getTrojanLink(trojan, user.getPasswd());
            }
        }
        // 合并订阅
        subs += ssSubs + v2raySubs + trojanSubs;
        return Base64.getEncoder().encodeToString(subs.getBytes());
    }

    private static String getSsLink(Ss ss, String passwd) {
        String prefix = Base64.getUrlEncoder().encodeToString((ss.getMethod() + ":" + passwd + "@" + ss.getSubServer() + ":" + ss.getSubPort()).getBytes(StandardCharsets.UTF_8));
        while (prefix.endsWith("=")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        String link = prefix + "?remarks=";
        String suffix = ss.getName();
        link = link + URLUtil.encode(suffix, StandardCharsets.UTF_8);
        return "ss://" + link + "\n";
    }

    private static String getV2rayLink(V2ray v2ray, String uuid) {
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
        link = link + URLUtil.encode(suffix, StandardCharsets.UTF_8);
        return "vmess://" + link + "\n";
    }

    private static String getTrojanLink(Trojan trojan, String passwd) {
        String prefix = passwd + "@" + trojan.getSubServer() + ":" + trojan.getSubPort();
        String link = prefix + "?remarks=";
        String suffix = trojan.getName();
        link = link + URLUtil.encode(suffix, StandardCharsets.UTF_8);
        return "trojan://" + link + "\n";
    }
}
