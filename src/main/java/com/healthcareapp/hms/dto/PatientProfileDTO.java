package com.healthcareapp.hms.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class PatientProfileDTO {
    private Long id;
    private String name;
    private Integer age;
    private LocalDate dateOfBirth;
    private String gender;
    private String personalNumber; // Usually phone number
    private String address;
    private String email;
    private String guardianName;
    private String guardianRelation;
    private String guardianPhoneNumber;
    private LocalDateTime createdAt;

    // Constructor for easy mapping (optional)
    public PatientProfileDTO(Long id, String name, Integer age, LocalDate dateOfBirth, String gender, String personalNumber, String address, String email, String guardianName, String guardianRelation, String guardianPhoneNumber, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.personalNumber = personalNumber;
        this.address = address;
        this.email = email;
        this.guardianName = guardianName;
        this.guardianRelation = guardianRelation;
        this.guardianPhoneNumber = guardianPhoneNumber;
        this.createdAt = createdAt;
    }
}