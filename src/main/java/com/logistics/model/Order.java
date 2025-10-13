package com.logistics.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Getter
@Setter
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @Column(name = "order_id")
    private Integer id;

    @Column(name = "creation_date")
    private LocalDateTime createTime;

    @Column(name = "total_weight")
    private Integer totalWeight;

    @Column(name = "status")
    private String status;

    @Column(name = "delivery_address")
    private String deliveryAddress;

    // Связь многие-к-одному с Customer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    // Связь многие-к-одному с Vehicle
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    public Order() {}
}