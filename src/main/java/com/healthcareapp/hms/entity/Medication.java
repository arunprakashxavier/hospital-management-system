package com.healthcareapp.hms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "medications")
@Data
@NoArgsConstructor
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Medication name cannot be blank")
    @Column(nullable = false, length = 200)
    private String medicationName;

    @NotBlank(message = "Dosage cannot be blank")
    @Column(nullable = false, length = 100)
    private String dosage; // e.g., "100mg", "1 tablet", "5ml"

    @NotBlank(message = "Frequency cannot be blank")
    @Column(nullable = false, length = 100)
    private String frequency; // e.g., "Twice daily", "Every 6 hours", "Once at bedtime", "As needed"

    @Column(length = 100) // Optional
    private String duration; // e.g., "7 days", "1 month", "Until finished"

    @Column(length = 500) // Optional
    private String instructions; // e.g., "Take with food", "Avoid alcohol"

    @NotNull(message = "Appointment context is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment; // Link to the specific appointment

    @NotNull(message = "Prescribing doctor is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false) // Store the prescribing doctor directly
    private Doctor prescribingDoctor;

    @NotNull(message = "Patient is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false) // Store the patient directly for easier querying
    private Patient patient;

    @Column(nullable = false, updatable = false)
    private LocalDateTime prescribedDate;

    @PrePersist
    protected void onCreate() {
        prescribedDate = LocalDateTime.now();
    }

    // Constructor example
    public Medication(String medicationName, String dosage, String frequency, String duration, String instructions, Appointment appointment, Doctor prescribingDoctor, Patient patient) {
        this.medicationName = medicationName;
        this.dosage = dosage;
        this.frequency = frequency;
        this.duration = duration;
        this.instructions = instructions;
        this.appointment = appointment;
        this.prescribingDoctor = prescribingDoctor;
        this.patient = patient;
    }
}