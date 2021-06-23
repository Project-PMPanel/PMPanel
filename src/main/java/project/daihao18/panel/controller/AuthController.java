package project.daihao18.panel.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.exception.AuthException;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthGoogleRequest;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.utils.AuthStateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import project.daihao18.panel.common.response.Result;
import project.daihao18.panel.common.utils.EmailUtil;
import project.daihao18.panel.common.utils.IpUtil;
import project.daihao18.panel.common.utils.JwtTokenUtil;
import project.daihao18.panel.entity.OperateIp;
import project.daihao18.panel.entity.User;
import project.daihao18.panel.service.*;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * @ClassName: AuthController
 * @Description:
 * @Author: code18
 * @Date: 2020-10-10 15:09
 */
@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private OperateIpService operateIpService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private OauthService oauthService;

    @GetMapping("/getSiteConfig")
    public Result getSiteConfig() {
        return userService.getSiteConfig();
    }

    @PostMapping("/register")
    public Result register(@RequestBody User user) {
        return userService.register(user);
    }

    @PostMapping("/findPass")
    public Result findPass(@RequestBody User user) {
        return userService.findPass(user);
    }

    @GetMapping("/getEmailCheckCode")
    public Result getEmailCheckCode(@RequestParam Map<String, Object> params) throws MessagingException {
        return EmailUtil.send(Integer.parseInt(params.get("type").toString()), "验证邮件", null, true, params.get("email").toString());
    }

    @RequestMapping("/login/{source}")
    public Result renderAuth(@PathVariable("source") String source) throws IOException {
        AuthRequest authRequest = getAuthRequest(source);
        return Result.ok().data("url", authRequest.authorize(AuthStateUtils.createState()));
    }

    @RequestMapping("/callback/{source}")
    public void login(@PathVariable("source") String source, AuthCallback callback, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AuthRequest authRequest = getAuthRequest(source);
        AuthResponse<AuthUser> result = authRequest.login(callback);
        log.info(JSONUtil.toJsonStr(result));
        if (result.ok()) {
            // 授权验证成功
            // 根据该用户绑定的source的唯一识别去查找该用户
            User user = oauthService.getUser(result.getData().getEmail());
            // 用户存在,颁发token
            if (ObjectUtil.isNotEmpty(user)) {
                String role = "";
                Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
                // springcache 导致authorities为null, 但是从缓存中可以取到role,所以在这里判断一下从哪获取role
                if (ObjectUtil.isNotEmpty(authorities)) {
                    for (GrantedAuthority authority : authorities) {
                        role = authority.getAuthority();
                    }
                } else {
                    role = user.getRole().get("id").toString();
                }
                String token = JwtTokenUtil.createToken(user.getUsername(), role, ObjectUtil.equal(false, true));
                // 记录登录ip
                OperateIp operateIp = new OperateIp();
                operateIp.setIp(IpUtil.getIpAddr(request));
                operateIp.setTime(new Date());
                operateIp.setType(1);
                operateIp.setUserId(user.getId());
                operateIpService.save(operateIp);
                // 返回创建成功的token
                // 但是这里创建的token只是单纯的token
                // 按照jwt的规定，最后请求的格式应该是 `Bearer token`
                response.sendRedirect(configService.getValueByName("siteUrl") + "/auth/login?token=" + token);
            } else {
                response.sendRedirect(configService.getValueByName("siteUrl") + "/account/settings/binding?action=true&type=" + source + "&email=" + result.getData().getEmail() + "&uuid=" + result.getData().getUuid());
            }

        }
    }

    private AuthRequest getAuthRequest(String source) {
        AuthRequest authRequest = null;
        switch (source.toLowerCase()) {
            case "google":
                JSONObject google = (JSONObject) JSONUtil.toBean(configService.getValueByName("oauthConfig"), Map.class).get("google");
                authRequest = new AuthGoogleRequest(AuthConfig.builder()
                        .clientId(JSONUtil.toBean(google, Map.class).get("id").toString())
                        .clientSecret(JSONUtil.toBean(google, Map.class).get("secret").toString())
                        .redirectUri(JSONUtil.toBean(google, Map.class).get("redirectUri").toString())
                        .build());
        }
        if (null == authRequest) {
            throw new AuthException("未获取到有效的Auth配置");
        }
        return authRequest;
    }
}