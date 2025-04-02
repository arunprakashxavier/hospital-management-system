package com.healthcareapp.hms.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AppointmentBookingDTO {

    @NotNull(message = "Doctor ID cannot be null")
    private Long doctorId;

    // Patient ID will likely come from the authenticated user context, not the DTO

    @NotNull(message = "Appointment date and time cannot be null")
    @Future(message = "Requested appointment time must be in the future")
    private LocalDateTime requestedDateTime; // The specific start time patient wants to book

    private String reason; // Optional
}