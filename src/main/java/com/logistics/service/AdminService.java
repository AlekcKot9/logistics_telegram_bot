package com.logistics.service;

import com.logistics.model.Admin;
import com.logistics.model.Order;
import com.logistics.model.Vehicle;
import com.logistics.repositories.AdminRepository;
import com.logistics.repositories.OrderRepository;
import com.logistics.repositories.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    private final Map<Long, Boolean> adminSessions = new HashMap<>();
    private final Map<Long, String> adminLoginProcess = new HashMap<>();

    public boolean authenticateAdmin(Integer adminId, String password) {
        return adminRepository.findByAdminIdAndPassword(adminId, password).isPresent();
    }

    // Добавляем метод для проверки существования администратора по ID
    public boolean adminExists(Integer adminId) {
        return adminRepository.existsByCustomerId(adminId);
    }

    public void startAdminLogin(Long chatId) {
        adminLoginProcess.put(chatId, "AWAITING_ADMIN_ID");
    }

    public boolean isAdminInLoginProcess(Long chatId) {
        return adminLoginProcess.containsKey(chatId);
    }

    public void cancelAdminLogin(Long chatId) {
        adminLoginProcess.remove(chatId);
        adminSessions.remove(chatId);
    }

    public boolean isAdminAuthenticated(Long chatId) {
        return adminSessions.getOrDefault(chatId, false);
    }

    public void logoutAdmin(Long chatId) {
        adminSessions.remove(chatId);
        adminLoginProcess.remove(chatId);
    }

    public String processAdminLoginInput(Long chatId, String input) {
        String currentStep = adminLoginProcess.get(chatId);

        if (currentStep == null) {
            return "❌ Ошибка процесса входа. Попробуйте снова.";
        }

        // Проверяем, начинается ли состояние с "AWAITING_ADMIN_ID"
        if (currentStep.equals("AWAITING_ADMIN_ID")) {
            try {
                Integer adminId = Integer.parseInt(input);

                // Проверяем существование администратора по ID
                if (adminExists(adminId)) {
                    adminLoginProcess.put(chatId, "AWAITING_ADMIN_PASSWORD:" + adminId);
                    return "🔐 Вход для администратора\n\nАдминистратор с ID " + adminId + " найден.\n\nВведите пароль:";
                } else {
                    cancelAdminLogin(chatId);
                    return "❌ Администратор с ID " + adminId + " не найден.\n\nПопробуйте снова: /admin";
                }

            } catch (NumberFormatException e) {
                cancelAdminLogin(chatId);
                return "❌ Неверный формат ID. ID должен быть числом.\n\nПопробуйте снова: /admin";
            }
        }
        // Проверяем, начинается ли состояние с "AWAITING_ADMIN_PASSWORD"
        else if (currentStep.startsWith("AWAITING_ADMIN_PASSWORD:")) {
            try {
                Integer adminId = extractIdFromState(currentStep);
                if (adminId != null && authenticateAdmin(adminId, input)) {
                    adminSessions.put(chatId, true);
                    adminLoginProcess.remove(chatId);
                    return "✅ Успешный вход как администратор!\n\nДоступные команды:\n" +
                            "• 📋 Все заказы - просмотр всех заказов\n" +
                            "• 🚗 Весь транспорт - просмотр всего транспорта\n" +
                            "• ✏️ Изменить статус заказа - изменить статус заказа\n" +
                            "• 🔄 Изменить статус транспорта - изменить статус транспорта\n" +
                            "• 🚪 Выход - выход из режима администратора";
                } else {
                    cancelAdminLogin(chatId);
                    return "❌ Неверный пароль. Доступ запрещен.\n\nПопробуйте снова: /admin";
                }
            } catch (Exception e) {
                cancelAdminLogin(chatId);
                return "❌ Ошибка процесса входа. Попробуйте снова: /admin";
            }
        }
        else {
            cancelAdminLogin(chatId);
            return "❌ Ошибка процесса входа. Попробуйте снова: /admin";
        }
    }

    private Integer extractIdFromState(String state) {
        if (state != null && state.contains(":")) {
            try {
                return Integer.parseInt(state.split(":")[1]);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    public boolean updateOrderStatus(Integer orderId, String newStatus) {
        try {
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order != null) {
                order.setStatus(newStatus);
                orderRepository.save(order);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean updateVehicleStatus(Integer vehicleId, String newStatus) {
        try {
            Vehicle vehicle = vehicleRepository.findById(vehicleId).orElse(null);
            if (vehicle != null) {
                vehicle.setStatus(newStatus);
                vehicleRepository.save(vehicle);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public Order getOrderById(Integer orderId) {
        return orderRepository.findById(orderId).orElse(null);
    }

    public Vehicle getVehicleById(Integer vehicleId) {
        return vehicleRepository.findById(vehicleId).orElse(null);
    }
}