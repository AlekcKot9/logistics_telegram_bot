package com.logistics.bot;

import com.logistics.model.*;
import com.logistics.service.*;
import com.logistics.service.SessionService;
import com.logistics.session.UserSession;
import jakarta.annotation.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private LoginService loginService;

    @Autowired
    private AuthService authService;

    private final RegistrationService registrationService;
    private final MessageSender messageSender;
    private final SessionService sessionService;

    @Value("${bot.username}")
    private String botUsername;

    public TelegramBot(@Value("${bot.token}") String botToken,
                       RegistrationService registrationService,
                       MessageSender messageSender,
                       SessionService sessionService) {
        super(botToken);
        this.registrationService = registrationService;
        this.messageSender = messageSender;
        this.sessionService = sessionService;
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

    private void handleCommands(String text, Long chatId) {
        switch (text) {
            case "/start":
            case "🚀 Старт":
                startSession(chatId);
                sendWelcomeMessage(chatId);
                break;
            case "/help":
            case "❓ Помощь":
                sendHelpMessage(chatId);
                break;
            case "/sign":
            case "📝 Регистрация":
                if (authService.isAuthenticated(chatId)) {
                    sendMessage(chatId, "✅ Вы уже вошли в систему. Для выхода используйте /logout");
                } else {
                    registrationService.startRegistration(chatId);
                }
                break;
            case "/login":
            case "🔐 Вход":
                if (authService.isAuthenticated(chatId)) {
                    sendMessage(chatId, "✅ Вы уже вошли в систему. Для выхода используйте /logout");
                } else {
                    loginService.startLoginProcess(chatId);
                    sendMessage(chatId, "🔐 Вход в систему\n\nПожалуйста, введите ваш email:");
                }
                break;
            case "/profile":
            case "👤 Профиль":
                showUserProfile(chatId);
                break;
            case "ℹ️ О боте":
                sendAboutMessage(chatId);
                break;
            case "❌ Отмена":
                handleCancel(chatId);
                break;
            default:
                sendDefaultMessage(chatId);
                break;
        }
    }



    // Новые методы для обработки логина/логаута
    private void handleLogout(Long chatId) {
        if (authService.isAuthenticated(chatId)) {
            Customer customer = authService.getAuthenticatedCustomer(chatId);
            authService.logout(chatId);
            sessionService.updateSessionState(chatId, "UNAUTHENTICATED");
            sendMessage(chatId, "✅ Вы успешно вышли из системы, " + customer.getFullName() + "!");
        } else {
            sendMessage(chatId, "❌ Вы не вошли в систему.");
        }
    }

    private void showUserProfile(Long chatId) {
        if (authService.isAuthenticated(chatId)) {
            Customer customer = authService.getAuthenticatedCustomer(chatId);
            String profile = "👤 Ваш профиль:\n\n" +
                    "• Имя: " + customer.getFullName() + "\n" +
                    "• Email: " + customer.getEmail() + "\n" +
                    "• Телефон: " + customer.getPhone() + "\n" +
                    "• Адрес: " + customer.getAddress() + "\n\n" +
                    "Статус: ✅ Авторизован";
            sendMessage(chatId, profile);
        } else {
            sendMessage(chatId, "❌ Вы не авторизованы. Используйте /login для входа в систему.");
        }
    }

    private void handleCancel(Long chatId) {
        if (registrationService.isUserInRegistrationProcess(chatId)) {
            registrationService.cancelRegistration(chatId);
            sendMessage(chatId, "❌ Регистрация отменена.");
        } else if (loginService.isUserInLoginProcess(chatId)) {
            loginService.cancelLoginProcess(chatId);
            sendMessage(chatId, "❌ Вход отменен.");
        } else {
            sendMessage(chatId, "❌ Нечего отменять.");
        }
    }

    private void processMessage(String text, Long chatId) {
        // Проверяем команды выхода и статуса сессии
        if ("/logout".equals(text) || "🚪 Выход".equals(text)) {
            handleLogout(chatId);
            return;
        }

        if ("/session".equals(text) || "📊 Статус сессии".equals(text)) {
            showSessionStatus(chatId);
            return;
        }

        // Проверяем активна ли сессия (кроме команды /start)
        if (!"/start".equals(text) && !"🚀 Старт".equals(text)) {
            if (!sessionService.isSessionActive(chatId)) {
                sendSessionExpiredMessage(chatId);
                return;
            }
        }

        // Проверяем процессы (в порядке приоритета)
        if (registrationService.isUserInRegistrationProcess(chatId)) {
            registrationService.processInput(chatId, text);
        } else if (loginService.isUserInLoginProcess(chatId)) {
            String response = loginService.processLoginInput(chatId, text);
            sendMessage(chatId, response);
        } else {
            // Обрабатываем команды и кнопки
            handleCommands(text, chatId);
        }
    }

    private void startSession(Long chatId) {
        sessionService.createSession(chatId);
    }

    private void endSession(Long chatId) {
        sessionService.invalidateSession(chatId);
        sendMessage(chatId, "🔒 Сессия завершена. Для продолжения работы используйте /start");
    }

    private void showSessionStatus(Long chatId) {
        UserSession session = sessionService.getSession(chatId);
        if (session != null) {
            String status = "📊 Статус сессии:\n" +
                    "• Создана: " + session.getCreatedAt() + "\n" +
                    "• Последняя активность: " + session.getLastAccessed() + "\n" +
                    "• Статус: Активна ✅";
            sendMessage(chatId, status);
        } else {
            sendMessage(chatId, "❌ Сессия не активна. Используйте /start для начала работы.");
        }
    }

    private void sendSessionExpiredMessage(Long chatId) {
        String message = "⏰ Время сессии истекло!\n\n" +
                "Ваша сессия была автоматически завершена из-за неактивности.\n" +
                "Для продолжения работы используйте /start";

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText(message);

        // Убираем клавиатуру при истечении сессии
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setSelective(true);
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("🚀 Старт");
        keyboardRows.add(row);
        keyboard.setKeyboard(keyboardRows);

        sendMessage.setReplyMarkup(keyboard);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    private void sendAboutMessage(Long chatId) {
        String aboutText = "🤖 Logistics Bot\n\n" +
                "Этот бот предназначен для управления логистикой и доставками.\n\n" +
                "Возможности:\n" +
                "• 📝 Регистрация пользователей\n" +
                "• 🚚 Управление доставками\n" +
                "• 📊 Отслеживание статусов\n" +
                "• 🔐 Система сессий (30 минут)\n\n" +
                "Для начала работы пройдите регистрацию!";

        sendMessageWithMenu(chatId, aboutText);
    }

    private void sendDefaultMessage(Long chatId) {
        String defaultText = "🤔 Я не понял вашу команду.\n\n" +
                "Используйте меню ниже или команды:\n" +
                "/start - начать работу\n" +
                "/sign - регистрация\n" +
                "/help - помощь\n" +
                "/session - статус сессии\n" +
                "/logout - выход";

        sendMessageWithMenu(chatId, defaultText);
    }

    private void sendMessageWithMenu(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        // Создаем клавиатуру с меню
        ReplyKeyboardMarkup keyboardMarkup = createMainMenuKeyboard(chatId);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendWelcomeMessage(Long chatId) {
        String welcomeText = "👋 Добро пожаловать в Logistics Bot!\n\n" +
                "✅ Сессия начата! Вы можете работать в течение 30 минут.\n\n" +
                "Я помогу вам управлять логистикой и доставками.\n\n";

        if (authService.isAuthenticated(chatId)) {
            Customer customer = authService.getAuthenticatedCustomer(chatId);
            welcomeText += "✅ Вы вошли как: " + customer.getFullName() + "\n\n";
        } else {
            welcomeText += "🔐 Для доступа к функциям войдите в систему или зарегистрируйтесь.\n\n";
        }

        welcomeText += "Выберите действие из меню ниже:";

        sendMessageWithMenu(chatId, welcomeText);
    }

    // Обновляем метод sendHelpMessage:
    private void sendHelpMessage(Long chatId) {
        String helpText = "📋 Доступные команды:\n\n";

        if (authService.isAuthenticated(chatId)) {
            helpText += "• 👤 Профиль - посмотреть свой профиль\n";
        } else {
            helpText += "• 📝 Регистрация - зарегистрироваться в системе\n";
            helpText += "• 🔐 Вход - войти в систему\n";
        }

        helpText += "• ❓ Помощь - показать это сообщение\n" +
                "• ℹ️ О боте - информация о боте\n" +
                "• 📊 Статус сессии - показать статус текущей сессии\n" +
                "• 🚪 Выход - завершить текущую сессию\n\n" +
                "Или используйте команды:\n" +
                "/sign - регистрация\n" +
                "/login - вход\n" +
                "/profile - профиль\n" +
                "/help - помощь\n" +
                "/session - статус сессии\n" +
                "/logout - выход";

        sendMessageWithMenu(chatId, helpText);
    }

    // Обновляем метод createMainMenuKeyboard:
    private ReplyKeyboardMarkup createMainMenuKeyboard(Long chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        // Первый ряд - основные действия
        KeyboardRow row1 = new KeyboardRow();
        if (authService.isAuthenticated(chatId)) {
            row1.add("👤 Профиль");
            row1.add("❓ Помощь");
        } else {
            row1.add("📝 Регистрация");
            row1.add("🔐 Вход");
        }

        // Второй ряд
        KeyboardRow row2 = new KeyboardRow();
        row2.add("ℹ️ О боте");
        row2.add("🚀 Старт");

        // Третий ряд (управление сессией)
        KeyboardRow row3 = new KeyboardRow();
        row3.add("📊 Статус сессии");
        row3.add("🚪 Выход");

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }
}