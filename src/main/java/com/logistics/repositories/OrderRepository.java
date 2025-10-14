package com.logistics.repositories;

import com.logistics.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    // Найти все заказы по customer_id
    List<Order> findByCustomerCustomerId(Integer customerId);

    // Найти заказы по статусу
    List<Order> findByStatus(String status);

    // Найти заказы по диапазону дат создания
    List<Order> findByCreateTimeBetween(LocalDateTime startDate, LocalDateTime endDate);

    // Найти заказы по customer_id и статусу
    List<Order> findByCustomerCustomerIdAndStatus(Integer customerId, String status);

    // Найти заказы с весом больше указанного
    List<Order> findByTotalWeightGreaterThan(Integer weight);

    // Найти заказы по vehicle_id
    List<Order> findByVehicleVehicleId(Integer vehicleId);

    // Кастомный запрос: найти заказы с доставкой по определенному адресу
    @Query("SELECT o FROM Order o WHERE o.deliveryAddress LIKE %:address%")
    List<Order> findByDeliveryAddressContaining(@Param("address") String address);

    // Кастомный запрос: подсчитать количество заказов по статусу для пользователя
    @Query("SELECT COUNT(o) FROM Order o WHERE o.customer.customerId = :customerId AND o.status = :status")
    Long countByCustomerIdAndStatus(@Param("customerId") Integer customerId, @Param("status") String status);

    // Кастомный запрос: найти заказы с сортировкой по дате создания (новые сначала)
    @Query("SELECT o FROM Order o WHERE o.customer.customerId = :customerId ORDER BY o.createTime DESC")
    List<Order> findByCustomerIdOrderByCreateTimeDesc(@Param("customerId") Integer customerId);

    // Кастомный запрос: найти последние N заказов пользователя
    @Query(value = "SELECT * FROM orders WHERE customer_id = :customerId ORDER BY creation_date DESC LIMIT :limit", nativeQuery = true)
    List<Order> findRecentOrdersByCustomerId(@Param("customerId") Integer customerId, @Param("limit") int limit);

    // Проверить существование заказа по ID и customer_id (для проверки прав доступа)
    boolean existsByIdAndCustomerCustomerId(Integer orderId, Integer customerId);

    // Найти максимальный order_id
    @Query("SELECT MAX(o.id) FROM Order o")
    Integer findMaxOrderId();
}