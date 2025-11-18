package com.logistics.service;

import com.logistics.model.Customer;
import com.logistics.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private AuthService authService;

    @Mock
    private SessionService sessionService;

    @InjectMocks
    private LoginService loginService;

    private final Long CHAT_ID = 123456789L;
    private final String VALID_EMAIL = "test@example.com";
    private final String INVALID_EMAIL = "invalid-email";
    private final String PASSWORD = "password123";
    private final String WRONG_PASSWORD = "wrongpassword";

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer();
        testCustomer.setCustomerId(1);
        testCustomer.setEmail(VALID_EMAIL);
        testCustomer.setFullName("Test User");
    }

    @Test
    void testStartLoginProcess() {
        // Act
        loginService.startLoginProcess(CHAT_ID);

        // Assert
        assertTrue(loginService.isUserInLoginProcess(CHAT_ID));
        verify(sessionService).updateSessionState(CHAT_ID, "LOGIN_IN_PROGRESS");
    }

    @Test
    void testProcessLoginInput_EmailStep_ValidEmail_UserExists() {
        // Arrange
        loginService.startLoginProcess(CHAT_ID);
        when(authService.customerExists(VALID_EMAIL)).thenReturn(true);

        // Act
        String result = loginService.processLoginInput(CHAT_ID, VALID_EMAIL);

        // Assert
        assertTrue(result.contains("✅ Email принят. Теперь введите ваш пароль:"));
        assertTrue(loginService.isUserInLoginProcess(CHAT_ID));
        verify(authService).customerExists(VALID_EMAIL);
        verify(sessionService, never()).updateSessionState(eq(CHAT_ID), eq("UNAUTHENTICATED"));
    }

    @Test
    void testProcessLoginInput_EmailStep_ValidEmail_UserNotExists() {
        // Arrange
        loginService.startLoginProcess(CHAT_ID);
        when(authService.customerExists(VALID_EMAIL)).thenReturn(false);

        // Act
        String result = loginService.processLoginInput(CHAT_ID, VALID_EMAIL);

        // Assert
        assertTrue(result.contains("❌ Пользователь с таким email не найден"));
        assertFalse(loginService.isUserInLoginProcess(CHAT_ID));
        verify(authService).customerExists(VALID_EMAIL);
        verify(sessionService).updateSessionState(CHAT_ID, "UNAUTHENTICATED");
    }

    @Test
    void testProcessLoginInput_EmailStep_InvalidEmail() {
        // Arrange
        loginService.startLoginProcess(CHAT_ID);

        // Act
        String result = loginService.processLoginInput(CHAT_ID, INVALID_EMAIL);

        // Assert
        assertTrue(result.contains("❌ Неверный формат email"));
        assertTrue(loginService.isUserInLoginProcess(CHAT_ID));
        verify(authService, never()).customerExists(anyString());
    }

    @Test
    void testProcessLoginInput_PasswordStep_SuccessfulAuthentication() {
        // Arrange - имитируем процесс после ввода email
        loginService.startLoginProcess(CHAT_ID);
        when(authService.customerExists(VALID_EMAIL)).thenReturn(true);
        loginService.processLoginInput(CHAT_ID, VALID_EMAIL); // Переходим на шаг пароля

        when(authService.authenticate(CHAT_ID, VALID_EMAIL, PASSWORD)).thenReturn(true);
        when(authService.getAuthenticatedCustomer(CHAT_ID)).thenReturn(testCustomer);

        // Act
        String result = loginService.processLoginInput(CHAT_ID, PASSWORD);

        // Assert
        assertTrue(result.contains("✅ Вход выполнен успешно!"));
        assertTrue(result.contains("Добро пожаловать, " + testCustomer.getFullName()));
        assertFalse(loginService.isUserInLoginProcess(CHAT_ID));
        verify(authService).authenticate(CHAT_ID, VALID_EMAIL, PASSWORD);
        verify(authService).getAuthenticatedCustomer(CHAT_ID);
        verify(sessionService).updateSessionState(CHAT_ID, "AUTHENTICATED");
    }

    @Test
    void testProcessLoginInput_PasswordStep_FailedAuthentication() {
        // Arrange - имитируем процесс после ввода email
        loginService.startLoginProcess(CHAT_ID);
        when(authService.customerExists(VALID_EMAIL)).thenReturn(true);
        loginService.processLoginInput(CHAT_ID, VALID_EMAIL); // Переходим на шаг пароля

        when(authService.authenticate(CHAT_ID, VALID_EMAIL, WRONG_PASSWORD)).thenReturn(false);

        // Act
        String result = loginService.processLoginInput(CHAT_ID, WRONG_PASSWORD);

        // Assert
        assertTrue(result.contains("❌ Неверный пароль"));
        assertFalse(loginService.isUserInLoginProcess(CHAT_ID));
        verify(authService).authenticate(CHAT_ID, VALID_EMAIL, WRONG_PASSWORD);
        verify(authService, never()).getAuthenticatedCustomer(CHAT_ID);
        verify(sessionService).updateSessionState(CHAT_ID, "UNAUTHENTICATED");
    }

    @Test
    void testProcessLoginInput_ProcessNotStarted() {
        // Act
        String result = loginService.processLoginInput(CHAT_ID, "any input");

        // Assert
        assertTrue(result.contains("Процесс логина не начат"));
        assertFalse(loginService.isUserInLoginProcess(CHAT_ID));
    }

    @Test
    void testProcessLoginInput_EmailStep_EmptyEmail() {
        // Arrange
        loginService.startLoginProcess(CHAT_ID);

        // Act
        String result = loginService.processLoginInput(CHAT_ID, "");

        // Assert
        assertTrue(result.contains("❌ Неверный формат email"));
        assertTrue(loginService.isUserInLoginProcess(CHAT_ID));
    }

    @Test
    void testProcessLoginInput_EmailStep_NullEmail() {
        // Arrange
        loginService.startLoginProcess(CHAT_ID);

        // Act
        String result = loginService.processLoginInput(CHAT_ID, null);

        // Assert
        assertTrue(result.contains("❌ Неверный формат email"));
        assertTrue(loginService.isUserInLoginProcess(CHAT_ID));
    }

    @Test
    void testProcessLoginInput_EmailStep_EmailWithSpaces() {
        // Arrange
        loginService.startLoginProcess(CHAT_ID);

        // Act
        String result = loginService.processLoginInput(CHAT_ID, "  test@example.com  ");

        // Assert
        assertTrue(result.contains("❌ Неверный формат email"));
        assertTrue(loginService.isUserInLoginProcess(CHAT_ID));
    }

    @Test
    void testProcessLoginInput_PasswordStep_EmptyPassword() {
        // Arrange - имитируем процесс после ввода email
        loginService.startLoginProcess(CHAT_ID);
        when(authService.customerExists(VALID_EMAIL)).thenReturn(true);
        loginService.processLoginInput(CHAT_ID, VALID_EMAIL); // Переходим на шаг пароля

        when(authService.authenticate(CHAT_ID, VALID_EMAIL, "")).thenReturn(false);

        // Act
        String result = loginService.processLoginInput(CHAT_ID, "");

        // Assert
        assertTrue(result.contains("❌ Неверный пароль"));
        assertFalse(loginService.isUserInLoginProcess(CHAT_ID));
        verify(authService).authenticate(CHAT_ID, VALID_EMAIL, "");
        verify(sessionService).updateSessionState(CHAT_ID, "UNAUTHENTICATED");
    }

    @Test
    void testProcessLoginInput_PasswordStep_NullPassword() {
        // Arrange - имитируем процесс после ввода email
        loginService.startLoginProcess(CHAT_ID);
        when(authService.customerExists(VALID_EMAIL)).thenReturn(true);
        loginService.processLoginInput(CHAT_ID, VALID_EMAIL); // Переходим на шаг пароля

        when(authService.authenticate(eq(CHAT_ID), eq(VALID_EMAIL), isNull())).thenReturn(false);

        // Act
        String result = loginService.processLoginInput(CHAT_ID, null);

        // Assert
        assertTrue(result.contains("❌ Неверный пароль"));
        assertFalse(loginService.isUserInLoginProcess(CHAT_ID));
        verify(authService).authenticate(eq(CHAT_ID), eq(VALID_EMAIL), isNull());
        verify(sessionService).updateSessionState(CHAT_ID, "UNAUTHENTICATED");
    }

    @Test
    void testCancelLoginProcess() {
        // Arrange
        loginService.startLoginProcess(CHAT_ID);
        assertTrue(loginService.isUserInLoginProcess(CHAT_ID));

        // Act
        loginService.cancelLoginProcess(CHAT_ID);

        // Assert
        assertFalse(loginService.isUserInLoginProcess(CHAT_ID));
        verify(sessionService).updateSessionState(CHAT_ID, "UNAUTHENTICATED");
    }

    @Test
    void testCancelLoginProcess_WhenNoProcessExists() {
        // Act
        loginService.cancelLoginProcess(CHAT_ID);

        // Assert
        assertFalse(loginService.isUserInLoginProcess(CHAT_ID));
        verify(sessionService).updateSessionState(CHAT_ID, "UNAUTHENTICATED");
    }

    @Test
    void testIsUserInLoginProcess_WhenInProcess() {
        // Arrange
        loginService.startLoginProcess(CHAT_ID);

        // Act & Assert
        assertTrue(loginService.isUserInLoginProcess(CHAT_ID));
    }

    @Test
    void testIsUserInLoginProcess_WhenNotInProcess() {
        // Act & Assert
        assertFalse(loginService.isUserInLoginProcess(CHAT_ID));
    }

    @Test
    void testMultipleLoginProcesses_DifferentChatIds() {
        // Arrange
        Long chatId1 = 111111111L;
        Long chatId2 = 222222222L;

        // Act
        loginService.startLoginProcess(chatId1);
        loginService.startLoginProcess(chatId2);

        // Assert
        assertTrue(loginService.isUserInLoginProcess(chatId1));
        assertTrue(loginService.isUserInLoginProcess(chatId2));

        // Cancel one process
        loginService.cancelLoginProcess(chatId1);
        assertFalse(loginService.isUserInLoginProcess(chatId1));
        assertTrue(loginService.isUserInLoginProcess(chatId2));
    }

    @Test
    void testProcessLoginInput_AfterCancellation() {
        // Arrange
        loginService.startLoginProcess(CHAT_ID);
        loginService.cancelLoginProcess(CHAT_ID);

        // Act
        String result = loginService.processLoginInput(CHAT_ID, VALID_EMAIL);

        // Assert
        assertTrue(result.contains("Процесс логина не начат"));
        assertFalse(loginService.isUserInLoginProcess(CHAT_ID));
    }

    @Test
    void testCompleteLoginProcess_ClearsProcess() {
        // Arrange - полный процесс успешного логина
        loginService.startLoginProcess(CHAT_ID);
        when(authService.customerExists(VALID_EMAIL)).thenReturn(true);
        when(authService.authenticate(CHAT_ID, VALID_EMAIL, PASSWORD)).thenReturn(true);
        when(authService.getAuthenticatedCustomer(CHAT_ID)).thenReturn(testCustomer);

        // Act - выполняем полный процесс
        loginService.processLoginInput(CHAT_ID, VALID_EMAIL); // Email шаг
        String result = loginService.processLoginInput(CHAT_ID, PASSWORD); // Password шаг

        // Assert
        assertTrue(result.contains("✅ Вход выполнен успешно!"));
        assertFalse(loginService.isUserInLoginProcess(CHAT_ID));

        // Попытка продолжить после завершения
        String afterResult = loginService.processLoginInput(CHAT_ID, "any input");
        assertTrue(afterResult.contains("Процесс логина не начат"));
    }
}