package com.logistics.service;

import com.logistics.model.Customer;
import com.logistics.repositories.CustomerRepository;
import com.logistics.service.SessionService;
import com.logistics.session.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private SessionService sessionService;

    @Mock
    private UserSession userSession;

    @InjectMocks
    private AuthService authService;

    private final Long CHAT_ID = 123456789L;
    private final String EMAIL = "test@example.com";
    private final String PASSWORD = "password123";
    private final String WRONG_PASSWORD = "wrongpassword";
    private final Integer CUSTOMER_ID = 1;

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer();
        testCustomer.setCustomerId(CUSTOMER_ID);
        testCustomer.setEmail(EMAIL);
        testCustomer.setPasswordHash(PASSWORD);
    }

    @Test
    void testAuthenticate_Success() {
        // Arrange
        when(customerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testCustomer));
        when(sessionService.getSession(CHAT_ID)).thenReturn(userSession);

        // Act
        boolean result = authService.authenticate(CHAT_ID, EMAIL, PASSWORD);

        // Assert
        assertTrue(result);
        verify(customerRepository).findByEmail(EMAIL);
        verify(sessionService).getSession(CHAT_ID);
        verify(userSession).setAttribute("authenticated", true);
        verify(userSession).setAttribute("customer", testCustomer);
        verify(userSession).setAttribute("customerId", CUSTOMER_ID);
        verify(userSession).setCurrentState("AUTHENTICATED");
    }

    @Test
    void testAuthenticate_WrongPassword() {
        // Arrange
        when(customerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testCustomer));

        // Act
        boolean result = authService.authenticate(CHAT_ID, EMAIL, WRONG_PASSWORD);

        // Assert
        assertFalse(result);
        verify(customerRepository).findByEmail(EMAIL);
        verify(sessionService, never()).getSession(CHAT_ID);
    }

    @Test
    void testAuthenticate_CustomerNotFound() {
        // Arrange
        when(customerRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // Act
        boolean result = authService.authenticate(CHAT_ID, EMAIL, PASSWORD);

        // Assert
        assertFalse(result);
        verify(customerRepository).findByEmail(EMAIL);
        verify(sessionService, never()).getSession(CHAT_ID);
    }

    @Test
    void testIsAuthenticated_False_WhenSessionNull() {
        // Arrange
        when(sessionService.getSession(CHAT_ID)).thenReturn(null);

        // Act
        boolean result = authService.isAuthenticated(CHAT_ID);

        // Assert
        assertFalse(result);
        verify(sessionService).getSession(CHAT_ID);
    }

    @Test
    void testIsAuthenticated_False_WhenAuthenticatedAttributeNull() {
        // Arrange
        when(sessionService.getSession(CHAT_ID)).thenReturn(userSession);
        when(userSession.getAttribute("authenticated")).thenReturn(null);

        // Act
        boolean result = authService.isAuthenticated(CHAT_ID);

        // Assert
        assertFalse(result);
        verify(sessionService).getSession(CHAT_ID);
        verify(userSession).getAttribute("authenticated");
    }

    @Test
    void testGetAuthenticatedCustomer_WhenSessionNull() {
        // Arrange
        when(sessionService.getSession(CHAT_ID)).thenReturn(null);

        // Act
        Customer result = authService.getAuthenticatedCustomer(CHAT_ID);

        // Assert
        assertNull(result);
        verify(sessionService).getSession(CHAT_ID);
        verify(userSession, never()).getAttribute("authenticated");
        verify(userSession, never()).getAttribute("customer");
    }

    @Test
    void testLogout_Success() {
        // Arrange
        when(sessionService.getSession(CHAT_ID)).thenReturn(userSession);

        // Act
        authService.logout(CHAT_ID);

        // Assert
        verify(sessionService).getSession(CHAT_ID);
        verify(userSession).setAttribute("authenticated", false);
        verify(userSession).removeAttribute("customer");
        verify(userSession).removeAttribute("customerId");
        verify(userSession).setCurrentState("UNAUTHENTICATED");
    }

    @Test
    void testLogout_WhenSessionNull() {
        // Arrange
        when(sessionService.getSession(CHAT_ID)).thenReturn(null);

        // Act
        authService.logout(CHAT_ID);

        // Assert
        verify(sessionService).getSession(CHAT_ID);
        // Не должно быть вызовов методов сессии, так как сессия null
        verify(userSession, never()).setAttribute(anyString(), any());
        verify(userSession, never()).removeAttribute(anyString());
        verify(userSession, never()).setCurrentState(anyString());
    }

    @Test
    void testCustomerExists_True() {
        // Arrange
        when(customerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testCustomer));

        // Act
        boolean result = authService.customerExists(EMAIL);

        // Assert
        assertTrue(result);
        verify(customerRepository).findByEmail(EMAIL);
    }

    @Test
    void testCustomerExists_False() {
        // Arrange
        when(customerRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // Act
        boolean result = authService.customerExists(EMAIL);

        // Assert
        assertFalse(result);
        verify(customerRepository).findByEmail(EMAIL);
    }

    @Test
    void testAuthenticate_EdgeCase_EmptyEmail() {
        // Arrange
        when(customerRepository.findByEmail("")).thenReturn(Optional.empty());

        // Act
        boolean result = authService.authenticate(CHAT_ID, "", PASSWORD);

        // Assert
        assertFalse(result);
        verify(customerRepository).findByEmail("");
    }

    @Test
    void testAuthenticate_EdgeCase_EmptyPassword() {
        // Arrange
        testCustomer.setPasswordHash("");
        when(customerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testCustomer));

        // Act
        boolean result = authService.authenticate(CHAT_ID, EMAIL, "");

        // Assert
        assertTrue(result);
        verify(customerRepository).findByEmail(EMAIL);
        verify(sessionService).getSession(CHAT_ID);
    }
}