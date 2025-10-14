package com.logistics.service;

import com.logistics.model.Order;
import com.logistics.repositories.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    public List<Order> getUserOrders(Integer customerId) {
        return orderRepository.findByCustomerIdOrderByCreateTimeDesc(customerId);
    }

    public List<Order> getUserOrdersByStatus(Integer customerId, String status) {
        return orderRepository.findByCustomerCustomerIdAndStatus(customerId, status);
    }

    public Optional<Order> getOrderById(Integer orderId) {
        return orderRepository.findById(orderId);
    }

    public boolean canUserAccessOrder(Integer customerId, Integer orderId) {
        return orderRepository.existsByIdAndCustomerCustomerId(orderId, customerId);
    }

    public List<Order> getRecentOrders(Integer customerId, int limit) {
        return orderRepository.findRecentOrdersByCustomerId(customerId, limit);
    }

    public long getOrderCountByStatus(Integer customerId, String status) {
        return orderRepository.countByCustomerIdAndStatus(customerId, status);
    }

    public Order saveOrder(Order order) {
        return orderRepository.save(order);
    }

    public void deleteOrder(Integer orderId) {
        orderRepository.deleteById(orderId);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public List<Order> getOrdersByStatus(String status) {
        return orderRepository.findByStatus(status);
    }
}