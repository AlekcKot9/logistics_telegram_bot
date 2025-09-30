package com.logistics.bot;

import com.logistics.service.*;
import jakarta.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final RegistrationService registrationService;
    private final MessageSender messageSender;

    @Value("${bot.username}")
    private String botUsername;

    public TelegramBot(@Value("${bot.token}") String botToken,
                       RegistrationService registrationService,
                       MessageSender messageSender) {
        super(botToken);
        this.registrationService = registrationService;
        this.messageSender = messageSender;
    }

    @PostConstruct
    public void init() {
        MessageSender.setBot(this);
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            processMessage(messageText, chatId);
        }
    }

    private void processMessage(String text, Long chatId) {
        if (registrationService.isUserInRegistrationProcess(chatId)) {
            registrationService.processInput(chatId, text);
        } else {
            // Если нет - обрабатываем команды
            switch (text) {
                case "/start":
                    messageSender.sendMessage(chatId, "Добро пожаловать! Используйте /sign для регистрации.");
                    break;
                case "/help":
                    messageSender.sendMessage(chatId, "Доступные команды:\n/sign - регистрация\n/help - помощь");
                    break;
                case "/login":
                case "/sign":
                    registrationService.startRegistration(chatId);
                    break;
                default:
                    messageSender.sendMessage(chatId, "Используйте /sign для начала регистрации.");
                    break;
            }
        }
    }
}