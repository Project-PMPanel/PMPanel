package project.daihao18.panel.common.utils;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import project.daihao18.panel.common.exceptions.CustomException;
import project.daihao18.panel.common.response.Result;
import project.daihao18.panel.common.response.ResultCodeEnum;
import project.daihao18.panel.service.ConfigService;
import project.daihao18.panel.service.RedisService;
import project.daihao18.panel.service.UserService;

import javax.annotation.PostConstruct;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Map;
import java.util.Properties;

/**
 * @ClassName: EmailUtil
 * @Description:
 * @Author: code18
 * @Date: 2020-10-27 20:13
 */
@Slf4j
@Component
public class EmailUtil {

    @PostConstruct
    private void init() {
        configService = this.autoConfigService;
        redisService = this.autoRedisService;
        userService = this.autoUserService;
    }

    private static JavaMailSenderImpl jms = new JavaMailSenderImpl();

    @Autowired
    private ConfigService autoConfigService;
    private static ConfigService configService;

    @Autowired
    private RedisService autoRedisService;
    private static RedisService redisService;

    @Autowired
    private UserService autoUserService;
    private static UserService userService;

    /**
     * 根据emailType和application.properties配置的邮件方式发送邮件
     *
     * @param emailType
     * @param subject
     * @param text
     * @param isHtml
     * @param sendTo
     * @return
     * @throws MessagingException
     */
    public static Result send(Integer emailType, String subject, String text, boolean isHtml, String sendTo) throws MessagingException {
        // emailType: 0注册邮件, 1找回密码邮件, 2修改email的验证邮件
        if (emailType == 0 || emailType == 1 || emailType == 2) {
            Integer id = userService.getIdByEmail(sendTo);
            if (ObjectUtil.isNotEmpty(id) && emailType == 0) {
                throw new CustomException(ResultCodeEnum.EXIST_EMAIL_ERROR);
            }
            if (ObjectUtil.isEmpty(id) && emailType == 1) {
                throw new CustomException(ResultCodeEnum.USER_NOT_FIND_ERROR);
            }
            // 判断该用户请求验证码次数
            Object count = redisService.get("panel::CheckCodeLimit::" + sendTo);
            if (ObjectUtil.isNotEmpty(count)) {
                Integer limit = Integer.parseInt(configService.getValueByName("mailLimit"));
                if (Integer.parseInt(count.toString()) >= limit) {
                    throw new CustomException(ResultCodeEnum.MAIL_SEND_LIMIT_ERROR);
                }
            }
            // 生成一个有效期5分钟的6位验证码
            String checkCode = RandomUtil.randomStringUpper(6);
            if (emailType == 0) {
                redisService.set("panel::RegCheckCode::" + sendTo, checkCode, 300);
            } else if (emailType == 1) {
                redisService.set("panel::ForgotPassCheckCode::" + sendTo, checkCode, 300);
            } else {
                redisService.set("panel::ResetEmailCheckCode::" + sendTo, checkCode, 300);
            }
            redisService.incr("panel::CheckCodeLimit::" + sendTo, 1);
            if (ObjectUtil.isEmpty(count)) {
                // 1分钟内第一次请求验证码,开始计时
                redisService.expire("panel::CheckCodeLimit::" + sendTo, 60);
            }
            text = "您好,您的验证码为:" + "<font color='red'>" + checkCode + "</font><br/>";
            switch (configService.getValueByName("mailType")) {
                case "smtp":
                    return sendEmailBySmtp(subject, text, isHtml, sendTo);
            }
        }
        return null;
    }

    /**
     * smtp发送邮件
     *
     * @param subject
     * @param text
     * @param isHtml
     * @param sendTo
     * @return
     * @throws MessagingException
     */
    public static Result sendEmailBySmtp(String subject, String text, boolean isHtml, String sendTo) throws MessagingException {
        // 查询config的mail配置
        Map<String, Object> mailConfig = JSONUtil.toBean(configService.getValueByName("mailConfig"), Map.class);

        jms.setHost(mailConfig.get("host").toString());
        jms.setPort(Double.valueOf(mailConfig.get("port").toString()).intValue());
        jms.setUsername(mailConfig.get("username").toString());
        jms.setPassword(mailConfig.get("password").toString());
        jms.setDefaultEncoding("Utf-8");
        Properties p = new Properties();
        p.setProperty("mail.smtp.auth", "true");
        p.setProperty("mail.smtp.ssl.enable", "true");
        jms.setJavaMailProperties(p);

        MimeMessage mimeMessage = jms.createMimeMessage();

        MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);

        // 邮件设置
        messageHelper.setSubject(subject);
        messageHelper.setText(text, isHtml);
        messageHelper.setTo(sendTo);
        messageHelper.setFrom(mailConfig.get("username").toString());

        jms.send(mimeMessage);

        return Result.ok().message("");
    }

}