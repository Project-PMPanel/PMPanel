package project.daihao18.panel.common.runner;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.MessageEntity;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.GetUpdates;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import project.daihao18.panel.common.utils.EmailUtil;
import project.daihao18.panel.common.utils.FlowSizeConverterUtil;
import project.daihao18.panel.entity.Ticket;
import project.daihao18.panel.entity.User;
import project.daihao18.panel.service.ConfigService;
import project.daihao18.panel.service.UserService;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
public class TelegramBotRunner implements ApplicationRunner {

    @Value("${setting.enableTGBot}")
    private Boolean enableTGBot;

    @Autowired
    private TelegramBot bot;

    @Autowired
    private UserService userService;

    @Autowired
    private ConfigService configService;

    @Override
    public void run(ApplicationArguments args) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Lock lock = new ReentrantLock();
                lock.lock();
                try {
                    if (enableTGBot) {
                        runBot();
                    }
                } catch (Exception e) {
                    log.error(e.getMessage() + ", restarting bot...");
                    if (enableTGBot) {
                        runBot();
                    }
                } finally {
                    lock.unlock();
                }
            }
        }).start();
    }

    public void runBot() {
        log.info("telegram bot start");
        int m = 0;
        while (true) {
            List<Update> updates = bot.execute(new GetUpdates().limit(100).offset(m).timeout(0)).updates();
            if (ObjectUtil.isNotEmpty(updates)) {
                for (Update update : updates) {
                    log.debug(update.toString());
                    m = update.updateId() + 1;
                    String text = "";
                    Message message = update.message();
                    if (ObjectUtil.isNotEmpty(message)) {
                        log.debug(message.toString());
                        // 处理私聊bot command消息
                        if (message.entities()[0].type().equals(MessageEntity.Type.bot_command) && message.chat().type().equals(Chat.Type.Private)) {
                            if (message.text().startsWith("/start")) {
                                handleStart(message);
                            } else if (message.text().startsWith("/site")) {
                                handleSite(message);
                            } else if (message.text().startsWith("/info")) {
                                handleInfo(message);
                            } else if (message.text().startsWith("/ticket")) {
                                handleTicket(message);
                            }
                        // 处理群组bot command消息
                        } else if (message.entities()[0].type().equals(MessageEntity.Type.bot_command) && message.chat().type().equals(Chat.Type.supergroup)) {

                        }
                    }
                }
            }
        }
    }

    private void handleStart(Message message) {
        // 用户启用bot,根据uuid查该用户
        String uuid = message.text().split(" ")[1];
        User user = userService.getUserByUUID(uuid);
        if (ObjectUtil.isEmpty(user.getTgId())) {
            user.setTgId(message.from().id());
            if (userService.updateById(user)) {
                log.info("用户:" + user.getEmail() + ", 绑定TG成功");
                bot.execute(new SendMessage(message.from().id(), "绑定成功"));
            }
        } else {
            bot.execute(new SendMessage(message.from().id(), "请解绑后重新进行绑定"));
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
}