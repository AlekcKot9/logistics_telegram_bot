package com.logistics.service;

import com.logistics.DTO.RegistrationDTOs.*;
import com.logistics.component.*;
import org.springframework.stereotype.*;
import java.util.*;
import java.util.concurrent.*;

@Service
public class RegistrationService {

    private Map<Long, RegistrationState> userStates = new ConcurrentHashMap<>();
    private Map<Long, UserRegistrationDTO> userData = new ConcurrentHashMap<>();
    private final MessageSender messageSender;
    private final InputValidator inputValidator;

    public RegistrationService(MessageSender messageSender, InputValidator inputValidator) {
        this.messageSender = messageSender;
        this.inputValidator = inputValidator;
    }

    // Метод для проверки, находится ли пользователь в процессе регистрации
    public boolean isUserInRegistrationProcess(Long chatId) {
        return userStates.containsKey(chatId);
    }

    public void startRegistration(Long chatId) {
        userStates.put(chatId, RegistrationState.WAITING_FOR_EMAIL);
        userData.put(chatId, new UserRegistrationDTO());

        messageSender.sendMessage(chatId,
                "📝 Начинаем регистрацию!\n\n" +
                        "Введите ваш email:");
    }

    public void processInput(Long chatId, String message) {
        RegistrationState currentState = userStates.get(chatId);

        if (currentState == null) {
            messageSender.sendMessage(chatId, "Начните регистрацию с помощью команды /sign");
            return;
        }

        UserRegistrationDTO data = userData.get(chatId);

        switch (currentState) {
            case WAITING_FOR_EMAIL:
                if (inputValidator.isValidEmail(message)) {
                    data.setEmail(message);
                    userStates.put(chatId, RegistrationState.WAITING_FOR_NAME);
                    messageSender.sendMessage(chatId, "✅ Email принят!\n\nТеперь введите ваше полное имя:");
                } else {
                    messageSender.sendMessage(chatId, "❌ Неверный формат email. Попробуйте еще раз:");
                }
                break;

            case WAITING_FOR_NAME:
                if (inputValidator.isValidName(message)) {
                    data.setName(message);
                    userStates.put(chatId, RegistrationState.WAITING_FOR_PASSWORD);
                    messageSender.sendMessage(chatId, "✅ Имя принято!\n\nПридумайте и введите пароль (минимум 6 символов):");
                } else {
                    messageSender.sendMessage(chatId, "❌ Имя должно содержать минимум 2 символа. Попробуйте еще раз:");
                }
                break;

            case WAITING_FOR_PASSWORD:
                if (inputValidator.isValidPassword(message)) {
                    data.setPassword(message);
                    userStates.put(chatId, RegistrationState.WAITING_FOR_CONFIRMATION);
                    messageSender.sendMessage(chatId, "✅ Пароль принят!\n\nПовторите пароль для подтверждения:");
                } else {
                    messageSender.sendMessage(chatId, "❌ Пароль должен содержать минимум 6 символов. Попробуйте еще раз:");
                }
                break;

            case WAITING_FOR_CONFIRMATION:
                if (data.getPassword().equals(message)) {
                    completeRegistration(chatId, data);
                } else {
                    messageSender.sendMessage(chatId, "❌ Пароли не совпадают. Введите пароль еще раз:");
                    userStates.put(chatId, RegistrationState.WAITING_FOR_PASSWORD);
                }
                break;
        }
    }

    private void completeRegistration(Long chatId, UserRegistrationDTO data) {
        try {
            // Здесь будет вызов вашего сервиса для сохранения пользователя
            // Например: userService.registerUser(data);

            // Очищаем состояния
            userStates.remove(chatId);
            userData.remove(chatId);

            messageSender.sendMessage(chatId,
                    "🎉 Регистрация завершена успешно!\n\n" +
                            "✅ Ваши данные:\n" +
                            "• Email: " + data.getEmail() + "\n" +
                            "• Имя: " + data.getName() + "\n\n" +
                            "Добро пожаловать!");

        } catch (Exception e) {
            messageSender.sendMessage(chatId,
                    "❌ Ошибка при регистрации: " + e.getMessage() + "\n" +
                            "Попробуйте снова с командой /sign");
            userStates.remove(chatId);
            userData.remove(chatId);
        }
    }

    // Метод для отмены регистрации (можно добавить команду /cancel)
    public void cancelRegistration(Long chatId) {
        userStates.remove(chatId);
        userData.remove(chatId);
        messageSender.sendMessage(chatId, "❌ Регистрация отменена.");
    }
}

enum RegistrationState {
    WAITING_FOR_EMAIL,
    WAITING_FOR_NAME,
    WAITING_FOR_PASSWORD,
    WAITING_FOR_CONFIRMATION
}