package project.daihao18.panel.common.utils;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dm.model.v20151123.SingleSendMailRequest;
import com.aliyuncs.dm.model.v20151123.SingleSendMailResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import lombok.extern.slf4j.Slf4j;
import net.sargue.mailgun.Configuration;
import net.sargue.mailgun.Mail;
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
        // 过滤gmail邮箱重复批量注册
        if (sendTo.split("@")[0].contains("+") || sendTo.split("@")[0].contains(".")) {
            return Result.error().message("非法邮箱").messageEnglish("Invalid email address");
        }
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
            StringBuilder content = new StringBuilder();
            content.append("尊敬的用户您好,<br/><br/>");
            content.append("您在 {siteName} 的申请的验证码为:<br/>");
            content.append("<font color='red'>");
            content.append(checkCode);
            content.append("</font><br/>");
            content.append("有效期5分钟");

            // 获取要发信的内容
            String siteName = configService.getValueByName("siteName");
            String siteUrl = configService.getValueByName("siteUrl");
            subject = "验证邮件";
            // 获取通知续费邮件模板
            String body = configService.getValueByName("mailTemplate");
            body = body.replaceAll("\\{content}", content.toString());
            body = body.replaceAll("\\{siteName}", siteName);
            body = body.replaceAll("\\{siteUrl}", siteUrl);
            body = body.replaceAll("\\{title}", subject);
            switch (configService.getValueByName("mailType")) {
                case "smtp":
                    return sendEmailBySmtp(subject, body, isHtml, sendTo, 0) ? Result.ok() : Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
                case "postalAPI":
                    return sendEmailByPostalAPI(subject, body, isHtml, sendTo, 0) ? Result.ok() : Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
                case "aliyunAPI":
                    return sendEmailByAliyunAPI(subject, body, isHtml, sendTo, 0) ? Result.ok() : Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
                case "mailgunAPI":
                    return sendEmailByMailgunAPI(subject, body, isHtml, sendTo, 0) ? Result.ok() : Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
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
                case "aliyunAPI":
                    return sendEmailByAliyunAPI(subject, text, isHtml, sendTo, 1);
                case "mailgunAPI":
                    return sendEmailByMailgunAPI(subject, text, isHtml, sendTo, 1);
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
        messageHelper.setFrom(mailConfig.get("from").toString());

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
        // 自己从redis查
        List<String> emails = new ArrayList<>();
        if (ObjectUtil.isEmpty(sendTo)) {
            // sendTo是null
            emails = (List) redisService.lRange("panel::emails", 0, -1);
            if (ObjectUtil.isEmpty(emails)) {
                return false;
            } else {
                redisService.del("panel::emails");
            }
        } else {
            emails.add(sendTo);
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

    private static IAcsClient client;

    private static IAcsClient getClient(String accessKey, String accessSecret) {
        if (ObjectUtil.isNotEmpty(client)) {
            return client;
        }
        // 如果是除杭州region外的其它region（如新加坡、澳洲Region），需要将下面的”cn-hangzhou”替换为”ap-southeast-1”、或”ap-southeast-2”。
        IClientProfile profile = DefaultProfile.getProfile("ap-southeast-1", accessKey, accessSecret);
        // 如果是除杭州region外的其它region（如新加坡region）， 需要做如下处理
        try {
            DefaultProfile.addEndpoint("dm.ap-southeast-1.aliyuncs.com", "ap-southeast-1", "Dm", "dm.ap-southeast-1.aliyuncs.com");
        } catch (ClientException e) {
            e.printStackTrace();
        }
        client = new DefaultAcsClient(profile);
        return client;
    }

    /**
     * 阿里云API发信
     * @param subject
     * @param text
     * @param isHtml
     * @param sendTo
     * @param type
     * @return
     */
    private static boolean sendEmailByAliyunAPI(String subject, String text, boolean isHtml, String sendTo, int type) {
        // 自己从redis查
        List<String> emails = new ArrayList<>();
        if (ObjectUtil.isEmpty(sendTo)) {
            // sendTo是null
            emails = (List) redisService.lRange("panel::emails", 0, -1);
            if (ObjectUtil.isEmpty(emails)) {
                return false;
            } else {
                redisService.del("panel::emails");
            }
        } else {
            emails.add(sendTo);
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
            SingleSendMailRequest request = new SingleSendMailRequest();
            try {
                request.setVersion("2017-06-22");// 如果是除杭州region外的其它region（如新加坡region）,必须指定为2017-06-22
                request.setAccountName(mailConfig.get("accountName").toString());
                request.setAddressType(1);
                request.setReplyToAddress(true);
                request.setFromAlias(mailConfig.get("alias").toString());
                request.setToAddress(String.join(",", sendTO));
                //可以给多个收件人发送邮件，收件人之间用逗号分开，批量发信建议使用BatchSendMailRequest方式
                //request.setToAddress("邮箱1,邮箱2");
                request.setSubject(subject);
                //如果采用byte[].toString的方式的话请确保最终转换成utf-8的格式再放入htmlbody和textbody，若编码不一致则会被当成垃圾邮件。
                //注意：文本邮件的大小限制为3M，过大的文本会导致连接超时或413错误
                request.setHtmlBody(text);
                //SDK 采用的是http协议的发信方式, 默认是GET方法，有一定的长度限制。
                //若textBody、htmlBody或content的大小不确定，建议采用POST方式提交，避免出现uri is not valid异常
                request.setMethod(MethodType.POST);
                //开启需要备案，0关闭，1开启
                //request.setClickTrace("0");
                //如果调用成功，正常返回httpResponse；如果调用失败则抛出异常，需要在异常中捕获错误异常码；错误异常码请参考对应的API文档;
                SingleSendMailResponse httpResponse = getClient(mailConfig.get("accessKey").toString(), mailConfig.get("accessSecret").toString()).getAcsResponse(request);
                log.info(JSONUtil.toJsonStr(httpResponse));
            } catch (ServerException e) {
                //捕获错误异常码
                log.error("Mail to: {} occurs ServerException, ErrCode: {}, ErrMsg: {}", sendTO, e.getErrCode(), e.getErrMsg());
                // TODO 每日限额完成,报警
            } catch (ClientException e) {
                //捕获错误异常码
                log.error("Mail to: {} occurs ClientException, ErrCode: {}, ErrMsg: {}", sendTO, e.getErrCode(), e.getErrMsg());
            }
        }
        return true;
    }

    private static Configuration configuration = null;

    private static Configuration getConfiguration(Map<String, Object> mailConfig) {
        if (ObjectUtil.isEmpty(configuration)) {
            configuration = new Configuration()
                    .domain(mailConfig.get("domain").toString())
                    .apiKey(mailConfig.get("key").toString())
                    .from(configService.getValueByName("siteName"), mailConfig.get("sender").toString());
        }
        return configuration;
    }

    /**
     * mailgun发信api
     * @param subject
     * @param text
     * @param isHtml
     * @param sendTo
     * @param type
     * @return
     */
    private static boolean sendEmailByMailgunAPI(String subject, String text, boolean isHtml, String sendTo, int type) {
        // 自己从redis查
        List<String> emails = new ArrayList<>();
        if (ObjectUtil.isEmpty(sendTo)) {
            // sendTo是null
            emails = (List) redisService.lRange("panel::emails", 0, -1);
            if (ObjectUtil.isEmpty(emails)) {
                return false;
            } else {
                redisService.del("panel::emails");
            }
        } else {
            emails.add(sendTo);
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
        for (int i = 0; i < emails.size(); i++) {
            // 请求发信
            Mail.using(getConfiguration(mailConfig))
                    .to(emails.remove(i))
                    .subject(subject)
                    .html(text)
                    .build()
                    .send();
        }
        return true;
    }

}