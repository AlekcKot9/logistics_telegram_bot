package com.logistics.DTO.RegistrationDTOs;

import lombok.*;

@Data
public class UserDTO {
    private Long id;
    private String email;
    private String name;
    private String token;
}