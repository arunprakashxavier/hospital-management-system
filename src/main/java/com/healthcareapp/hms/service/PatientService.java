package com.healthcareapp.hms.service;

import com.healthcareapp.hms.dto.*;
import com.healthcareapp.hms.entity.Patient;
import com.healthcareapp.hms.exception.BadRequestException;
import com.healthcareapp.hms.exception.DuplicateResourceException;
import com.healthcareapp.hms.exception.ResourceNotFoundException;
import com.healthcareapp.hms.repository.PatientRepository;
import com.healthcareapp.hms.security.AppUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List; // Make sure List is imported if returning lists later
import java.util.Objects;

@Service
public class PatientService {

    private static final Logger logger = LoggerFactory.getLogger(PatientService.class);

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public Patient registerPatient(PatientRegistrationDTO registrationDTO) {
        // ... (registerPatient logic remains the same) ...
        if (!registrationDTO.getPassword().equals(registrationDTO.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }
        if (patientRepository.existsByEmail(registrationDTO.getEmail())) {
            throw new DuplicateResourceException("Patient", "email", registrationDTO.getEmail());
        }
        if (patientRepository.existsByPersonalNumber(registrationDTO.getPersonalNumber())) {
            throw new DuplicateResourceException("Patient", "personal number", registrationDTO.getPersonalNumber());
        }
        Patient patient = new Patient();
        patient.setName(registrationDTO.getName());
        patient.setAge(registrationDTO.getAge());
        patient.setDateOfBirth(registrationDTO.getDateOfBirth());
        patient.setGender(registrationDTO.getGender());
        patient.setPersonalNumber(registrationDTO.getPersonalNumber());
        patient.setAddress(registrationDTO.getAddress());
        patient.setEmail(registrationDTO.getEmail());
        patient.setGuardianName(registrationDTO.getGuardianName());
        patient.setGuardianRelation(registrationDTO.getGuardianRelation());
        patient.setGuardianPhoneNumber(registrationDTO.getGuardianPhoneNumber());
        patient.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));
        return patientRepository.save(patient);
    }

    // Optional login method (may not be used directly by API controller anymore)
    public AuthResponseDTO loginPatient(LoginRequestDTO loginRequestDTO) {
        // ... (existing logic) ...
        Patient patient = patientRepository.findByEmail(loginRequestDTO.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Login Failed", "email", loginRequestDTO.getEmail()));
        if (passwordEncoder.matches(loginRequestDTO.getPassword(), patient.getPassword())) {
            return new AuthResponseDTO("Patient Login Successful", true, "PATIENT", patient.getId(), patient.getName());
        } else {
            throw new BadRequestException("Invalid email or password");
        }
    }

    // Helper to find patient or throw exception
    private Patient findPatientByIdOrThrow(Long patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", patientId));
    }

    // Existing getPatientById (returns entity)
    public Patient getPatientById(Long id) {
        return findPatientByIdOrThrow(id);
    }


    // --- Profile Management Methods ---

    @Transactional(readOnly = true)
    public PatientProfileDTO getPatientProfile(Long patientId, AppUserDetails currentUser) {
        // 1. Authorization Check
        if (!Objects.equals(currentUser.getUserId(), patientId) && !currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            // Use logger here
            logger.warn("User ID {} attempted to access profile for Patient ID {} without permission.", currentUser.getUserId(), patientId);
            throw new AccessDeniedException("You do not have permission to view this patient's profile.");
        }

        // 2. Fetch patient
        Patient patient = findPatientByIdOrThrow(patientId);

        // 3. Map to DTO
        return mapToPatientProfileDTO(patient);
    }

    @Transactional
    public PatientProfileDTO updatePatientProfile(Long patientId, PatientUpdateDTO updateDto, AppUserDetails currentUser) {
        // 1. Authorization Check
        if (!Objects.equals(currentUser.getUserId(), patientId)) {
            logger.warn("User ID {} attempted to update profile for Patient ID {} without permission.", currentUser.getUserId(), patientId);
            throw new AccessDeniedException("You can only update your own profile.");
        }

        // 2. Fetch patient
        Patient patient = findPatientByIdOrThrow(patientId);

        // 3. Check for uniqueness if personal number is changing
        if (!Objects.equals(patient.getPersonalNumber(), updateDto.getPersonalNumber()) &&
                patientRepository.existsByPersonalNumberAndIdNot(updateDto.getPersonalNumber(), patientId)) {
            throw new DuplicateResourceException("Patient", "personal number", updateDto.getPersonalNumber());
        }

        // 4. Update allowed fields
        patient.setAddress(updateDto.getAddress());
        patient.setPersonalNumber(updateDto.getPersonalNumber());
        patient.setGuardianName(updateDto.getGuardianName());
        patient.setGuardianRelation(updateDto.getGuardianRelation());
        patient.setGuardianPhoneNumber(updateDto.getGuardianPhoneNumber());

        // 5. Save updated patient
        Patient updatedPatient = patientRepository.save(patient);
        logger.info("Profile updated successfully for patient ID: {}", patientId);

        // 6. Map to DTO and return
        return mapToPatientProfileDTO(updatedPatient);
    }

    @Transactional
    public void changePassword(Long patientId, PasswordChangeDTO passwordDto, AppUserDetails currentUser) {
        // 1. Authorization Check
        if (!Objects.equals(currentUser.getUserId(), patientId)) {
            logger.warn("User ID {} attempted to change password for Patient ID {} without permission.", currentUser.getUserId(), patientId);
            throw new AccessDeniedException("You can only change your own password.");
        }

        // 2. Fetch patient
        Patient patient = findPatientByIdOrThrow(patientId);

        // 3. Verify current password
        if (!passwordEncoder.matches(passwordDto.getCurrentPassword(), patient.getPassword())) {
            throw new BadRequestException("Incorrect current password.");
        }

        // 4. Verify new passwords match
        if (!passwordDto.getNewPassword().equals(passwordDto.getConfirmNewPassword())) {
            throw new BadRequestException("New passwords do not match.");
        }

        // 5. Verify new password isn't the same as the old one
        if (passwordEncoder.matches(passwordDto.getNewPassword(), patient.getPassword())) {
            throw new BadRequestException("New password cannot be the same as the current password.");
        }

        // 6. Encode and set new password
        patient.setPassword(passwordEncoder.encode(passwordDto.getNewPassword()));

        // 7. Save patient
        patientRepository.save(patient);

        logger.info("Password changed successfully for patient ID: {}", patientId);
    }

    // --- Helper Method: Map Entity to Profile DTO ---
    private PatientProfileDTO mapToPatientProfileDTO(Patient patient) {
        return new PatientProfileDTO(
                patient.getId(),
                patient.getName(),
                patient.getAge(),
                patient.getDateOfBirth(),
                patient.getGender(),
                patient.getPersonalNumber(),
                patient.getAddress(),
                patient.getEmail(),
                patient.getGuardianName(),
                patient.getGuardianRelation(),
                patient.getGuardianPhoneNumber(),
                patient.getCreatedAt()
        );
    }

}