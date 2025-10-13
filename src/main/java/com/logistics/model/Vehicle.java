package com.logistics.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "vehicle")
public class Vehicle {

    @Id
    @Column(name = "vehicle_id")
    private Integer vehicleId;

    @Column(name = "model")
    private String model;

    @Column(name = "license_plate")
    private String licensePlate;

    @Column(name = "capacity_ton")
    private Double capacityTon;

    @Column(name = "status")
    private String status;

    // Связь один-ко-многим с заказами
    @OneToMany(mappedBy = "vehicle", fetch = FetchType.LAZY)
    private List<Order> orders;

    public Vehicle() {}
}