package com.healthcareapp.hms.controller;

import com.healthcareapp.hms.dto.AuthResponseDTO;
import com.healthcareapp.hms.dto.LoginRequestDTO;
import com.healthcareapp.hms.dto.PatientRegistrationDTO;
import com.healthcareapp.hms.entity.Patient;
import com.healthcareapp.hms.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.healthcareapp.hms.dto.JwtAuthResponse; // Create this DTO
import com.healthcareapp.hms.security.JwtTokenProvider; // Import provider
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager; // Import AuthManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication", description = "APIs for User Registration and Login")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private PatientService patientService;
    // Inject DoctorService, AdminService later if needed for specific logic

    @Autowired
    private AuthenticationManager authenticationManager; // Inject AuthenticationManager

    @Autowired
    private JwtTokenProvider tokenProvider; // Inject Token Provider

    // Patient Registration (no change needed here)
    @PostMapping("/patient/register")
    public ResponseEntity<?> registerPatient(@Valid @RequestBody PatientRegistrationDTO registrationDTO) {
        // ... existing registration logic ...
        Patient registeredPatient = patientService.registerPatient(registrationDTO);
        // ... return success response ...
        AuthResponseDTO response = new AuthResponseDTO(
                "Patient registered successfully with ID: " + registeredPatient.getId(),
                true,
                "PATIENT",
                registeredPatient.getId(),
                registeredPatient.getName()
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    // **** MODIFIED Patient Login ****
    @Operation(summary = "Login for Patients (returns JWT)", description = "Authenticates a patient and returns a JWT Bearer token.")
    @ApiResponse(responseCode = "200", description = "Login successful, JWT returned")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    @PostMapping("/patient/login")
    public ResponseEntity<?> authenticatePatient(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {

        // Perform authentication using AuthenticationManager
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequestDTO.getEmail(),
                        loginRequestDTO.getPassword()
                )
        );

        // If authentication successful, set it in SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate JWT token
        String jwt = tokenProvider.generateToken(authentication);

        // Return the token in the response body
        return ResponseEntity.ok(new JwtAuthResponse(jwt));
    }


    // **** ADD Doctor Login endpoint returning JWT ****
    @Operation(summary = "Login for Doctors (returns JWT)", description = "Authenticates a doctor and returns a JWT Bearer token.")
    @ApiResponse(responseCode = "200", description = "Login successful, JWT returned")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    @PostMapping("/doctor/login")
    public ResponseEntity<?> authenticateDoctor(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequestDTO.getEmail(), loginRequestDTO.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);
        return ResponseEntity.ok(new JwtAuthResponse(jwt));
    }

    // **** ADD Admin Login endpoint returning JWT ****
    @Operation(summary = "Login for Admin (returns JWT)", description = "Authenticates the admin and returns a JWT Bearer token.")
    @ApiResponse(responseCode = "200", description = "Login successful, JWT returned")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    @PostMapping("/admin/login")
    public ResponseEntity<?> authenticateAdmin(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequestDTO.getEmail(), loginRequestDTO.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);
        return ResponseEntity.ok(new JwtAuthResponse(jwt));
    }

}