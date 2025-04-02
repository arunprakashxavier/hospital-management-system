package com.healthcareapp.hms.entity;

import com.healthcareapp.hms.domain.AppointmentStatus; // Import the enum
import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Data
@NoArgsConstructor
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Patient cannot be null")
    @ManyToOne(fetch = FetchType.LAZY) // Many appointments can belong to one patient
    @JoinColumn(name = "patient_id", nullable = false) // Foreign key column
    private Patient patient;

    @NotNull(message = "Doctor cannot be null")
    @ManyToOne(fetch = FetchType.LAZY) // Many appointments can be handled by one doctor
    @JoinColumn(name = "doctor_id", nullable = false) // Foreign key column
    private Doctor doctor;

    @NotNull(message = "Appointment date and time cannot be null")
    @Future(message = "Appointment date must be in the future") // Basic validation
    @Column(nullable = false)
    private LocalDateTime appointmentDateTime;

    @Column(length = 500) // Optional field for reason for visit
    private String reason;

    @NotNull(message = "Appointment status cannot be null")
    @Enumerated(EnumType.STRING) // Store status as string in the DB (PENDING, SCHEDULED, etc.)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status;

    @Column(length = 500) // Optional field for doctor's notes after completion
    private String doctorNotes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) { // Default status on creation
            status = AppointmentStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructor for essential fields (optional)
    public Appointment(Patient patient, Doctor doctor, LocalDateTime appointmentDateTime, String reason) {
        this.patient = patient;
        this.doctor = doctor;
        this.appointmentDateTime = appointmentDateTime;
        this.reason = reason;
        this.status = AppointmentStatus.PENDING; // Default status
    }
}