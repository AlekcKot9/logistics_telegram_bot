package com.logistics.service;

import com.logistics.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class LoginService {

    @Autowired
    private AuthService authService;

    @Autowired
    private SessionService sessionService;

    // Храним временные данные для процесса логина
    private final Map<Long, LoginProcess> loginProcesses = new HashMap<>();

    /**
     * Начало процесса логина
     */
    public void startLoginProcess(Long chatId) {
        LoginProcess process = new LoginProcess();
        process.setCurrentStep(LoginStep.WAITING_EMAIL);
        loginProcesses.put(chatId, process);

        sessionService.updateSessionState(chatId, "LOGIN_IN_PROGRESS");
    }

    /**
     * Обработка ввода пользователя в процессе логина
     */
    public String processLoginInput(Long chatId, String input) {
        LoginProcess process = loginProcesses.get(chatId);
        if (process == null) {
            return "Процесс логина не начат. Используйте /login для входа.";
        }

        switch (process.getCurrentStep()) {
            case WAITING_EMAIL:
                return processEmailStep(chatId, process, input);

            case WAITING_PASSWORD:
                return processPasswordStep(chatId, process, input);

            default:
                return "Неизвестный шаг процесса логина.";
        }
    }

    private String processEmailStep(Long chatId, LoginProcess process, String email) {
        if (!isValidEmail(email)) {
            return "❌ Неверный формат email. Пожалуйста, введите корректный email:";
        }

        if (!authService.customerExists(email)) {
            loginProcesses.remove(chatId);
            sessionService.updateSessionState(chatId, "UNAUTHENTICATED");
            return "❌ Пользователь с таким email не найден. Используйте /sign для регистрации.";
        }

        process.setEmail(email);
        process.setCurrentStep(LoginStep.WAITING_PASSWORD);

        return "✅ Email принят. Теперь введите ваш пароль:";
    }

    private String processPasswordStep(Long chatId, LoginProcess process, String password) {
        boolean isAuthenticated = authService.authenticate(chatId, process.getEmail(), password);

        // Очищаем процесс логина независимо от результата
        loginProcesses.remove(chatId);

        if (isAuthenticated) {
            sessionService.updateSessionState(chatId, "AUTHENTICATED");
            Customer customer = authService.getAuthenticatedCustomer(chatId);
            return "✅ Вход выполнен успешно!\n\n" +
                    "Добро пожаловать, " + customer.getFullName() + "!\n" +
                    "Теперь вам доступны все функции бота.";
        } else {
            sessionService.updateSessionState(chatId, "UNAUTHENTICATED");
            return "❌ Неверный пароль. Попробуйте снова используя /login";
        }
    }

    /**
     * Отмена процесса логина
     */
    public void cancelLoginProcess(Long chatId) {
        loginProcesses.remove(chatId);
        sessionService.updateSessionState(chatId, "UNAUTHENTICATED");
    }

    /**
     * Проверка, находится ли пользователь в процессе логина
     */
    public boolean isUserInLoginProcess(Long chatId) {
        return loginProcesses.containsKey(chatId);
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    // Вспомогательные классы для процесса логина
    private static class LoginProcess {
        private LoginStep currentStep;
        private String email;

        public LoginStep getCurrentStep() { return currentStep; }
        public void setCurrentStep(LoginStep currentStep) { this.currentStep = currentStep; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    private enum LoginStep {
        WAITING_EMAIL,
        WAITING_PASSWORD
    }
}