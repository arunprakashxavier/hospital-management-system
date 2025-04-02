package com.healthcareapp.hms.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class DoctorProfileDTO {
    private Long id;
    private String name;
    private Integer age;
    private String qualification;
    private String specialization;
    private String phoneNumber;
    private Integer yearsOfExperience;
    private String email;
    private LocalDateTime createdAt;

    // Constructor for easy mapping (optional) - useful if manually mapping in service
    public DoctorProfileDTO(Long id, String name, Integer age, String qualification, String specialization, String phoneNumber, Integer yearsOfExperience, String email, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.qualification = qualification;
        this.specialization = specialization;
        this.phoneNumber = phoneNumber;
        this.yearsOfExperience = yearsOfExperience;
        this.email = email;
        this.createdAt = createdAt;
    }
}