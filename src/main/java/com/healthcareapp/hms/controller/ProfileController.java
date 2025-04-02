package com.healthcareapp.hms.controller;

import com.healthcareapp.hms.dto.*; // Import necessary DTOs
import com.healthcareapp.hms.security.AppUserDetails;
import com.healthcareapp.hms.service.DoctorService;
import com.healthcareapp.hms.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User Profile Management", description = "APIs for viewing/updating user profiles and changing passwords")
@RestController
@RequestMapping("/api/profile") // Base path for profile related APIs
@SecurityRequirement(name = "bearerAuth") // Assume JWT needed for all profile actions
public class ProfileController {

    @Autowired
    private PatientService patientService;

    @Autowired
    private DoctorService doctorService;

    // --- Get Profile Endpoints ---

    @Operation(summary = "Get Current Patient Profile", description = "Retrieves the profile details for the currently logged-in patient. Requires PATIENT role.")
    @GetMapping("/patient")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<PatientProfileDTO> getCurrentPatientProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails currentUser) {

        PatientProfileDTO profile = patientService.getPatientProfile(currentUser.getUserId(), currentUser);
        return ResponseEntity.ok(profile);
    }

    @Operation(summary = "Get Current Doctor Profile", description = "Retrieves the profile details for the currently logged-in doctor. Requires DOCTOR role.")
    @GetMapping("/doctor")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<DoctorProfileDTO> getCurrentDoctorProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails currentUser) {

        DoctorProfileDTO profile = doctorService.getDoctorProfile(currentUser.getUserId(), currentUser);
        return ResponseEntity.ok(profile);
    }

    // Optional: Admin endpoint to get any patient profile
    @Operation(summary = "[Admin] Get Patient Profile by ID", description = "Retrieves a specific patient's profile by ID. Requires ADMIN role.")
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PatientProfileDTO> getPatientProfileById(
            @PathVariable Long patientId,
            @Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails currentUser) { // Pass current user for service validation consistency

        PatientProfileDTO profile = patientService.getPatientProfile(patientId, currentUser);
        return ResponseEntity.ok(profile);
    }

    // Optional: Admin endpoint to get any doctor profile
    @Operation(summary = "[Admin] Get Doctor Profile by ID", description = "Retrieves a specific doctor's profile by ID. Requires ADMIN role.")
    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DoctorProfileDTO> getDoctorProfileById(
            @PathVariable Long doctorId,
            @Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails currentUser) { // Pass current user for service validation consistency

        DoctorProfileDTO profile = doctorService.getDoctorProfile(doctorId, currentUser);
        return ResponseEntity.ok(profile);
    }


    // --- Update Profile Endpoints ---

    @Operation(summary = "Update Current Patient Profile", description = "Updates allowed fields for the currently logged-in patient. Requires PATIENT role.")
    @PutMapping("/patient")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<PatientProfileDTO> updateCurrentPatientProfile(
            @Valid @RequestBody PatientUpdateDTO updateDto,
            @Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails currentUser) {

        PatientProfileDTO updatedProfile = patientService.updatePatientProfile(currentUser.getUserId(), updateDto, currentUser);
        return ResponseEntity.ok(updatedProfile);
    }

    @Operation(summary = "Update Current Doctor Profile", description = "Updates allowed fields for the currently logged-in doctor. Requires DOCTOR role.")
    @PutMapping("/doctor")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<DoctorProfileDTO> updateCurrentDoctorProfile(
            @Valid @RequestBody DoctorUpdateDTO updateDto,
            @Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails currentUser) {

        DoctorProfileDTO updatedProfile = doctorService.updateDoctorProfile(currentUser.getUserId(), updateDto, currentUser);
        return ResponseEntity.ok(updatedProfile);
    }


    // --- Change Password Endpoint ---

    @Operation(summary = "Change Current User Password", description = "Allows the currently logged-in user (Patient or Doctor) to change their password.")
    @PutMapping("/change-password")
    @PreAuthorize("isAuthenticated()") // Any authenticated user can try
    public ResponseEntity<?> changeCurrentUserPassword(
            @Valid @RequestBody PasswordChangeDTO passwordDto,
            @Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails currentUser) {

        // Call the appropriate service based on user type
        if ("PATIENT".equals(currentUser.getUserType())) {
            patientService.changePassword(currentUser.getUserId(), passwordDto, currentUser);
        } else if ("DOCTOR".equals(currentUser.getUserType())) {
            doctorService.changePassword(currentUser.getUserId(), passwordDto, currentUser);
        }
        // Add Admin password change logic here or in a separate AdminController if needed
        // else if ("ADMIN".equals(currentUser.getUserType())) { ... }
        else {
            // Should not happen if roles are set correctly, but handle defensively
            throw new AccessDeniedException("Password change not supported for this user type.");
        }

        return ResponseEntity.ok("Password changed successfully.");
    }

}