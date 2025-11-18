package com.logistics.service;

import com.logistics.model.Customer;
import com.logistics.model.Order;
import com.logistics.model.Vehicle;
import com.logistics.repositories.OrderRepository;
import com.logistics.repositories.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OrderCreationService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private AuthService authService;

    // Хранилище состояний создания заказа
    private final Map<Long, OrderCreationState> orderCreationStates = new ConcurrentHashMap<>();

    // Временное хранилище данных заказа
    private final Map<Long, OrderData> orderData = new ConcurrentHashMap<>();

    public void startOrderCreation(Long chatId) {
        orderCreationStates.put(chatId, OrderCreationState.AWAITING_ADDRESS);
        orderData.put(chatId, new OrderData());
    }

    public String processOrderCreationInput(Long chatId, String input) {
        OrderCreationState currentState = orderCreationStates.get(chatId);

        if (currentState == null) {
            return "❌ Сессия создания заказа не начата. Используйте /new_order для начала.";
        }

        switch (currentState) {
            case AWAITING_ADDRESS:
                return processAddress(chatId, input);
            case AWAITING_WEIGHT:
                return processWeight(chatId, input);
            case AWAITING_CONFIRMATION:
                return processConfirmation(chatId, input);
            default:
                cancelOrderCreation(chatId);
                return "❌ Произошла ошибка при создании заказа. Попробуйте снова.";
        }
    }

    private String processAddress(Long chatId, String address) {
        if (address == null || address.trim().isEmpty()) {
            return "❌ Адрес не может быть пустым. Пожалуйста, введите корректный адрес доставки:";
        }

        if (address.length() < 5) {
            return "❌ Адрес слишком короткий. Пожалуйста, введите полный адрес доставки:";
        }

        OrderData data = orderData.get(chatId);
        data.setDeliveryAddress(address.trim());
        orderCreationStates.put(chatId, OrderCreationState.AWAITING_WEIGHT);

        return "✅ Адрес доставки сохранен.\n\n📦 Теперь введите вес посылки в килограммах (целое число):";
    }

    private String processWeight(Long chatId, String weightInput) {
        try {
            int weight = Integer.parseInt(weightInput.trim());

            if (weight <= 2000) {
                return "❌ Минимальный вес заказа 2000 кг:";
            }

            if (weight > 21000) {
                return "❌ Вес слишком большой. Максимальный вес - 21000 кг. Введите меньший вес:";
            }

            OrderData data = orderData.get(chatId);
            data.setTotalWeight(weight);

            // Поиск подходящего транспорта
            List<Vehicle> availableVehicles = vehicleRepository.findAvailableVehiclesWithCapacity(weight / 1000.0);

            if (availableVehicles.isEmpty()) {
                orderCreationStates.put(chatId, OrderCreationState.AWAITING_CONFIRMATION);
                data.setVehicle(null);
                return String.format(
                        "⚠️ Внимание! На данный момент нет свободного транспорта для заказа весом %d кг.\n\n" +
                                "📋 Данные заказа:\n" +
                                "• Адрес доставки: %s\n" +
                                "• Вес: %d кг\n" +
                                "• Транспорт: будет назначен позже\n\n" +
                                "Вы хотите создать заказ? (да/нет)",
                        weight, data.getDeliveryAddress(), weight
                );
            } else {
                Vehicle selectedVehicle = availableVehicles.get(0);
                data.setVehicle(selectedVehicle);

                orderCreationStates.put(chatId, OrderCreationState.AWAITING_CONFIRMATION);

                return String.format(
                        "✅ Найден подходящий транспорт!\n\n" +
                                "📋 Данные заказа:\n" +
                                "• Адрес доставки: %s\n" +
                                "• Вес: %d кг\n" +
                                "• Транспорт: %s (%s, грузоподъемность: %.1f т)\n\n" +
                                "Вы подтверждаете создание заказа? (да/нет)",
                        data.getDeliveryAddress(), weight,
                        selectedVehicle.getModel(), selectedVehicle.getLicensePlate(),
                        selectedVehicle.getCapacityTon()
                );
            }

        } catch (NumberFormatException e) {
            return "❌ Неверный формат веса. Пожалуйста, введите целое число (вес в килограммах):";
        }
    }

    private String processConfirmation(Long chatId, String confirmation) {
        String lowerConfirmation = confirmation.trim().toLowerCase();

        if (lowerConfirmation.equals("да") || lowerConfirmation.equals("yes") ||
                lowerConfirmation.equals("y") || lowerConfirmation.equals("д")) {

            return createOrder(chatId);

        } else if (lowerConfirmation.equals("нет") || lowerConfirmation.equals("no") ||
                lowerConfirmation.equals("n") || lowerConfirmation.equals("н")) {

            cancelOrderCreation(chatId);
            return "❌ Создание заказа отменено.";

        } else {
            return "❌ Пожалуйста, ответьте 'да' или 'нет':";
        }
    }

    private String createOrder(Long chatId) {
        try {
            Customer customer = authService.getAuthenticatedCustomer(chatId);
            OrderData data = orderData.get(chatId);

            // Создание нового заказа
            Order order = new Order();
            order.setId(orderRepository.findMaxOrderId() + 1);
            order.setCreateTime(LocalDateTime.now());
            order.setDeliveryAddress(data.getDeliveryAddress());
            order.setTotalWeight(data.getTotalWeight());
            order.setStatus("создан");
            order.setCustomer(customer);

            if (data.getVehicle() != null) {
                order.setVehicle(data.getVehicle());
            }

            // Генерация ID заказа
            Integer maxOrderId = orderRepository.findMaxOrderId();
            int newOrderId = (maxOrderId != null) ? maxOrderId + 1 : 1;

            // Сохранение заказа
            Order savedOrder = orderRepository.save(order);

            // Очистка состояния
            orderCreationStates.remove(chatId);
            orderData.remove(chatId);

            String successMessage = String.format(
                    "✅ Заказ успешно создан!\n\n" +
                            "📦 Номер заказа: #%d\n" +
                            "📍 Адрес доставки: %s\n" +
                            "⚖️ Вес: %d кг\n" +
                            "📊 Статус: %s\n" +
                            "📅 Дата создания: %s\n\n" +
                            "Вы можете отслеживать статус заказа в разделе 'Мои заказы'.",
                    savedOrder.getId(),
                    savedOrder.getDeliveryAddress(),
                    savedOrder.getTotalWeight(),
                    savedOrder.getStatus(),
                    savedOrder.getCreateTime()
            );

            if (data.getVehicle() == null) {
                successMessage += "\n\n⚠️ Транспорт для заказа будет назначен позже менеджером.";
            }

            return successMessage;

        } catch (Exception e) {
            e.printStackTrace();
            cancelOrderCreation(chatId);
            return "❌ Произошла ошибка при создании заказа. Пожалуйста, попробуйте позже.";
        }
    }

    public void cancelOrderCreation(Long chatId) {
        orderCreationStates.remove(chatId);
        orderData.remove(chatId);
    }

    public boolean isUserInOrderCreationProcess(Long chatId) {
        return orderCreationStates.containsKey(chatId);
    }

    public OrderCreationState getCurrentState(Long chatId) {
        return orderCreationStates.get(chatId);
    }

    // Вспомогательные классы
    public enum OrderCreationState {
        AWAITING_ADDRESS,
        AWAITING_WEIGHT,
        AWAITING_CONFIRMATION
    }

    private static class OrderData {
        private String deliveryAddress;
        private Integer totalWeight;
        private Vehicle vehicle;

        // Getters and setters
        public String getDeliveryAddress() { return deliveryAddress; }
        public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

        public Integer getTotalWeight() { return totalWeight; }
        public void setTotalWeight(Integer totalWeight) { this.totalWeight = totalWeight; }

        public Vehicle getVehicle() { return vehicle; }
        public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }
    }
}