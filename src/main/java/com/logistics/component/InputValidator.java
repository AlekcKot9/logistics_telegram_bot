package com.logistics.component;

import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

@Component
public class InputValidator {

    // Регулярные выражения для валидации
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[+]?[0-9\\s\\-\\(\\)]{10,20}$");

    /**
     * Валидация email
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Валидация имени
     */
    public boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        return name.trim().length() >= 2;
    }

    /**
     * Валидация номера телефона
     */
    public boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        // Убираем все пробелы и дефисы для проверки
        String cleanPhone = phone.replaceAll("[\\s\\-\\(\\)]", "");
        return PHONE_PATTERN.matcher(phone).matches() && cleanPhone.length() >= 10;
    }

    /**
     * Валидация адреса
     */
    public boolean isValidAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return false;
        }
        return address.trim().length() >= 5;
    }

    /**
     * Валидация пароля
     */
    public boolean isValidPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        return password.length() >= 6;
    }

    /**
     * Валидация подтверждения пароля
     */
    public boolean isPasswordConfirmed(String password, String confirmation) {
        return password != null && password.equals(confirmation);
    }
}