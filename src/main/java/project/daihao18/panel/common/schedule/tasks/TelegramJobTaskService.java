package project.daihao18.panel.common.schedule.tasks;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.MessageEntity;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import project.daihao18.panel.common.utils.EmailUtil;
import project.daihao18.panel.common.utils.FlowSizeConverterUtil;
import project.daihao18.panel.entity.Ticket;
import project.daihao18.panel.entity.User;
import project.daihao18.panel.service.ConfigService;
import project.daihao18.panel.service.RedisService;
import project.daihao18.panel.service.UserService;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @ClassName: TelegramJobTaskService
 * @Description:
 * @Author: code18
 * @Date: 2020-11-12 10:53
 */
@Component
@Slf4j
public class TelegramJobTaskService {

    @Value("${setting.enableTGBot}")
    private Boolean enableTGBot;

    @Value("${setting.enableVerifyTG}")
    private Boolean enableVerifyTG;

    @Value("${setting.chatid}")
    private Object chatid;

    @Value("${setting.checkin.enable}")
    private Boolean enableCheckin;

    @Value("${setting.checkin.min}")
    private Integer checkinMin;

    @Value("${setting.checkin.max}")
    private Integer checkinMax;

    @Autowired
    private TelegramBot bot;

    @Autowired
    private UserService userService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private RedisService redisService;

    private static String inviteLink;

    private static int offset = 0;
    // 0 * * * * ?
    public void telegramJob() {
        if (enableTGBot) {
            List<Update> updates = bot.execute(new GetUpdates().limit(100).offset(offset).timeout(0)).updates();
            // ... process updates
            if (ObjectUtil.isNotEmpty(updates)) {
                for (Update update : updates) {
                    log.debug(update.toString());
                    offset = update.updateId() + 1;
                    String text = "";
                    Message message = update.message();
                    if (ObjectUtil.isNotEmpty(message)) {
                        log.debug(message.toString());
                        // 处理私聊bot command消息
                        if (ObjectUtil.isNotEmpty(message.entities()) && message.entities()[0].type().equals(MessageEntity.Type.bot_command) && message.chat().type().equals(Chat.Type.Private)) {
                            if (message.text().startsWith("/start")) {
                                handleStart(message);
                            } else if (message.text().startsWith("/site")) {
                                handleSite(message);
                            } else if (message.text().startsWith("/info")) {
                                handleInfo(message);
                            } else if (message.text().startsWith("/ticket")) {
                                handleTicket(message);
                            } else if (message.text().startsWith("/checkin")) {
                                if (enableCheckin) {
                                    handleCheckIn(message);
                                }
                            } else if (message.text().startsWith("/bc")) {
                                handleBroadCast(message);
                            }
                            // 绑定tg后入群,否则直接拉黑
                        } else if (message.chat().type().equals(Chat.Type.group) || message.chat().type().equals(Chat.Type.supergroup)) {
                            // 获取chat id
                            if (ObjectUtil.isNotEmpty(message.text()) && message.text().startsWith("/chatid")) {
                                bot.execute(new SendMessage(message.chat().id(), message.chat().id().toString()));
                            }
                            if (ObjectUtil.isNotEmpty(message.newChatMembers())) {
                                List<com.pengrad.telegrambot.model.User> tgUsers = Arrays.asList(message.newChatMembers());
                                if (ObjectUtil.isNotEmpty(tgUsers)) {
                                    for (com.pengrad.telegrambot.model.User user : tgUsers) {
                                        User existUser = userService.getUserByTgId(user.id());
                                        if (ObjectUtil.isEmpty(existUser)) {
                                            // 踢出群
                                            bot.execute(new KickChatMember(message.chat().id(), user.id()));
                                            // 从黑名单解封
                                            bot.execute(new UnbanChatMember(message.chat().id(), user.id()));
                                        }
                                        // revokeInviteLink
                                        if (ObjectUtil.isNotEmpty(message.chat().id()) && message.chat().id() < 0 && ObjectUtil.isNotEmpty(inviteLink)) {
                                            bot.execute(new RevokeChatInviteLink(message.chat().id(), inviteLink));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void handleStart(Message message) {
        // 用户启用bot,根据passwd查该用户
        if (message.text().split(" ").length > 1) {
            String passwd = message.text().split(" ")[1];
            User user = userService.getUserByPasswd(passwd);
            if (ObjectUtil.isNotEmpty(user) && ObjectUtil.isEmpty(user.getTgId())) {
                user.setTgId(message.from().id());
                if (userService.updateById(user)) {
                    redisService.del("panel::user::" + user.getId());
                    log.info("用户:" + user.getEmail() + ", 绑定TG成功");
                    bot.execute(new SendMessage(message.from().id(), "绑定成功"));
                    // 发送群组链接
                    if (enableVerifyTG) {
                        // get new invite link
                        inviteLink = bot.execute(new ExportChatInviteLink(chatid)).result();
                        bot.execute(new SendMessage(message.from().id(), "欢迎加入tg群组: " + inviteLink));
                    }
                }
            } else {
                bot.execute(new SendMessage(message.from().id(), "请解绑后重新进行绑定"));
            }
        } else {
            bot.execute(new SendMessage(message.from().id(), "参数错误"));
        }
    }

    private void handleSite(Message message) {
        // 判断该用户是否为有效用户
        User user = userService.getUserByTgId(message.from().id());
        if (ObjectUtil.isEmpty(user)) {
            bot.execute(new SendMessage(message.from().id(), "非法越权"));
            return;
        }
        // handleSite
        bot.execute(new SendMessage(message.from().id(), configService.getValueByName("siteUrl")));
    }

    private void handleInfo(Message message) {
        // 判断该用户是否为有效用户
        User user = userService.getUserByTgId(message.from().id());
        if (ObjectUtil.isEmpty(user)) {
            bot.execute(new SendMessage(message.from().id(), "非法越权"));
            return;
        }
        // handleInfo
        String msg = "注册邮箱: " + user.getEmail() + "\n" +
                "当前余额: " + user.getMoney() + "元" + "\n" +
                "剩余流量: " + FlowSizeConverterUtil.BytesToGb(user.getTransferEnable() - user.getU() - user.getD()) + "GB\n" +
                "到期时间: " + DateUtil.format(user.getExpireIn(), "yyyy-MM-dd HH:mm:ss");
        bot.execute(new SendMessage(message.from().id(), msg));
    }

    private void handleTicket(Message message) {
        // 判断该用户是否为有效用户
        User user = userService.getUserByTgId(message.from().id());
        if (ObjectUtil.isEmpty(user)) {
            bot.execute(new SendMessage(message.from().id(), "非法越权"));
            return;
        }
        // handleTicket
        if (message.text().split(" ").length >= 2) {
            String title = message.text().split(" ")[1].split(":")[0];
            String content = message.text().split(" ")[1].split(":")[1];
            Ticket ticket = new Ticket();
            ticket.setTitle(title);
            ticket.setContent(content);
            userService.saveTicket(user.getId(), ticket, "save");
            bot.execute(new SendMessage(message.from().id(), "工单已提交"));
            // 给管理员发送提醒
            List<User> admins = userService.getAdmins();
            for (User admin : admins) {
                EmailUtil.sendEmail("新的工单提醒~", "有新的工单待处理~", false, admin.getEmail());
                if (ObjectUtil.isNotEmpty(admin.getTgId())) {
                    bot.execute(new SendMessage(admin.getTgId(), "有新的工单待处理~"));
                }
            }
        }
    }

    private void handleCheckIn(Message message) {
        // 判断该用户是否为有效用户
        User user = userService.getUserByTgId(message.from().id());
        if (ObjectUtil.isEmpty(user)) {
            bot.execute(new SendMessage(message.from().id(), "非法越权"));
            return;
        }
        // if class <= 0 or expired, return
        if (user.getClazz() <= 0 || user.getExpireIn().before(new Date())) {
            bot.execute(new SendMessage(message.chat().id(), "当前无会员等级或已过期,请购买套餐后再签到喔~").replyToMessageId(message.messageId()));
            return;
        }
        // handleCheckIn
        Integer mb = RandomUtil.randomInt(checkinMin, checkinMax);
        // if checked in, return
        if (ObjectUtil.isNotEmpty(user.getCheckinTime()) && user.getCheckinTime().after(DateUtil.beginOfDay(new Date()))) {
            bot.execute(new SendMessage(message.chat().id(), "今天已经签到过啦~").replyToMessageId(message.messageId()));
            return;
        }
        user.setTransferEnable(user.getTransferEnable() + mb * 1024 * 1024);
        user.setCheckinTime(new Date());
        if (userService.updateById(user)) {
            redisService.del("panel::user::" + user.getId());
            log.info("用户:" + user.getEmail() + "签到成功,流量" + mb + "mb");
            bot.execute(new SendMessage(message.chat().id(), "签到成功,流量" + mb + "mb").replyToMessageId(message.messageId()));
        }
    }

    private void handleBroadCast(Message message) {
        // 判断该用户是否为管理员
        User user = userService.getUserByTgId(message.from().id());
        if (ObjectUtil.isEmpty(user)) {
            return;
        }
        List<User> admins = userService.getAdmins();
        for (User admin : admins) {
            if (ObjectUtil.equals(admin.getId(), user.getId())) {
                // 是管理员,对所有绑定tg的用户广播tg消息
                List<User> tgUsers = userService.getTGUsers();
                for (User tg : tgUsers) {
                    bot.execute(new SendMessage(tg.getTgId(), message.text().split(" ")[1]));
                }
                break;
            }
        }
    }
}