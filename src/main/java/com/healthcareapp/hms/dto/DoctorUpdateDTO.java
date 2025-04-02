package com.healthcareapp.hms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DoctorUpdateDTO {

    // Example editable fields for doctors
    @NotBlank(message = "Phone number cannot be blank")
    @Pattern(regexp = "^\\+?[0-9. ()-]{7,25}$", message = "Invalid phone number format")
    @Size(max=25)
    private String phoneNumber; // Needs uniqueness check in service

    @NotNull(message = "Years of experience cannot be null")
    @Min(value = 0, message = "Experience must be non-negative")
    private Integer yearsOfExperience;

    @NotBlank(message = "Qualification cannot be blank")
    @Size(max=200)
    private String qualification; // Allow updating qualification?

    // Specialization/Age might be less commonly updated by the doctor themselves
}