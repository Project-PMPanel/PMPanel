package project.daihao18.panel.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import project.daihao18.panel.common.response.Result;
import project.daihao18.panel.common.utils.EmailUtil;
import project.daihao18.panel.entity.User;
import project.daihao18.panel.service.UserService;

import javax.mail.MessagingException;
import java.util.Map;

/**
 * @ClassName: AuthController
 * @Description:
 * @Author: code18
 * @Date: 2020-10-10 15:09
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

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
}