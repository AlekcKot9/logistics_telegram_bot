package com.logistics.DTO.RegistrationDTOs;

import lombok.*;

@Getter
@Setter
@Data
public class UserRegistrationDTO {
    private Integer userId;
    private String email;
    private String name;
    private String password;
    private String phone;
    private String address;
}