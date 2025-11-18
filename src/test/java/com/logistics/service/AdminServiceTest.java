package com.logistics.service;

import com.logistics.model.Admin;
import com.logistics.model.Order;
import com.logistics.model.Vehicle;
import com.logistics.repositories.AdminRepository;
import com.logistics.repositories.OrderRepository;
import com.logistics.repositories.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private AdminService adminService;

    private final Long CHAT_ID = 123456789L;
    private final Integer ADMIN_ID = 1;
    private final String PASSWORD = "password123";
    private final Integer ORDER_ID = 100;
    private final Integer VEHICLE_ID = 200;

    private Order testOrder;
    private Vehicle testVehicle;

    @BeforeEach
    void setUp() {
        testOrder = new Order();
        testOrder.setId(ORDER_ID);
        testOrder.setStatus("PENDING");

        testVehicle = new Vehicle();
        testVehicle.setVehicleId(VEHICLE_ID);
        testVehicle.setStatus("AVAILABLE");
    }

    @Test
    void testAuthenticateAdmin_Success() {
        when(adminRepository.findByAdminIdAndPassword(ADMIN_ID, PASSWORD))
                .thenReturn(Optional.of(new Admin()));

        boolean result = adminService.authenticateAdmin(ADMIN_ID, PASSWORD);

        assertTrue(result);
        verify(adminRepository).findByAdminIdAndPassword(ADMIN_ID, PASSWORD);
    }

    @Test
    void testAuthenticateAdmin_Failure() {
        when(adminRepository.findByAdminIdAndPassword(ADMIN_ID, PASSWORD))
                .thenReturn(Optional.empty());

        boolean result = adminService.authenticateAdmin(ADMIN_ID, PASSWORD);

        assertFalse(result);
        verify(adminRepository).findByAdminIdAndPassword(ADMIN_ID, PASSWORD);
    }

    @Test
    void testAdminExists_True() {
        when(adminRepository.existsByCustomerId(ADMIN_ID)).thenReturn(true);

        boolean result = adminService.adminExists(ADMIN_ID);

        assertTrue(result);
        verify(adminRepository).existsByCustomerId(ADMIN_ID);
    }

    @Test
    void testAdminExists_False() {
        when(adminRepository.existsByCustomerId(ADMIN_ID)).thenReturn(false);

        boolean result = adminService.adminExists(ADMIN_ID);

        assertFalse(result);
        verify(adminRepository).existsByCustomerId(ADMIN_ID);
    }

    @Test
    void testStartAdminLogin() {
        adminService.startAdminLogin(CHAT_ID);

        assertTrue(adminService.isAdminInLoginProcess(CHAT_ID));
    }

    @Test
    void testIsAdminInLoginProcess_WhenInProcess() {
        adminService.startAdminLogin(CHAT_ID);

        boolean result = adminService.isAdminInLoginProcess(CHAT_ID);

        assertTrue(result);
    }

    @Test
    void testIsAdminInLoginProcess_WhenNotInProcess() {
        boolean result = adminService.isAdminInLoginProcess(CHAT_ID);

        assertFalse(result);
    }

    @Test
    void testCancelAdminLogin() {
        adminService.startAdminLogin(CHAT_ID);
        adminService.cancelAdminLogin(CHAT_ID);

        assertFalse(adminService.isAdminInLoginProcess(CHAT_ID));
        assertFalse(adminService.isAdminAuthenticated(CHAT_ID));
    }

    @Test
    void testIsAdminAuthenticated_WhenAuthenticated() {
        // Настраиваем моки для успешной аутентификации
        when(adminRepository.existsByCustomerId(ADMIN_ID)).thenReturn(true);
        when(adminRepository.findByAdminIdAndPassword(ADMIN_ID, PASSWORD))
                .thenReturn(Optional.of(new Admin()));

        // Запускаем процесс логина
        adminService.startAdminLogin(CHAT_ID);
        adminService.processAdminLoginInput(CHAT_ID, ADMIN_ID.toString());
        adminService.processAdminLoginInput(CHAT_ID, PASSWORD);

        boolean result = adminService.isAdminAuthenticated(CHAT_ID);

        assertTrue(result);
    }

    @Test
    void testIsAdminAuthenticated_WhenNotAuthenticated() {
        boolean result = adminService.isAdminAuthenticated(CHAT_ID);

        assertFalse(result);
    }

    @Test
    void testLogoutAdmin() {
        adminService.startAdminLogin(CHAT_ID);
        adminService.logoutAdmin(CHAT_ID);

        assertFalse(adminService.isAdminInLoginProcess(CHAT_ID));
        assertFalse(adminService.isAdminAuthenticated(CHAT_ID));
    }

    @Test
    void testProcessAdminLoginInput_AdminIdStep_ValidId() {
        adminService.startAdminLogin(CHAT_ID);
        when(adminRepository.existsByCustomerId(ADMIN_ID)).thenReturn(true);

        String result = adminService.processAdminLoginInput(CHAT_ID, ADMIN_ID.toString());

        assertTrue(result.contains("Введите пароль:"));
        assertTrue(adminService.isAdminInLoginProcess(CHAT_ID));
    }

    @Test
    void testProcessAdminLoginInput_AdminIdStep_InvalidId() {
        adminService.startAdminLogin(CHAT_ID);
        when(adminRepository.existsByCustomerId(ADMIN_ID)).thenReturn(false);

        String result = adminService.processAdminLoginInput(CHAT_ID, ADMIN_ID.toString());

        assertTrue(result.contains("не найден"));
        assertFalse(adminService.isAdminInLoginProcess(CHAT_ID));
    }

    @Test
    void testProcessAdminLoginInput_AdminIdStep_InvalidFormat() {
        adminService.startAdminLogin(CHAT_ID);

        String result = adminService.processAdminLoginInput(CHAT_ID, "invalid_id");

        assertTrue(result.contains("Неверный формат ID"));
        assertFalse(adminService.isAdminInLoginProcess(CHAT_ID));
    }

    @Test
    void testProcessAdminLoginInput_PasswordStep_Success() {
        // Настраиваем первый шаг
        adminService.startAdminLogin(CHAT_ID);
        when(adminRepository.existsByCustomerId(ADMIN_ID)).thenReturn(true);
        adminService.processAdminLoginInput(CHAT_ID, ADMIN_ID.toString());

        // Настраиваем второй шаг
        when(adminRepository.findByAdminIdAndPassword(ADMIN_ID, PASSWORD))
                .thenReturn(Optional.of(new Admin()));

        String result = adminService.processAdminLoginInput(CHAT_ID, PASSWORD);

        assertTrue(result.contains("Успешный вход как администратор"));
        assertTrue(adminService.isAdminAuthenticated(CHAT_ID));
        assertFalse(adminService.isAdminInLoginProcess(CHAT_ID));
    }

    @Test
    void testProcessAdminLoginInput_PasswordStep_Failure() {
        // Настраиваем первый шаг
        adminService.startAdminLogin(CHAT_ID);
        when(adminRepository.existsByCustomerId(ADMIN_ID)).thenReturn(true);
        adminService.processAdminLoginInput(CHAT_ID, ADMIN_ID.toString());

        // Настраиваем второй шаг с ошибкой
        when(adminRepository.findByAdminIdAndPassword(ADMIN_ID, PASSWORD))
                .thenReturn(Optional.empty());

        String result = adminService.processAdminLoginInput(CHAT_ID, PASSWORD);

        assertTrue(result.contains("Неверный пароль"));
        assertFalse(adminService.isAdminAuthenticated(CHAT_ID));
        assertFalse(adminService.isAdminInLoginProcess(CHAT_ID));
    }

    @Test
    void testGetAllOrders() {
        List<Order> expectedOrders = Arrays.asList(testOrder, new Order());
        when(orderRepository.findAll()).thenReturn(expectedOrders);

        List<Order> result = adminService.getAllOrders();

        assertEquals(expectedOrders.size(), result.size());
        verify(orderRepository).findAll();
    }

    @Test
    void testGetAllVehicles() {
        List<Vehicle> expectedVehicles = Arrays.asList(testVehicle, new Vehicle());
        when(vehicleRepository.findAll()).thenReturn(expectedVehicles);

        List<Vehicle> result = adminService.getAllVehicles();

        assertEquals(expectedVehicles.size(), result.size());
        verify(vehicleRepository).findAll();
    }

    @Test
    void testUpdateOrderStatus_Success() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        boolean result = adminService.updateOrderStatus(ORDER_ID, "DELIVERED");

        assertTrue(result);
        verify(orderRepository).findById(ORDER_ID);
        verify(orderRepository).save(testOrder);
    }

    @Test
    void testUpdateOrderStatus_OrderNotFound() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        boolean result = adminService.updateOrderStatus(ORDER_ID, "DELIVERED");

        assertFalse(result);
        verify(orderRepository).findById(ORDER_ID);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testUpdateVehicleStatus_Success() {
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(testVehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(testVehicle);

        boolean result = adminService.updateVehicleStatus(VEHICLE_ID, "IN_USE");

        assertTrue(result);
        verify(vehicleRepository).findById(VEHICLE_ID);
        verify(vehicleRepository).save(testVehicle);
    }

    @Test
    void testUpdateVehicleStatus_VehicleNotFound() {
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.empty());

        boolean result = adminService.updateVehicleStatus(VEHICLE_ID, "IN_USE");

        assertFalse(result);
        verify(vehicleRepository).findById(VEHICLE_ID);
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void testGetOrderById_Found() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(testOrder));

        Order result = adminService.getOrderById(ORDER_ID);

        assertNotNull(result);
        assertEquals(ORDER_ID, result.getId());
        verify(orderRepository).findById(ORDER_ID);
    }

    @Test
    void testGetOrderById_NotFound() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        Order result = adminService.getOrderById(ORDER_ID);

        assertNull(result);
        verify(orderRepository).findById(ORDER_ID);
    }

    @Test
    void testGetVehicleById_Found() {
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(testVehicle));

        Vehicle result = adminService.getVehicleById(VEHICLE_ID);

        assertNotNull(result);
        assertEquals(VEHICLE_ID, result.getVehicleId());
        verify(vehicleRepository).findById(VEHICLE_ID);
    }

    @Test
    void testGetVehicleById_NotFound() {
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.empty());

        Vehicle result = adminService.getVehicleById(VEHICLE_ID);

        assertNull(result);
        verify(vehicleRepository).findById(VEHICLE_ID);
    }
}