package project.daihao18.panel.common.configs;

import com.pengrad.telegrambot.TelegramBot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TelegramBotConfig {

    @Value("${setting.enableTGBot}")
    private Boolean enableTGBot;

    @Value("${setting.botToken}")
    private String botToken;

    @Bean
    public TelegramBot getBot() {
        if (enableTGBot) {
            TelegramBot bot = new TelegramBot(botToken);
            return bot;
        } else {
            return new TelegramBot("");
        }
    }
}