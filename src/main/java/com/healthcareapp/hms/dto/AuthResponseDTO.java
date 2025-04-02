package com.healthcareapp.hms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {
    private String message;
    private boolean success;
    // We might add user details or a JWT token here later
    private String userType; // e.g., "PATIENT", "DOCTOR"
    private Long userId;
    private String userName;
}