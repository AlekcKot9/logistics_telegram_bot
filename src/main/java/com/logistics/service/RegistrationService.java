package com.logistics.service;

import com.logistics.DTO.RegistrationDTOs.*;
import com.logistics.component.*;
import com.logistics.model.*;
import com.logistics.repositories.CustomerRepository;
import org.springframework.stereotype.*;
import java.util.*;
import java.util.concurrent.*;

@Service
public class RegistrationService {

    private Map<Long, RegistrationState> userStates = new ConcurrentHashMap<>();
    private Map<Long, UserRegistrationDTO> userData = new ConcurrentHashMap<>();
    private final MessageSender messageSender;
    private final InputValidator inputValidator;
    private final PasswordHasher passwordHasher;
    private final CustomerRepository customerRepository;

    public RegistrationService(MessageSender messageSender,
                               InputValidator inputValidator,
                               PasswordHasher passwordHasher,
                               CustomerRepository customerRepository) {
        this.messageSender = messageSender;
        this.inputValidator = inputValidator;
        this.passwordHasher = passwordHasher;
        this.customerRepository = customerRepository;
    }

    // Метод для проверки, находится ли пользователь в процессе регистрации
    public boolean isUserInRegistrationProcess(Long chatId) {
        return userStates.containsKey(chatId);
    }

    public void startRegistration(Long chatId) {
        userStates.put(chatId, RegistrationState.WAITING_FOR_EMAIL);
        userData.put(chatId, new UserRegistrationDTO());

        messageSender.sendMessageWithCancel(chatId,  // используем новый метод
                "📝 Начинаем регистрацию!\n\n" +
                        "Введите ваш email:");
    }

    public void processInput(Long chatId, String message) {
        RegistrationState currentState = userStates.get(chatId);

        if (currentState == null) {
            messageSender.sendMessage(chatId, "Начните регистрацию с помощью команды /sign");
            return;
        }

        // ПРОВЕРЯЕМ ОТМЕНУ ПЕРВЫМ ДЕЛОМ
        if ("❌ Отмена".equals(message)) {
            cancelRegistration(chatId);
            return;
        }

        UserRegistrationDTO data = userData.get(chatId);

        switch (currentState) {
            case WAITING_FOR_EMAIL:
                if (inputValidator.isValidEmail(message)) {
                    // Проверяем, не занят ли email
                    if (customerRepository.findByEmail(message).isPresent()) {
                        messageSender.sendMessageWithCancel(chatId,
                                "❌ Этот email уже зарегистрирован. Введите другой email:");
                        return;
                    }
                    data.setEmail(message);
                    userStates.put(chatId, RegistrationState.WAITING_FOR_NAME);
                    messageSender.sendMessageWithCancel(chatId,
                            "✅ Email принят!\n\n" +
                                    "Теперь введите ваше полное имя:");
                } else {
                    messageSender.sendMessageWithCancel(chatId,
                            "❌ Неверный формат email. Попробуйте еще раз:");
                }
                break;

            case WAITING_FOR_NAME:
                if (inputValidator.isValidName(message)) {
                    data.setName(message);
                    userStates.put(chatId, RegistrationState.WAITING_FOR_PHONE);
                    messageSender.sendMessageWithCancel(chatId,
                            "✅ Имя принято!\n\n" +
                                    "Теперь введите ваш номер телефона:");
                } else {
                    messageSender.sendMessageWithCancel(chatId,
                            "❌ Имя должно содержать минимум 2 символа. Попробуйте еще раз:");
                }
                break;

            case WAITING_FOR_PHONE:
                if (inputValidator.isValidPhone(message)) {
                    data.setPhone(message);
                    userStates.put(chatId, RegistrationState.WAITING_FOR_ADDRESS);
                    messageSender.sendMessageWithCancel(chatId,
                            "✅ Номер телефона принят!\n\n" +
                                    "Теперь введите ваш адрес:");
                } else {
                    messageSender.sendMessageWithCancel(chatId,
                            "❌ Неверный формат номера телефона. Попробуйте еще раз:");
                }
                break;

            case WAITING_FOR_ADDRESS:
                if (inputValidator.isValidAddress(message)) {
                    data.setAddress(message);
                    userStates.put(chatId, RegistrationState.WAITING_FOR_PASSWORD);
                    messageSender.sendMessageWithCancel(chatId,
                            "✅ Адрес принят!\n\n" +
                                    "Придумайте и введите пароль (минимум 6 символов):");
                } else {
                    messageSender.sendMessageWithCancel(chatId,
                            "❌ Адрес должен содержать минимум 5 символов. Попробуйте еще раз:");
                }
                break;

            case WAITING_FOR_PASSWORD:
                if (inputValidator.isValidPassword(message)) {
                    data.setPassword(message);
                    userStates.put(chatId, RegistrationState.WAITING_FOR_CONFIRMATION);
                    messageSender.sendMessageWithCancel(chatId,
                            "✅ Пароль принят!\n\n" +
                                    "Повторите пароль для подтверждения:");
                } else {
                    messageSender.sendMessageWithCancel(chatId,
                            "❌ Пароль должен содержать минимум 6 символов. Попробуйте еще раз:");
                }
                break;

            case WAITING_FOR_CONFIRMATION:
                if (data.getPassword().equals(message)) {
                    data.setPassword(passwordHasher.hashPassword(message));
                    completeRegistration(chatId, data);
                } else {
                    messageSender.sendMessageWithCancel(chatId,
                            "❌ Пароли не совпадают. Введите пароль еще раз:");
                    userStates.put(chatId, RegistrationState.WAITING_FOR_PASSWORD);
                }
                break;
        }
    }

    private void completeRegistration(Long chatId, UserRegistrationDTO data) {
        try {
            // Создаем нового пользователя в базе данных
            data.setUserId(customerRepository.findMaxCustomerId() + 1);
            Customer customer = new Customer(data);

            // Сохраняем в базу данных
            Customer savedCustomer = customerRepository.save(customer);

            // Очищаем состояния
            userStates.remove(chatId);
            userData.remove(chatId);

            messageSender.sendMessage(chatId,
                    "🎉 Регистрация завершена успешно!\n\n" +
                            "✅ Ваши данные:\n" +
                            "• Email: " + data.getEmail() + "\n" +
                            "• Имя: " + data.getName() + "\n" +
                            "• Телефон: " + data.getPhone() + "\n" +
                            "• Адрес: " + data.getAddress() + "\n\n" +
                            "Добро пожаловать! Ваш ID: " + savedCustomer.getCustomerId(),
                    true); // true - показать главное меню

        } catch (Exception e) {
            messageSender.sendMessage(chatId,
                    "❌ Ошибка при регистрации: " + e.getMessage() + "\n" +
                            "Попробуйте снова с командой /sign",
                    true); // показываем меню даже при ошибке
            userStates.remove(chatId);
            userData.remove(chatId);
        }
    }

    // Метод для отмены регистрации
    public void cancelRegistration(Long chatId) {
        userStates.remove(chatId);
        userData.remove(chatId);
        messageSender.sendMessage(chatId, "❌ Регистрация отменена.", true);
    }
}

enum RegistrationState {
    WAITING_FOR_EMAIL,
    WAITING_FOR_NAME,
    WAITING_FOR_PHONE,
    WAITING_FOR_ADDRESS,
    WAITING_FOR_PASSWORD,
    WAITING_FOR_CONFIRMATION
}