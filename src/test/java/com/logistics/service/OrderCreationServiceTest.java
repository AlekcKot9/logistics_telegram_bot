package com.logistics.service;

import com.logistics.model.Customer;
import com.logistics.model.Order;
import com.logistics.model.Vehicle;
import com.logistics.repositories.OrderRepository;
import com.logistics.repositories.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCreationServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private OrderCreationService orderCreationService;

    private final Long CHAT_ID = 123456789L;
    private final String ADDRESS = "ул. Примерная, д. 123, кв. 45";
    private final String VALID_WEIGHT = "2500";
    private final String TOO_SMALL_WEIGHT = "1500";
    private final String TOO_LARGE_WEIGHT = "22000";
    private final String INVALID_WEIGHT = "abc";

    private Customer testCustomer;
    private Vehicle testVehicle;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer();
        testCustomer.setCustomerId(1);
        testCustomer.setFullName("Test Customer");

        testVehicle = new Vehicle();
        testVehicle.setVehicleId(1);
        testVehicle.setModel("Volvo FH16");
        testVehicle.setLicensePlate("A123BC");
        testVehicle.setCapacityTon(25.0);

        testOrder = new Order();
        testOrder.setId(100);
        testOrder.setCustomer(testCustomer);
        testOrder.setDeliveryAddress(ADDRESS);
        testOrder.setTotalWeight(2500);
        testOrder.setStatus("создан");
        testOrder.setCreateTime(LocalDateTime.now());
    }

    @Test
    void testStartOrderCreation() {
        // Act
        orderCreationService.startOrderCreation(CHAT_ID);

        // Assert
        assertTrue(orderCreationService.isUserInOrderCreationProcess(CHAT_ID));
        assertEquals(OrderCreationService.OrderCreationState.AWAITING_ADDRESS,
                orderCreationService.getCurrentState(CHAT_ID));
    }

    @Test
    void testProcessOrderCreationInput_NoSession() {
        // Act
        String result = orderCreationService.processOrderCreationInput(CHAT_ID, "any input");

        // Assert
        assertTrue(result.contains("❌ Сессия создания заказа не начата"));
        assertFalse(orderCreationService.isUserInOrderCreationProcess(CHAT_ID));
    }

    @Test
    void testProcessAddress_ValidAddress() {
        // Arrange
        orderCreationService.startOrderCreation(CHAT_ID);

        // Act
        String result = orderCreationService.processOrderCreationInput(CHAT_ID, ADDRESS);

        // Assert
        assertTrue(result.contains("✅ Адрес доставки сохранен"));
        assertTrue(result.contains("введите вес посылки"));
        assertEquals(OrderCreationService.OrderCreationState.AWAITING_WEIGHT,
                orderCreationService.getCurrentState(CHAT_ID));
    }

    @Test
    void testProcessAddress_EmptyAddress() {
        // Arrange
        orderCreationService.startOrderCreation(CHAT_ID);

        // Act
        String result = orderCreationService.processOrderCreationInput(CHAT_ID, "");

        // Assert
        assertTrue(result.contains("❌ Адрес не может быть пустым"));
        assertEquals(OrderCreationService.OrderCreationState.AWAITING_ADDRESS,
                orderCreationService.getCurrentState(CHAT_ID));
    }

    @Test
    void testProcessAddress_NullAddress() {
        // Arrange
        orderCreationService.startOrderCreation(CHAT_ID);

        // Act
        String result = orderCreationService.processOrderCreationInput(CHAT_ID, null);

        // Assert
        assertTrue(result.contains("❌ Адрес не может быть пустым"));
        assertEquals(OrderCreationService.OrderCreationState.AWAITING_ADDRESS,
                orderCreationService.getCurrentState(CHAT_ID));
    }


    @Test
    void testProcessAddress_AddressWithSpaces() {
        // Arrange
        orderCreationService.startOrderCreation(CHAT_ID);

        // Act
        String result = orderCreationService.processOrderCreationInput(CHAT_ID, "   " + ADDRESS + "   ");

        // Assert
        assertTrue(result.contains("✅ Адрес доставки сохранен"));
        assertEquals(OrderCreationService.OrderCreationState.AWAITING_WEIGHT,
                orderCreationService.getCurrentState(CHAT_ID));
    }

    @Test
    void testProcessWeight_ValidWeight_WithAvailableVehicle() {
        // Arrange
        orderCreationService.startOrderCreation(CHAT_ID);
        orderCreationService.processOrderCreationInput(CHAT_ID, ADDRESS); // Переходим к весу

        List<Vehicle> availableVehicles = Arrays.asList(testVehicle);
        when(vehicleRepository.findAvailableVehiclesWithCapacity(2.5)).thenReturn(availableVehicles);

        // Act
        String result = orderCreationService.processOrderCreationInput(CHAT_ID, VALID_WEIGHT);

        // Assert
        assertTrue(result.contains("✅ Найден подходящий транспорт"));
        assertTrue(result.contains(ADDRESS));
        assertTrue(result.contains("2500"));
        assertTrue(result.contains(testVehicle.getModel()));
        assertTrue(result.contains(testVehicle.getLicensePlate()));
        assertEquals(OrderCreationService.OrderCreationState.AWAITING_CONFIRMATION,
                orderCreationService.getCurrentState(CHAT_ID));
        verify(vehicleRepository).findAvailableVehiclesWithCapacity(2.5);
    }

    @Test
    void testProcessWeight_ValidWeight_NoAvailableVehicle() {
        // Arrange
        orderCreationService.startOrderCreation(CHAT_ID);
        orderCreationService.processOrderCreationInput(CHAT_ID, ADDRESS); // Переходим к весу

        when(vehicleRepository.findAvailableVehiclesWithCapacity(2.5)).thenReturn(Collections.emptyList());

        // Act
        String result = orderCreationService.processOrderCreationInput(CHAT_ID, VALID_WEIGHT);

        // Assert
        assertTrue(result.contains("⚠️ Внимание! На данный момент нет свободного транспорта"));
        assertTrue(result.contains("будет назначен позже"));
        assertEquals(OrderCreationService.OrderCreationState.AWAITING_CONFIRMATION,
                orderCreationService.getCurrentState(CHAT_ID));
        verify(vehicleRepository).findAvailableVehiclesWithCapacity(2.5);
    }

    @Test
    void testProcessWeight_TooSmallWeight() {
        // Arrange
        orderCreationService.startOrderCreation(CHAT_ID);
        orderCreationService.processOrderCreationInput(CHAT_ID, ADDRESS); // Переходим к весу

        // Act
        String result = orderCreationService.processOrderCreationInput(CHAT_ID, TOO_SMALL_WEIGHT);

        // Assert
        assertTrue(result.contains("❌ Минимальный вес заказа 2000 кг"));
        assertEquals(OrderCreationService.OrderCreationState.AWAITING_WEIGHT,
                orderCreationService.getCurrentState(CHAT_ID));
        verify(vehicleRepository, never()).findAvailableVehiclesWithCapacity(anyDouble());
    }

    @Test
    void testProcessWeight_TooLargeWeight() {
        // Arrange
        orderCreationService.startOrderCreation(CHAT_ID);
        orderCreationService.processOrderCreationInput(CHAT_ID, ADDRESS); // Переходим к весу

        // Act
        String result = orderCreationService.processOrderCreationInput(CHAT_ID, TOO_LARGE_WEIGHT);

        // Assert
        assertTrue(result.contains("❌ Вес слишком большой"));
        assertEquals(OrderCreationService.OrderCreationState.AWAITING_WEIGHT,
                orderCreationService.getCurrentState(CHAT_ID));
        verify(vehicleRepository, never()).findAvailableVehiclesWithCapacity(anyDouble());
    }

    @Test
    void testProcessWeight_InvalidFormat() {
        // Arrange
        orderCreationService.startOrderCreation(CHAT_ID);
        orderCreationService.processOrderCreationInput(CHAT_ID, ADDRESS); // Переходим к весу

        // Act
        String result = orderCreationService.processOrderCreationInput(CHAT_ID, INVALID_WEIGHT);

        // Assert
        assertTrue(result.contains("❌ Неверный формат веса"));
        assertEquals(OrderCreationService.OrderCreationState.AWAITING_WEIGHT,
                orderCreationService.getCurrentState(CHAT_ID));
        verify(vehicleRepository, never()).findAvailableVehiclesWithCapacity(anyDouble());
    }

    @Test
    void testProcessWeight_WithSpaces() {
        // Arrange
        orderCreationService.startOrderCreation(CHAT_ID);
        orderCreationService.processOrderCreationInput(CHAT_ID, ADDRESS); // Переходим к весу

        List<Vehicle> availableVehicles = Arrays.asList(testVehicle);
        when(vehicleRepository.findAvailableVehiclesWithCapacity(2.5)).thenReturn(availableVehicles);

        // Act
        String result = orderCreationService.processOrderCreationInput(CHAT_ID, "  2500  ");

        // Assert
        assertTrue(result.contains("✅ Найден подходящий транспорт"));
        assertEquals(OrderCreationService.OrderCreationState.AWAITING_CONFIRMATION,
                orderCreationService.getCurrentState(CHAT_ID));
    }

    @Test
    void testProcessConfirmation_ConfirmYes() {
        // Arrange - полный процесс до подтверждения
        setupOrderCreationUntilConfirmation();
        when(authService.getAuthenticatedCustomer(CHAT_ID)).thenReturn(testCustomer);
        when(orderRepository.findMaxOrderId()).thenReturn(99);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        String result = orderCreationService.processOrderCreationInput(CHAT_ID, "да");

        // Assert
        assertTrue(result.contains("✅ Заказ успешно создан"));
        assertTrue(result.contains("#100"));
        assertTrue(result.contains(ADDRESS));
        assertTrue(result.contains("2500"));
        assertFalse(orderCreationService.isUserInOrderCreationProcess(CHAT_ID));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void testProcessConfirmation_ConfirmNo() {
        // Arrange - полный процесс до подтверждения
        setupOrderCreationUntilConfirmation();

        // Act
        String result = orderCreationService.processOrderCreationInput(CHAT_ID, "нет");

        // Assert
        assertTrue(result.contains("❌ Создание заказа отменено"));
        assertFalse(orderCreationService.isUserInOrderCreationProcess(CHAT_ID));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testProcessConfirmation_InvalidResponse() {
        // Arrange - полный процесс до подтверждения
        setupOrderCreationUntilConfirmation();

        // Act
        String result = orderCreationService.processOrderCreationInput(CHAT_ID, "может быть");

        // Assert
        assertTrue(result.contains("❌ Пожалуйста, ответьте 'да' или 'нет'"));
        assertEquals(OrderCreationService.OrderCreationState.AWAITING_CONFIRMATION,
                orderCreationService.getCurrentState(CHAT_ID));
    }

    @Test
    void testProcessConfirmation_VariousConfirmations() {
        // Arrange
        String[] confirmations = {"да", "yes", "y", "д", "ДА", "Yes"};

        for (String confirmation : confirmations) {
            setupOrderCreationUntilConfirmation();
            when(authService.getAuthenticatedCustomer(CHAT_ID)).thenReturn(testCustomer);
            when(orderRepository.findMaxOrderId()).thenReturn(99);
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            // Act
            String result = orderCreationService.processOrderCreationInput(CHAT_ID, confirmation);

            // Assert
            assertTrue(result.contains("✅ Заказ успешно создан"));

            // Cleanup for next iteration
            orderCreationService.cancelOrderCreation(CHAT_ID);
        }
    }

    @Test
    void testProcessConfirmation_VariousRejections() {
        // Arrange
        String[] rejections = {"нет", "no", "n", "н", "НЕТ", "No"};

        for (String rejection : rejections) {
            setupOrderCreationUntilConfirmation();

            // Act
            String result = orderCreationService.processOrderCreationInput(CHAT_ID, rejection);

            // Assert
            assertTrue(result.contains("❌ Создание заказа отменено"));

            // Cleanup for next iteration
            orderCreationService.cancelOrderCreation(CHAT_ID);
        }
    }

    @Test
    void testCreateOrder_NoVehicle() {
        // Arrange - процесс без доступного транспорта
        orderCreationService.startOrderCreation(CHAT_ID);
        orderCreationService.processOrderCreationInput(CHAT_ID, ADDRESS);

        when(vehicleRepository.findAvailableVehiclesWithCapacity(2.5)).thenReturn(Collections.emptyList());
        orderCreationService.processOrderCreationInput(CHAT_ID, VALID_WEIGHT);

        when(authService.getAuthenticatedCustomer(CHAT_ID)).thenReturn(testCustomer);
        when(orderRepository.findMaxOrderId()).thenReturn(99);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        String result = orderCreationService.processOrderCreationInput(CHAT_ID, "да");

        // Assert
        assertTrue(result.contains("✅ Заказ успешно создан"));
        assertTrue(result.contains("Транспорт для заказа будет назначен позже"));
        assertFalse(orderCreationService.isUserInOrderCreationProcess(CHAT_ID));
    }

    @Test
    void testCreateOrder_ExceptionDuringCreation() {
        // Arrange - полный процесс до подтверждения
        setupOrderCreationUntilConfirmation();
        when(authService.getAuthenticatedCustomer(CHAT_ID)).thenReturn(testCustomer);
        when(orderRepository.findMaxOrderId()).thenReturn(99);
        when(orderRepository.save(any(Order.class))).thenThrow(new RuntimeException("Database error"));

        // Act
        String result = orderCreationService.processOrderCreationInput(CHAT_ID, "да");

        // Assert
        assertTrue(result.contains("❌ Произошла ошибка при создании заказа"));
        assertFalse(orderCreationService.isUserInOrderCreationProcess(CHAT_ID));
    }

    @Test
    void testCreateOrder_NoAuthenticatedCustomer() {
        // Arrange - полный процесс до подтверждения
        setupOrderCreationUntilConfirmation();
        when(authService.getAuthenticatedCustomer(CHAT_ID)).thenReturn(null);

        // Act
        String result = orderCreationService.processOrderCreationInput(CHAT_ID, "да");

        // Assert
        assertTrue(result.contains("❌ Произошла ошибка при создании заказа"));
        assertFalse(orderCreationService.isUserInOrderCreationProcess(CHAT_ID));
    }

    @Test
    void testCancelOrderCreation() {
        // Arrange
        orderCreationService.startOrderCreation(CHAT_ID);
        assertTrue(orderCreationService.isUserInOrderCreationProcess(CHAT_ID));

        // Act
        orderCreationService.cancelOrderCreation(CHAT_ID);

        // Assert
        assertFalse(orderCreationService.isUserInOrderCreationProcess(CHAT_ID));
        assertNull(orderCreationService.getCurrentState(CHAT_ID));
    }

    @Test
    void testCancelOrderCreation_WhenNoProcess() {
        // Act
        orderCreationService.cancelOrderCreation(CHAT_ID);

        // Assert - не должно быть исключения
        assertFalse(orderCreationService.isUserInOrderCreationProcess(CHAT_ID));
    }

    @Test
    void testIsUserInOrderCreationProcess() {
        // Before starting
        assertFalse(orderCreationService.isUserInOrderCreationProcess(CHAT_ID));

        // After starting
        orderCreationService.startOrderCreation(CHAT_ID);
        assertTrue(orderCreationService.isUserInOrderCreationProcess(CHAT_ID));

        // After cancellation
        orderCreationService.cancelOrderCreation(CHAT_ID);
        assertFalse(orderCreationService.isUserInOrderCreationProcess(CHAT_ID));
    }

    @Test
    void testGetCurrentState() {
        // Before starting
        assertNull(orderCreationService.getCurrentState(CHAT_ID));

        // After starting
        orderCreationService.startOrderCreation(CHAT_ID);
        assertEquals(OrderCreationService.OrderCreationState.AWAITING_ADDRESS,
                orderCreationService.getCurrentState(CHAT_ID));

        // After address
        orderCreationService.processOrderCreationInput(CHAT_ID, ADDRESS);
        assertEquals(OrderCreationService.OrderCreationState.AWAITING_WEIGHT,
                orderCreationService.getCurrentState(CHAT_ID));

        // After cancellation
        orderCreationService.cancelOrderCreation(CHAT_ID);
        assertNull(orderCreationService.getCurrentState(CHAT_ID));
    }

    @Test
    void testMultipleUsersOrderCreation() {
        // Arrange
        Long chatId1 = 111111111L;
        Long chatId2 = 222222222L;

        // Act
        orderCreationService.startOrderCreation(chatId1);
        orderCreationService.startOrderCreation(chatId2);

        // Assert
        assertTrue(orderCreationService.isUserInOrderCreationProcess(chatId1));
        assertTrue(orderCreationService.isUserInOrderCreationProcess(chatId2));
        assertEquals(OrderCreationService.OrderCreationState.AWAITING_ADDRESS,
                orderCreationService.getCurrentState(chatId1));
        assertEquals(OrderCreationService.OrderCreationState.AWAITING_ADDRESS,
                orderCreationService.getCurrentState(chatId2));

        // Cancel one
        orderCreationService.cancelOrderCreation(chatId1);
        assertFalse(orderCreationService.isUserInOrderCreationProcess(chatId1));
        assertTrue(orderCreationService.isUserInOrderCreationProcess(chatId2));
    }

    // Вспомогательный метод для настройки процесса до шага подтверждения
    private void setupOrderCreationUntilConfirmation() {
        orderCreationService.startOrderCreation(CHAT_ID);
        orderCreationService.processOrderCreationInput(CHAT_ID, ADDRESS);

        List<Vehicle> availableVehicles = Arrays.asList(testVehicle);
        when(vehicleRepository.findAvailableVehiclesWithCapacity(2.5)).thenReturn(availableVehicles);
        orderCreationService.processOrderCreationInput(CHAT_ID, VALID_WEIGHT);
    }
}