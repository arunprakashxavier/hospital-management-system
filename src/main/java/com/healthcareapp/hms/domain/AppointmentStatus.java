package com.healthcareapp.hms.domain;

/**
 * Represents the possible statuses of an appointment.
 */
public enum AppointmentStatus {
    PENDING,    // Patient has booked, awaiting doctor approval
    SCHEDULED,  // Doctor has approved the appointment
    COMPLETED,  // The appointment has taken place
    CANCELLED,  // The appointment was cancelled by patient or doctor
    REJECTED,    // Doctor rejected the pending appointment request
    NO_SHOW      // When patient didn't show up the appointment
}