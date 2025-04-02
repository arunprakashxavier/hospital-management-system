package com.healthcareapp.hms.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class MedicationDTO {

    private Long id;
    private String medicationName;
    private String dosage;
    private String frequency;
    private String duration;
    private String instructions;
    private LocalDateTime prescribedDate;

    // Include info about the prescription context
    private Long appointmentId;
    private Long prescribingDoctorId;
    private String prescribingDoctorName;
    // Usually, the patient knows it's their medication list,
    // but you could add patientId/Name if needed for other contexts (e.g., admin view)
    // private Long patientId;
    // private String patientName;


    // Constructor for mapping (example)
    public MedicationDTO(Long id, String medicationName, String dosage, String frequency, String duration, String instructions, LocalDateTime prescribedDate, Long appointmentId, Long prescribingDoctorId, String prescribingDoctorName) {
        this.id = id;
        this.medicationName = medicationName;
        this.dosage = dosage;
        this.frequency = frequency;
        this.duration = duration;
        this.instructions = instructions;
        this.prescribedDate = prescribedDate;
        this.appointmentId = appointmentId;
        this.prescribingDoctorId = prescribingDoctorId;
        this.prescribingDoctorName = prescribingDoctorName;
    }
}