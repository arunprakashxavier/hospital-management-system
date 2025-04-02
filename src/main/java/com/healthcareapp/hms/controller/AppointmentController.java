package com.healthcareapp.hms.controller;

import com.healthcareapp.hms.dto.AppointmentBookingDTO;
import com.healthcareapp.hms.dto.AppointmentDTO;
import com.healthcareapp.hms.dto.CompleteAppointmentDTO;
// *** NEW: Import Medication DTOs ***
import com.healthcareapp.hms.dto.MedicationDTO;
import com.healthcareapp.hms.dto.MedicationInputDTO;
// **********************************
import com.healthcareapp.hms.security.AppUserDetails;
import com.healthcareapp.hms.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.healthcareapp.hms.service.AppointmentService;
import com.healthcareapp.hms.dto.AppointmentDTO;
import java.util.List;

import java.util.List;

@Tag(name = "Appointment Management", description = "APIs for managing appointments and related medications") // Updated Tag Description
@RestController
@RequestMapping("/api/appointments")
@SecurityRequirement(name = "bearerAuth")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    // --- Booking (Existing) ---
    @Operation(summary = "Book a new Appointment", description = "Allows an authenticated PATIENT to book an appointment. Requires PATIENT role.")
    @PostMapping("/book")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<AppointmentDTO> bookAppointment(
            @Valid @RequestBody AppointmentBookingDTO bookingDto,
            @Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails currentUser) {
        Long patientId = currentUser.getUserId();
        AppointmentDTO createdAppointment = appointmentService.bookAppointment(patientId, bookingDto);
        return new ResponseEntity<>(createdAppointment, HttpStatus.CREATED);
    }

    // --- Fetching Appointments (Existing) ---
    @Operation(summary = "Get Appointments for Current Patient", description = "Retrieves appointments for the currently logged-in patient. Requires PATIENT role.")
    @GetMapping("/my/patient")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<AppointmentDTO>> getMyAppointmentsPatient(
            @Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails currentUser) {
        Long patientId = currentUser.getUserId();
        List<AppointmentDTO> appointments = appointmentService.getAppointmentsForPatient(patientId);
        return ResponseEntity.ok(appointments);
    }

    @Operation(summary = "Get Appointments for Current Doctor", description = "Retrieves appointments for the currently logged-in doctor. Requires DOCTOR role.")
    @GetMapping("/my/doctor")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<AppointmentDTO>> getMyAppointmentsDoctor(
            @Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails currentUser) {
        Long doctorId = currentUser.getUserId();
        List<AppointmentDTO> appointments = appointmentService.getAppointmentsForDoctor(doctorId);
        return ResponseEntity.ok(appointments);
    }

    // --- Admin/Specific User Getters (Existing) ---
    @Operation(summary = "[Admin] Get Appointments for a Specific Patient", description = "Retrieves appointments for a specific patient by ID. Requires ADMIN role.")
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AppointmentDTO>> getAppointmentsForPatientAdmin(@PathVariable Long patientId) {
        List<AppointmentDTO> appointments = appointmentService.getAppointmentsForPatient(patientId);
        return ResponseEntity.ok(appointments);
    }

    @Operation(summary = "[Admin] Get Appointments for a Specific Doctor", description = "Retrieves appointments for a specific doctor by ID. Requires ADMIN role.")
    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AppointmentDTO>> getAppointmentsForDoctorAdmin(@PathVariable Long doctorId) {
        List<AppointmentDTO> appointments = appointmentService.getAppointmentsForDoctor(doctorId);
        return ResponseEntity.ok(appointments);
    }

    // --- Status Change Endpoints (Existing - Updated to pass currentUser) ---
    @Operation(summary = "Approve a Pending Appointment", description = "Allows a DOCTOR or ADMIN to approve a pending appointment.")
    @PutMapping("/{appointmentId}/approve")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<AppointmentDTO> approveAppointment(
            @PathVariable Long appointmentId,
            @Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails currentUser) {
        AppointmentDTO updatedAppointment = appointmentService.approveAppointment(appointmentId, currentUser);
        return ResponseEntity.ok(updatedAppointment);
    }

    @Operation(summary = "Reject a Pending Appointment", description = "Allows a DOCTOR or ADMIN to reject a pending appointment.")
    @PutMapping("/{appointmentId}/reject")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<AppointmentDTO> rejectAppointment(
            @PathVariable Long appointmentId,
            @Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails currentUser) {
        AppointmentDTO updatedAppointment = appointmentService.rejectAppointment(appointmentId, currentUser);
        return ResponseEntity.ok(updatedAppointment);
    }

    @Operation(summary = "Cancel an Appointment", description = "Allows a PATIENT, DOCTOR or ADMIN to cancel appointment.")
    @PutMapping("/{appointmentId}/cancel")
    @PreAuthorize("hasRole('PATIENT') or hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<AppointmentDTO> cancelAppointment(
            @PathVariable Long appointmentId,
            @Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails currentUser) {
        AppointmentDTO updatedAppointment = appointmentService.cancelAppointment(appointmentId, currentUser);
        return ResponseEntity.ok(updatedAppointment);
    }

    @Operation(summary = "Complete a Scheduled Appointment", description = "Allows a DOCTOR or ADMIN to mark an appointment as completed and add notes.")
    @PutMapping("/{appointmentId}/complete")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<AppointmentDTO> completeAppointment(
            @PathVariable Long appointmentId,
            @Valid @RequestBody CompleteAppointmentDTO completeDto, // Added @Valid
            @Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails currentUser) {
        AppointmentDTO updatedAppointment = appointmentService.completeAppointment(appointmentId, completeDto.getDoctorNotes(), currentUser);
        return ResponseEntity.ok(updatedAppointment);
    }

    // *** NEW: Medication Endpoints ***

    @Operation(summary = "Assign Medications to a Completed Appointment", description = "Allows a DOCTOR or ADMIN to assign one or more medications after an appointment is completed.")
    @PostMapping("/{appointmentId}/medications")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<List<MedicationDTO>> assignMedications(
            @PathVariable Long appointmentId,
            @Valid @RequestBody List<MedicationInputDTO> medicationInputs, // Expecting a List
            @Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails currentUser) {

        List<MedicationDTO> assignedMedications = appointmentService.assignMedicationsToAppointment(appointmentId, medicationInputs, currentUser);
        // Return 201 Created if successful, or 200 OK if preferred
        return new ResponseEntity<>(assignedMedications, HttpStatus.CREATED);
    }

    @Operation(summary = "Get Medications for a Specific Appointment", description = "Retrieves medications prescribed during a specific appointment. Accessible by involved Patient, Doctor, or Admin.")
    @GetMapping("/{appointmentId}/medications")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')") // Service layer handles specific ownership check
    public ResponseEntity<List<MedicationDTO>> getMedicationsForAppointment(
            @PathVariable Long appointmentId,
            @Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails currentUser) {

        List<MedicationDTO> medications = appointmentService.getMedicationsForAppointment(appointmentId, currentUser);
        return ResponseEntity.ok(medications);
    }

    @Operation(summary = "Get All Medications for Current Patient", description = "Retrieves all medications prescribed for the currently logged-in patient. Requires PATIENT role.")
    @GetMapping("/my/patient/medications") // New endpoint route
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<MedicationDTO>> getMyMedications(
            @Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails currentUser) {

        Long patientId = currentUser.getUserId();
        List<MedicationDTO> medications = appointmentService.getMedicationsForPatient(patientId, currentUser); // Pass currentUser for validation consistency
        return ResponseEntity.ok(medications);
    }

    // Optional: Add Admin endpoint to get medications for any patient
    // @GetMapping("/patient/{patientId}/medications")
    // @PreAuthorize("hasRole('ADMIN')")
    // public ResponseEntity<List<MedicationDTO>> getMedicationsForPatientAdmin(...) { ... }

    // *** End NEW Medication Endpoints ***

}