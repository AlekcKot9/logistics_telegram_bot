package com.logistics.model;

import com.logistics.DTO.RegistrationDTOs.*;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    // Связь один-ко-многим с заказами
    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    private List<Order> orders;

    // Конструктор по умолчанию
    public Customer() {}

    // Конструктор с параметрами
    public Customer(String fullName, String phone, String address, String email, String passwordHash) {
        this.customerId = 1;
        this.fullName = fullName;
        this.phone = phone;
        this.address = address;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public Customer(UserRegistrationDTO data) {
        this.customerId = data.getUserId();
        this.fullName = data.getName();
        this.phone = data.getPhone();
        this.address = data.getAddress();
        this.email = data.getEmail();
        this.passwordHash = data.getPassword();
    }
}