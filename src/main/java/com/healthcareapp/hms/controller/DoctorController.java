package com.healthcareapp.hms.controller;

import com.healthcareapp.hms.dto.AvailableSlotDTO;
import com.healthcareapp.hms.dto.DoctorDTO; // Import DoctorDTO
import com.healthcareapp.hms.entity.Doctor;
import com.healthcareapp.hms.service.AppointmentService; // Inject AppointmentService for slot logic
import com.healthcareapp.hms.service.DoctorService; // Keep DoctorService for other doctor actions
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Doctor Management", description = "APIs related to Doctors (fetching info, availability)")
@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    @Autowired
    private AppointmentService appointmentService; // For specialization & slots

    @Autowired
    private DoctorService doctorService; // For general doctor info if needed later

    @Operation(summary = "Get Doctors by Specialization", description = "Retrieves a list of doctors matching the given specialization. Accessible by authenticated users.")
    @GetMapping("/specialization/{specializationName}")
    @PreAuthorize("isAuthenticated()") // Allow any logged-in user (Patient, Doctor, Admin)
    public ResponseEntity<List<DoctorDTO>> getDoctorsBySpecialization(
            @Parameter(description = "Name of the specialization", required = true)
            @PathVariable String specializationName) {

        List<Doctor> doctors = appointmentService.getDoctorsBySpecialization(specializationName);
        List<DoctorDTO> doctorDTOs = doctors.stream()
                .map(this::mapToDoctorDTO) // Use mapping method
                .collect(Collectors.toList());
        return ResponseEntity.ok(doctorDTOs);
    }

    @Operation(summary = "Get Available Slots for a Doctor", description = "Retrieves a list of available 30-minute appointment slots for a specific doctor on a given date. Accessible by authenticated users.")
    @GetMapping("/{doctorId}/available-slots")
    @PreAuthorize("isAuthenticated()") // Allow any logged-in user
    public ResponseEntity<List<AvailableSlotDTO>> getAvailableSlots(
            @Parameter(description = "ID of the doctor", required = true)
            @PathVariable Long doctorId,
            @Parameter(description = "Date for which to find slots (YYYY-MM-DD)", required = true)
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<AvailableSlotDTO> availableSlots = appointmentService.getAvailableSlots(doctorId, date);
        return ResponseEntity.ok(availableSlots);
    }

    // --- Helper Method for Doctor -> DoctorDTO Mapping ---
    // Consider using MapStruct for more complex objects
    private DoctorDTO mapToDoctorDTO(Doctor doctor) {
        return new DoctorDTO(
                doctor.getId(),
                doctor.getName(),
                doctor.getQualification(),
                doctor.getSpecialization(),
                doctor.getPhoneNumber(),
                doctor.getYearsOfExperience(),
                doctor.getEmail()
        );
    }

    // Add other doctor-related endpoints here later if needed (e.g., get doctor profile)
}