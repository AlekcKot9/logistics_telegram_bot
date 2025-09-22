package com.logistics.bot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Value("${bot.username}")
    private String botUsername;

    public TelegramBot(@Value("${bot.token}") String botToken) {
        super(botToken);
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            String response = processMessage(messageText);

            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(response);

            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    private String processMessage(String text) {
        switch (text) {
            case "/start":
                return "Привет! Я простой бот. Отправь мне любое сообщение.";
            case "/help":
                return "Просто напиши что-нибудь, и я отвечу!";
            default:
                return "Ты написал: " + text;
        }
    }
}