package com.healthcareapp.hms.dto;

import lombok.Data;

@Data
public class JwtAuthResponse {
    private String accessToken;
    private String tokenType = "Bearer"; // Standard prefix

    public JwtAuthResponse(String accessToken) {
        this.accessToken = accessToken;
    }
}
