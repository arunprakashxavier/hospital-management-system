package com.healthcareapp.hms.controller;

import com.healthcareapp.hms.dto.DoctorRegistrationDTO;
import com.healthcareapp.hms.entity.Doctor;
import com.healthcareapp.hms.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Or use SecurityConfig rules
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Management", description = "APIs for Admin operations (Requires ADMIN role)")
@RestController
@RequestMapping("/api/admin") // Base path for all admin APIs
@SecurityRequirement(name = "bearerAuth") // Add this if using JWT Bearer auth in Swagger later
public class AdminController {

    @Autowired
    private DoctorService doctorService;

    @Operation(summary = "Register a new Doctor", description = "Creates a new doctor account. Requires ADMIN role.")
    @ApiResponse(responseCode = "201", description = "Doctor registered successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    @ApiResponse(responseCode = "401", description = "Unauthorized - Admin credentials required")
    @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role")
    @ApiResponse(responseCode = "409", description = "Email or Phone Number already exists")
    @PostMapping("/doctors/register")
    // Ensure this endpoint is secured (using SecurityConfig or @PreAuthorize)
    // @PreAuthorize("hasRole('ADMIN')") // Example using method-level security
    public ResponseEntity<?> registerDoctor(@Valid @RequestBody DoctorRegistrationDTO registrationDTO) {

        Doctor registeredDoctor = doctorService.registerDoctor(registrationDTO);

        // Avoid sending back the password hash in the response
        // Create a DoctorDTO if needed, or just return a success message/ID
        return new ResponseEntity<>("Doctor registered successfully with ID: " + registeredDoctor.getId(), HttpStatus.CREATED);
    }

    // Add other admin endpoints later (view doctors, view patients, manage settings, etc.)

}