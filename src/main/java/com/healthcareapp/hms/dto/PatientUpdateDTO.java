package com.healthcareapp.hms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PatientUpdateDTO {

    // Example editable fields - add/remove as needed
    @NotBlank(message = "Address cannot be blank")
    @Size(max=255, message = "Address too long")
    private String address;

    // Making phone number updatable requires checking uniqueness again in service layer
    @NotBlank(message = "Personal number cannot be blank")
    @Pattern(regexp = "^\\+?[0-9. ()-]{7,25}$", message = "Invalid phone number format")
    @Size(max=20, message = "Personal number too long")
    private String personalNumber;

    // Guardian details might also be updatable
    @NotBlank(message = "Guardian name cannot be blank")
    @Size(max=100)
    private String guardianName;

    @NotBlank(message = "Guardian relation cannot be blank")
    @Size(max=50)
    private String guardianRelation;

    @NotBlank(message = "Guardian phone number cannot be blank")
    @Pattern(regexp = "^\\+?[0-9. ()-]{7,25}$", message = "Invalid phone number format")
    @Size(max=25)
    private String guardianPhoneNumber;

    // Note: Email/Name/DOB changes often have different processes and aren't included here.
    // Password change will be handled separately.
}