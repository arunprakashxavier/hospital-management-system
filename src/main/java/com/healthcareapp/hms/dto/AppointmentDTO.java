package com.healthcareapp.hms.dto;

import com.healthcareapp.hms.domain.AppointmentStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class AppointmentDTO {
    private Long id;
    private LocalDateTime appointmentDateTime;
    private String reason;
    private AppointmentStatus status;
    private String doctorNotes;
    private LocalDateTime createdAt;

    // Simplified Patient Info
    private Long patientId;
    private String patientName;

    // Simplified Doctor Info
    private Long doctorId;
    private String doctorName;
    private String doctorSpecialization;

    // Constructor to map from Appointment entity (example)
    // We will use mapping libraries like MapStruct or manual mapping in service layer later
    public AppointmentDTO(Long id, LocalDateTime appointmentDateTime, String reason, AppointmentStatus status, String doctorNotes, LocalDateTime createdAt, Long patientId, String patientName, Long doctorId, String doctorName, String doctorSpecialization) {
        this.id = id;
        this.appointmentDateTime = appointmentDateTime;
        this.reason = reason;
        this.status = status;
        this.doctorNotes = doctorNotes;
        this.createdAt = createdAt;
        this.patientId = patientId;
        this.patientName = patientName;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.doctorSpecialization = doctorSpecialization;
    }
}