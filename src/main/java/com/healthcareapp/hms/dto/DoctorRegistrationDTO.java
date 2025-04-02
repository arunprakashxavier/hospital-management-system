package com.healthcareapp.hms.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class DoctorRegistrationDTO {

    @NotBlank(message = "Name cannot be blank")
    @Size(min = 2, max = 100)
    private String name;

    @NotNull(message = "Age cannot be null")
    @Min(value = 20)
    private Integer age;

    @NotBlank(message = "Qualification cannot be blank")
    private String qualification;

    @NotBlank(message = "Specialization cannot be blank")
    private String specialization;

    @NotBlank(message = "Phone number cannot be blank")
    @Pattern(regexp = "^\\+?[0-9. ()-]{7,25}$", message = "Invalid phone number format")
    private String phoneNumber;

    @NotNull(message = "Years of experience cannot be null")
    @Min(value = 0)
    private Integer yearsOfExperience;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password; // Admin sets the initial password
}