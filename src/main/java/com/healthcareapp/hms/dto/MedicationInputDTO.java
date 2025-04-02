package com.healthcareapp.hms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MedicationInputDTO {

    @NotBlank(message = "Medication name cannot be blank")
    @Size(max = 200, message = "Medication name too long")
    private String medicationName;

    @NotBlank(message = "Dosage cannot be blank")
    @Size(max = 100, message = "Dosage information too long")
    private String dosage;

    @NotBlank(message = "Frequency cannot be blank")
    @Size(max = 100, message = "Frequency information too long")
    private String frequency;

    @Size(max = 100, message = "Duration information too long")
    private String duration; // Optional

    @Size(max = 500, message = "Instructions too long")
    private String instructions; // Optional
}