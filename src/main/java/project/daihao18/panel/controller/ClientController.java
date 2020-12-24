package project.daihao18.panel.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.UnicodeUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.symmetric.RC4;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import project.daihao18.panel.common.utils.IpUtil;
import project.daihao18.panel.common.utils.JwtTokenUtil;
import project.daihao18.panel.entity.*;
import project.daihao18.panel.service.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @ClassName: ClientController
 * @Description:
 * @Author: code18
 * @Date: 2020-12-11 22:13
 */
@RestController
@RequestMapping("/v1")
public class ClientController {

    @Value("${client.enable}")
    private Boolean enable;

    @Autowired
    private Client client;

    @Autowired
    private UserService userService;

    @Autowired
    private OperateIpService operateIpService;

    @Autowired
    private SsNodeService ssNodeService;

    @Autowired
    private SsNodeOnlineLogService ssNodeOnlineLogService;

    @Autowired
    private AnnouncementService announcementService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public String login(HttpServletRequest request, HttpServletResponse response, @RequestBody Map<String, String> map) {
        if (!enable) {
            return badResp("定制客户端维护中...请临时使用开源客户端clash");
        }
        // 查登录次数
        String requestIP = IpUtil.getIpAddr(request);
        Date now = new Date();
        QueryWrapper<OperateIp> operateIpQueryWrapper = new QueryWrapper<>();
        operateIpQueryWrapper.eq("ip", requestIP).eq("type", 2).gt("time", (now.getTime() / 1000) - 3600);
        int count = operateIpService.count(operateIpQueryWrapper);
        if (count > 25) {
            return badResp("您最近登录过于频繁，请1小时后重试");
        }

        String username = map.get("username");
        String password = map.get("password");
        // 根据username,即email查用户
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.select("id").eq("email", username);
        User user = userService.getOne(userQueryWrapper);
        if (ObjectUtil.isEmpty(user)) {
            return badResp("用户名不存在");
        }
        user = userService.getUserById(user.getId(), false);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return this.badResp("用户名或密码错误");
        }
        // cookie
        // 设置cookie
        int expiry = 3600 * 24 * 600;
        Long expireIn = now.getTime() / 1000 + expiry;
        // jwt token
        String token = JwtTokenUtil.createToken(user.getUsername(), user.getRole().get("id").toString(), true);
        Cookie jwt = new Cookie("token", token);
        jwt.setMaxAge(expireIn.intValue());
        jwt.setPath("/");
        response.addCookie(jwt);
        Cookie uid = new Cookie("uid", user.getId().toString());
        Cookie email = new Cookie("email", user.getEmail());
        Cookie key = new Cookie("key", DigestUtil.sha256Hex(user.getPassword() + (expireIn)).substring(5, 50));
        // 存jwttoken让安卓跳转登录
        //Cookie ip = new Cookie("ip", DigestUtil.md5Hex(requestIP + user.getId() + expireIn));
        Cookie ip = new Cookie("ip", token);
        Cookie expire_in = new Cookie("expire_in", String.valueOf(expireIn));
        uid.setMaxAge(expireIn.intValue());
        uid.setPath("/");
        email.setMaxAge(expireIn.intValue());
        email.setPath("/");
        key.setMaxAge(expireIn.intValue());
        key.setPath("/");
        ip.setMaxAge(expireIn.intValue());
        ip.setPath("/");
        expire_in.setMaxAge(expireIn.intValue());
        expire_in.setPath("/");
        response.addCookie(uid);
        response.addCookie(email);
        response.addCookie(key);
        response.addCookie(ip);
        response.addCookie(expire_in);
        // 存一个loginIp
        OperateIp operateIp = new OperateIp();
        operateIp.setUserId(user.getId());
        operateIp.setTime(now);
        operateIp.setIp(requestIP);
        operateIp.setType(2);
        if (operateIpService.save(operateIp)) {
            Map<String, Object> userInfo = getUserInfo(user);
            return normalResp("登录成功", userInfo);
        } else {
            return badResp("登录失败");
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (ObjectUtil.isNotEmpty(cookies)) {
            for (Cookie cookie : cookies) {
                if ("uid".equals(cookie.getName())) {
                    cookie.setValue(null);
                    cookie.setMaxAge(0);
                    cookie.setPath("/");
                    response.addCookie(cookie);
                }
                if ("email".equals(cookie.getName())) {
                    cookie.setValue(null);
                    cookie.setMaxAge(0);
                    cookie.setPath("/");
                    response.addCookie(cookie);
                }
                if ("key".equals(cookie.getName())) {
                    cookie.setValue(null);
                    cookie.setMaxAge(0);
                    cookie.setPath("/");
                    response.addCookie(cookie);
                }
                if ("ip".equals(cookie.getName())) {
                    cookie.setValue(null);
                    cookie.setMaxAge(0);
                    cookie.setPath("/");
                    response.addCookie(cookie);
                }
                if ("expire_in".equals(cookie.getName())) {
                    cookie.setValue(null);
                    cookie.setMaxAge(0);
                    cookie.setPath("/");
                    response.addCookie(cookie);
                }
            }
        }
        return normalResp("登出成功", null);
    }

    @GetMapping("/init")
    public String init() {
        return client.getBaseUrl();
    }

    @GetMapping("/broadcast")
    public String broadcast() {
        Map<String, Object> data = new HashMap<>();
        data.put("title", ObjectUtil.isNotEmpty(client.getTitle()) ? UnicodeUtil.toUnicode(new String(client.getTitle().getBytes(StandardCharsets.UTF_8))) : false);
        data.put("content", ObjectUtil.isNotEmpty(client.getContent()) ? UnicodeUtil.toUnicode(new String(client.getContent().getBytes(StandardCharsets.UTF_8))) : false);
        data.put("broad_url", ObjectUtil.isNotEmpty(client.getBroadUrl()) ? client.getBroadUrl() : "");
        data.put("broad_show", ObjectUtil.isNotEmpty(client.getBroadShow()) ? 1 : 0);
        data.put("bootstrap_show", ObjectUtil.isNotEmpty(client.getBootstrapShow()) ? client.getBootstrapShow() : false);
        data.put("bootstrap_img", ObjectUtil.isNotEmpty(client.getBootstrapImg()) ? client.getBootstrapImg() : false);
        data.put("bootstrap_url", ObjectUtil.isNotEmpty(client.getBootstrapUrl()) ? client.getBootstrapUrl() : false);
        data.put("version_code", ObjectUtil.isNotEmpty(client.getVersionCode()) ? client.getVersionCode() : 0);
        data.put("description", ObjectUtil.isNotEmpty(client.getDescription()) ? UnicodeUtil.toUnicode(new String(client.getDescription().getBytes(StandardCharsets.UTF_8))) : "");
        data.put("download", ObjectUtil.isNotEmpty(client.getDownload()) ? client.getDownload() : "");
        return normalResp("获取成功", data);
    }

    @GetMapping("/update")
    public String update() {
        Map<String, Object> data = new HashMap<>();
        data.put("version_code", ObjectUtil.isNotEmpty(client.getVersionCode()) ? client.getVersionCode() : 0);
        data.put("description", ObjectUtil.isNotEmpty(client.getDescription()) ? client.getDescription() : "");
        data.put("download", ObjectUtil.isNotEmpty(client.getDownload()) ? client.getDownload() : "");
        return normalResp("获取成功", data);
    }

    @GetMapping("/config")
    public String config(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> map = new HashMap<>();
        map.put("online", getOnline(request, response));
        map.put("bootstrap", client.getBootstrap());
        map.put("nav", client.getNav());
        map.put("levelDesc", client.getLevelDesc());
        map.put("holdConnect", client.getHoldConnect());
        map.put("userinfo_frq", client.getUserinfoFrq());
        map.put("onlineinfo_frq", client.getOnlineinfoFrq());
        return UnicodeUtil.toUnicode(new String(JSONUtil.toJsonStr(map).getBytes(StandardCharsets.UTF_8)));
    }

    @GetMapping("/online")
    public String online(HttpServletRequest request, HttpServletResponse response) {
        return normalResp("获取成功", JSONUtil.toJsonStr(getOnline(request, response)));
    }

    private Map<String, Object> getOnline(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> onlineMap = new HashMap<>();
        onlineMap.put("enable", false);
        onlineMap.put("callback", new HashMap<>());
        // 获取online信息
        User user = AuthUser(request, response);
        if (ObjectUtil.isNotEmpty(user)) {
            onlineMap.put("enable", true);
            // 去查询该用户的节点
            List<SsNode> ssNodes = ssNodeService.listEnableNodesByGroupAndClass(user.getNodeGroup(), user.getClazz());
            ssNodes.forEach(node -> {
                SsNodeOnlineLog one = ssNodeOnlineLogService.getOne(new QueryWrapper<SsNodeOnlineLog>().select("online_user").eq("node_id", node.getId()).orderByDesc("log_time").last("limit 1"));
                if (ObjectUtil.isNotEmpty(one)) {
                    Map<String, Object> log = new HashMap<>();
                    if (one.getOnlineUser() < 20) {
                        log.put("text", "畅通");
                        log.put("color", "#52c41a");
                    } else if (one.getOnlineUser() < 40) {
                        log.put("text", "拥挤");
                        log.put("color", "#faad14");
                    } else {
                        log.put("text", "爆满");
                        log.put("color", "#d9363e");
                    }
                    onlineMap.put(node.getName(), log);
                }
            });
        }
        return onlineMap;
    }

    @GetMapping("/userinfo")
    public String userinfo(HttpServletRequest request, HttpServletResponse response) {
        User user = AuthUser(request, response);
        if (ObjectUtil.isEmpty(user)) {
            return unAuth();
        }
        return normalResp("获取成功", getUserInfo(user));
    }

    protected Map<String, Object> getUserInfo(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("username", user.getEmail());
        map.put("true_name", user.getEmail());
        Map<String, Object> traffic = new HashMap<>();
        traffic.put("total", user.getTransferEnable());
        traffic.put("used", user.getU() + user.getD());
        map.put("traffic", traffic);
        map.put("balance", user.getMoney().toString());
        map.put("class", user.getClazz());
        map.put("class_expire", DateUtil.format(user.getExpireIn(), "yyyy-MM-dd HH:mm:ss"));
        map.put("node_speedlimit", user.getNodeSpeedlimit() == 0 ? "不限速" : user.getNodeSpeedlimit());
        map.put("node_connector", user.getNodeConnector() == 0 ? "无限制" : user.getNodeConnector());
        String subRule = UnicodeUtil.toUnicode(new String(client.getSubecribeRule().getBytes(StandardCharsets.UTF_8)));
        map.put("defaultProxy", ObjectUtil.isEmpty(client.getSubecribeRule()) ? "Proxy" : subRule);
        // TODO getPCSub,getAndroidSub
        map.put("pc_sub", user.getSubsLink() + "clash");
        List<String> androidSub = new ArrayList<>();
        androidSub.add(user.getSubsLink() + "clash_ss");
        androidSub.add(user.getSubsLink() + "clash_v2");
        map.put("android_sub", androidSub);// TODO 获取SS & SSR 通用订阅 放列表里
        // 杂项
        map.put("last_checkin", DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        map.put("reg_date", DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        return map;
    }

    @GetMapping("/anno")
    public String anno(HttpServletRequest request, HttpServletResponse response) {
        User user = AuthUser(request, response);
        if (ObjectUtil.isEmpty(user)) {
            return unAuth();
        }
        List<Map<String, Object>> list = new ArrayList<>();
        List<Announcement> announcements = announcementService.list(new QueryWrapper<Announcement>().orderByDesc("time"));
        announcements.forEach(item -> {
            Map<String, Object> map = new HashMap<>();
            map.put("markdown", UnicodeUtil.toUnicode(new String(item.getMarkdownContent().getBytes(StandardCharsets.UTF_8))));
            map.put("date", DateUtil.format(item.getTime(), "yyyy-MM-dd HH:mm:ss"));
            list.add(map);
        });
        return normalResp("获取成功", list);
    }

    @GetMapping("/pc-alert")
    public String pcAlert(HttpServletRequest request, HttpServletResponse response) {
        User user = AuthUser(request, response);
        if (ObjectUtil.isEmpty(user)) {
            return unAuth();
        }
        Map<String, Object> map = new HashMap<>();
        if (client.getPcAnnoShow()) {
            map.put("show", true);
            map.put("title", UnicodeUtil.toUnicode(new String(client.getPcAnnoTitle().getBytes(StandardCharsets.UTF_8))));
            map.put("content", UnicodeUtil.toUnicode(new String(client.getPcAnnoContent().getBytes(StandardCharsets.UTF_8))));
        } else {
            map.put("show", false);
        }
        return normalResp("ok", map);
    }

    @GetMapping("/pc-update")
    public String pcUpdateCheck(HttpServletRequest request, HttpServletResponse response) {
        User user = AuthUser(request, response);
        if (ObjectUtil.isEmpty(user)) {
            return unAuth();
        }
        String latest = client.getPcUpdateVersionCode();
        String curVersion = request.getParameter("curVersion");
        Map<String, Object> map = new HashMap<>();
        Boolean needUpdate = Integer.parseInt(latest) > Integer.parseInt(curVersion);
        map.put("update", needUpdate);
        if (needUpdate) {
            map.put("desc", UnicodeUtil.toUnicode(new String(client.getPcUpdateDescription().getBytes(StandardCharsets.UTF_8))));
            map.put("pc", UnicodeUtil.toUnicode(new String(client.getPcUpdateDownload().getBytes(StandardCharsets.UTF_8))));
            map.put("mac", UnicodeUtil.toUnicode(new String(client.getPcUpdateDownloadMac().getBytes(StandardCharsets.UTF_8))));
            map.put("download", UnicodeUtil.toUnicode(new String(client.getDownload().getBytes(StandardCharsets.UTF_8))));
        }
        return normalResp("ok", map);
    }

    protected String normalResp(String info, Object data) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("info", UnicodeUtil.toUnicode(info));
        map.put("data", data);
        String pt = JSONUtil.toJsonStr(map).replace("\\\\", "\\");
        return RC4(pt, null);
    }

    protected String errorResp(Integer code, String info) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", code);
        map.put("info", UnicodeUtil.toUnicode(info));
        String pt = JSONUtil.toJsonStr(map).replace("\\\\", "\\");
        return RC4(pt, null);
    }

    protected static String RC4(String pt, String key) {
        if (ObjectUtil.isEmpty(key)) {
            key = "RocketMaker";
        }
        RC4 rc4 = new RC4(key);
        return rc4.encryptBase64(pt, StandardCharsets.UTF_8);
    }

    protected String unAuth() {
        return errorResp(401, "请先登录");
    }

    protected String forbid() {
        return errorResp(403, "您的账户已被禁用");
    }

    protected String badResp(String msg) {
        return errorResp(400, msg);
    }


    protected User AuthUser(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        String uid = "";
        String email = "";
        String key = "";
        String ipHash = "";
        String expire_in = "";
        if (ObjectUtil.isNotEmpty(cookies)) {
            for (Cookie cookie : cookies) {
                if ("uid".equals(cookie.getName())) {
                    uid = cookie.getValue();
                }
                if ("email".equals(cookie.getName())) {
                    email = cookie.getValue();
                }
                if ("key".equals(cookie.getName())) {
                    key = cookie.getValue();
                }
                if ("ip".equals(cookie.getName())) {
                    ipHash = cookie.getValue();
                }
                if ("expire_in".equals(cookie.getName())) {
                    expire_in = cookie.getValue();
                }
            }
        }
        User user = null;
        if (ObjectUtil.isNotEmpty(uid)) {
            // 查该用户
            user = userService.getUserById(Integer.parseInt(uid), false);
            if (ObjectUtil.isNotEmpty(user)) {
                // 判断email是否相等
                if (ObjectUtil.notEqual(email, user.getEmail())) {
                    return null;
                }
                // cookie hash是否相等
                String cookieIpHash = DigestUtil.sha256Hex(user.getPassword() + expire_in).substring(5, 50);
                if (ObjectUtil.notEqual(key, cookieIpHash)) {
                    return null;
                }
            }
        }
        return user;
    }

    /**
     * 已弃用

     @GetMapping("/link") public String subLink(HttpServletRequest request, HttpServletResponse response) {
     User user = AuthUser(request, response);
     if (ObjectUtil.isEmpty(user)) {
     return unAuth();
     }
     response.addHeader("Cache-Control", "no-store, no-cache, must-revalidate");
     // 获取clash的所有订阅节点
     String link = user.getSubsLink().split("\\/")[5];
     List<Map<String, Object>> nodeList = new ArrayList<>();
     List<Map<String, Object>> ssList = subService.getSSList(link);
     List<Map<String, Object>> v2rayList = subService.getV2rayList(link);
     nodeList.addAll(ssList);
     nodeList.addAll(v2rayList);
     // 遍历处理nodes
     List<String> nodeLink = new ArrayList<>();
     nodeList.forEach(node -> {
     Map<String, Object> map = new HashMap<>();
     if ("vmess".equals(node.get("type"))) {
     map.put("v", 2);
     map.put("ps", node.get("remark"));
     map.put("add", node.get("add"));
     map.put("port", node.get("port"));
     map.put("id", node.get("id"));
     map.put("aid", node.get("aid"));
     map.put("net", node.get("net"));
     map.put("type", node.get("headerType"));
     map.put("host", node.get("host"));
     map.put("path", node.get("path"));
     map.put("tls", node.get("tls"));
     String s = "vmess://" + Base64.getUrlEncoder().encodeToString(JSONUtil.toJsonStr(map).getBytes());
     nodeLink.add(s);
     }
     if (node.get("type").toString().startsWith("simple_obfs")) {
     // ss单端口
     String s =
     Base64.getUrlEncoder().encodeToString((node.get("method") + ":" + node.get("passwd")).getBytes(StandardCharsets.UTF_8)) + "@" +
     node.get("address") + ":" +
     node.get("port") +
     "/?plugin=obfs-local";
     String suffix = null;
     try {
     suffix = URLEncoder.encode(";obfs=" + node.get("obfs") + ";" +
     "obfs-host=" + node.get("obfs_param"), "UTF-8") + "&group=" + Base64.getUrlEncoder().encodeToString(node.get("group").toString().getBytes()) + "#" + URLEncoder.encode(node.get("remark").toString(), "UTF-8");
     } catch (UnsupportedEncodingException e) {
     e.printStackTrace();
     }
     s = "ss://" + s + suffix + "\n";
     nodeLink.add(s);
     }
     });
     Map<String, Object> info = new HashMap<>();
     info.put("link", nodeLink);
     info.put("md5", DigestUtil.md5Hex(JSONUtil.toJsonStr(nodeLink), StandardCharsets.UTF_8));
     info.put("rep", UnicodeUtil.toUnicode("/(香港|美国|日本|中国|俄罗斯|韩国|英国|新加坡|马来西亚|台湾|加拿大|菲律宾|德国)/"));
     info.put("default_rule", 0);
     return normalResp("获取成功", info);
     }




      * */

}