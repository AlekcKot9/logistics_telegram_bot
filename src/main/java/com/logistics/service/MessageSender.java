package com.logistics.service;

import org.springframework.stereotype.*;
import org.telegram.telegrambots.bots.*;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.*;
import org.telegram.telegrambots.meta.exceptions.*;

import java.util.ArrayList;
import java.util.List;

@Service
public class MessageSender {
    private static TelegramLongPollingBot bot;

    public static void setBot(TelegramLongPollingBot botInstance) {
        bot = botInstance;
    }

    public void sendMessage(Long chatId, String text) {
        sendMessage(chatId, text, false);
    }

    public void sendMessage(Long chatId, String text, boolean withMenu) {
        if (bot == null) {
            System.err.println("Bot is not initialized in MessageSender");
            return;
        }

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        if (withMenu) {
            ReplyKeyboardMarkup keyboardMarkup = createMainMenuKeyboard();
            message.setReplyMarkup(keyboardMarkup);
        }

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private ReplyKeyboardMarkup createMainMenuKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("📝 Регистрация");
        row1.add("❓ Помощь");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("ℹ️ О боте");
        row2.add("🚀 Старт");

        keyboard.add(row1);
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    // Метод для отправки сообщения с клавиатурой отмены (во время регистрации)
    public void sendMessageWithCancel(Long chatId, String text) {
        if (bot == null) {
            System.err.println("Bot is not initialized in MessageSender");
            return;
        }

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("❌ Отмена");
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}