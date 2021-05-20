package project.daihao18.panel.common.utils;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import project.daihao18.panel.common.exceptions.CustomException;
import project.daihao18.panel.common.response.Result;
import project.daihao18.panel.common.response.ResultCodeEnum;
import project.daihao18.panel.service.ConfigService;
import project.daihao18.panel.service.RedisService;
import project.daihao18.panel.service.UserService;

import javax.annotation.PostConstruct;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.*;

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
     * 根据emailType和application.properties配置的邮件方式发送验证码邮件
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
            String checkCode = RandomUtil.randomNumbers(6);
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
                    return sendEmailBySmtp(subject, text, isHtml, sendTo, 0) ? Result.ok() : Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
            }
        }
        return null;
    }

    /**
     * 通知发信
     *
     * @param subject
     * @param text
     * @param isHtml
     * @param sendTo
     * @return
     */
    public static boolean sendEmail(String subject, String text, boolean isHtml, String sendTo) {
        try {
            switch (configService.getValueByName("notifyMailType")) {
                case "smtp":
                    return sendEmailBySmtp(subject, text, isHtml, sendTo, 1);
                case "postalAPI":
                    return sendEmailByPostalAPI(subject, text, isHtml, sendTo, 1);
            }
            return true;
        } catch (Exception e) {
            // 发送失败,将该email放到redis邮件队列最后面
            redisService.lPush("panel::failedEmails", sendTo, 86400);
            log.info("邮件发送失败: {}, 失败原因: {}", sendTo, e.getMessage());
            return false;
        }
    }

    ///////////////////////////

    /**
     * smtp发送邮件
     *
     * @param subject
     * @param text
     * @param isHtml
     * @param sendTo
     * @param type
     * @return
     * @throws MessagingException
     */
    public static boolean sendEmailBySmtp(String subject, String text, boolean isHtml, String sendTo, Integer type) throws MessagingException {
        if (ObjectUtil.isEmpty(sendTo)) {
            return false;
        }
        // 查询config的mail配置
        Map<String, Object> mailConfig = new HashMap<>();
        switch (type) {
            case 0:
                // 0是验证码
                mailConfig = JSONUtil.toBean(configService.getValueByName("mailConfig"), Map.class);
                break;
            case 1:
                // 1是公告
                mailConfig = JSONUtil.toBean(configService.getValueByName("notifyMailConfig"), Map.class);
        }

        jms.setHost(mailConfig.get("host").toString());
        jms.setPort(Double.valueOf(mailConfig.get("port").toString()).intValue());
        jms.setUsername(mailConfig.get("username").toString());
        jms.setPassword(mailConfig.get("password").toString());
        jms.setDefaultEncoding("Utf-8");
        Properties p = new Properties();
        p.setProperty("mail.smtp.auth", "true");
        if ((Boolean) mailConfig.get("ssl")) {
            p.setProperty("mail.smtp.ssl.enable", "true");
        }
        p.setProperty("mail.smtp.timeout", "5000");
        p.setProperty("mail.smtp.connectiontimeout", "5000");
        p.setProperty("mail.smtp.writetimeout", "5000");
        jms.setJavaMailProperties(p);

        MimeMessage mimeMessage = jms.createMimeMessage();

        MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);

        // 邮件设置
        messageHelper.setSubject(subject);
        messageHelper.setText(text, isHtml);
        messageHelper.setTo(sendTo);
        messageHelper.setFrom(mailConfig.get("username").toString());

        jms.send(mimeMessage);

        return true;
    }

    /**
     * postalAPI发信
     *
     * @param subject
     * @param text
     * @param isHtml
     * @param sendTo
     * @param type
     * @return
     */
    public static boolean sendEmailByPostalAPI(String subject, String text, boolean isHtml, String sendTo, int type) {
        // sendTo是null
        // 自己从redis查
        List<String> emails = (List) redisService.lRange("panel::emails", 0, -1);
        if (ObjectUtil.isEmpty(emails)) {
            return false;
        } else {
            redisService.del("panel::emails");
        }
        // 查询config的mail配置
        Map<String, Object> mailConfig = new HashMap<>();
        switch (type) {
            case 0:
                // 0是验证码
                mailConfig = JSONUtil.toBean(configService.getValueByName("mailConfig"), Map.class);
                break;
            case 1:
                // 1是公告
                mailConfig = JSONUtil.toBean(configService.getValueByName("notifyMailConfig"), Map.class);
        }
        int count = 0;
        if (emails.size() % 50 == 0) {
            count = emails.size() / 50;
        } else {
            count = emails.size() / 50 + 1;
        }
        for (int i = 0; i < count; i++) {
            // 取出emails中50个元素
            List<String> sendTO = new ArrayList<>();
            for (int j = 0; j < 50; j++) {
                if (ObjectUtil.isNotEmpty(emails)) {
                    sendTO.add(emails.remove(0));
                }
            }
            // 请求发信
            RestTemplate restTemplate = new RestTemplate();
            //设置类型
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("X-Server-API-Key", mailConfig.get("apiKey").toString());
            Map<String, Object> map = new HashMap<>();
            map.put("to", sendTO);
            map.put("from", mailConfig.get("username").toString());
            map.put("sender", mailConfig.get("username").toString());
            map.put("subject", subject);
            map.put("html_body", text);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(map, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(mailConfig.get("host").toString(), request, Map.class);
            // log.info("{}", response);
        }
        return true;
    }

}