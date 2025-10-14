package com.logistics.model;

import com.logistics.DTO.RegistrationDTOs.*;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "admin")
public class Admin {

    @Id
    @Column(name = "admin_id")
    private Integer customerId;

    @Column(name = "hash_password")
    private String hashPassword;
}