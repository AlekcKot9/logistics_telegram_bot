package com.logistics.service;

import org.springframework.stereotype.*;
import org.telegram.telegrambots.bots.*;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.exceptions.*;

@Service
public class MessageSender {
    private static TelegramLongPollingBot bot;

    public static void setBot(TelegramLongPollingBot botInstance) {
        bot = botInstance;
    }

    public void sendMessage(Long chatId, String text) {
        if (bot == null) {
            System.err.println("Bot is not initialized in MessageSender");
            return;
        }

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
