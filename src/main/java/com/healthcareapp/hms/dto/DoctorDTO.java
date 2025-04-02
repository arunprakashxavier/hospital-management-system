package com.healthcareapp.hms.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DoctorDTO {
    private Long id;
    private String name;
    private String qualification;
    private String specialization;
    private String phoneNumber;
    private Integer yearsOfExperience;
    private String email; // Include email if needed for display/contact

    // Constructor or Mapper can be used for conversion
    public DoctorDTO(Long id, String name, String qualification, String specialization, String phoneNumber, Integer yearsOfExperience, String email) {
        this.id = id;
        this.name = name;
        this.qualification = qualification;
        this.specialization = specialization;
        this.phoneNumber = phoneNumber;
        this.yearsOfExperience = yearsOfExperience;
        this.email = email;
    }
}