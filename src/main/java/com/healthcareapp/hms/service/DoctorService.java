package com.healthcareapp.hms.service;

// Import necessary DTOs and classes
import com.healthcareapp.hms.dto.DoctorRegistrationDTO;
import com.healthcareapp.hms.dto.DoctorProfileDTO;
import com.healthcareapp.hms.dto.DoctorUpdateDTO;
import com.healthcareapp.hms.dto.PasswordChangeDTO;
import com.healthcareapp.hms.entity.Doctor;
import com.healthcareapp.hms.exception.BadRequestException;
import com.healthcareapp.hms.exception.DuplicateResourceException;
import com.healthcareapp.hms.exception.ResourceNotFoundException;
import com.healthcareapp.hms.repository.DoctorRepository;
import com.healthcareapp.hms.security.AppUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class DoctorService {

    private static final Logger logger = LoggerFactory.getLogger(DoctorService.class);


    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional // Existing method
    public Doctor registerDoctor(DoctorRegistrationDTO registrationDTO) {
        if (doctorRepository.existsByEmail(registrationDTO.getEmail())) {
            throw new DuplicateResourceException("Doctor", "email", registrationDTO.getEmail());
        }
        if (doctorRepository.existsByPhoneNumber(registrationDTO.getPhoneNumber())) {
            throw new DuplicateResourceException("Doctor", "phone number", registrationDTO.getPhoneNumber());
        }

        Doctor doctor = new Doctor();
        // Map fields...
        doctor.setName(registrationDTO.getName());
        doctor.setAge(registrationDTO.getAge());
        doctor.setQualification(registrationDTO.getQualification());
        doctor.setSpecialization(registrationDTO.getSpecialization());
        doctor.setPhoneNumber(registrationDTO.getPhoneNumber());
        doctor.setYearsOfExperience(registrationDTO.getYearsOfExperience());
        doctor.setEmail(registrationDTO.getEmail());
        doctor.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));

        return doctorRepository.save(doctor);
    }

    // Helper to find doctor or throw exception
    private Doctor findDoctorByIdOrThrow(Long doctorId) {
        return doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", doctorId));
    }


    // --- NEW Profile Management Methods ---

    @Transactional(readOnly = true)
    public DoctorProfileDTO getDoctorProfile(Long doctorId, AppUserDetails currentUser) {
        // 1. Authorization Check: Ensure the current user is the doctor themselves or an Admin
        if (!Objects.equals(currentUser.getUserId(), doctorId) && !currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new AccessDeniedException("You do not have permission to view this doctor's profile.");
        }

        // 2. Fetch doctor
        Doctor doctor = findDoctorByIdOrThrow(doctorId);

        // 3. Map to DTO
        return mapToDoctorProfileDTO(doctor);
    }

    @Transactional
    public DoctorProfileDTO updateDoctorProfile(Long doctorId, DoctorUpdateDTO updateDto, AppUserDetails currentUser) {
        // 1. Authorization Check: Only the doctor themselves can update their profile via this method
        if (!Objects.equals(currentUser.getUserId(), doctorId)) {
            throw new AccessDeniedException("You can only update your own profile.");
        }

        // 2. Fetch doctor
        Doctor doctor = findDoctorByIdOrThrow(doctorId);

        // 3. Check for uniqueness if phone number is changing
        if (!Objects.equals(doctor.getPhoneNumber(), updateDto.getPhoneNumber()) &&
                doctorRepository.existsByPhoneNumberAndIdNot(updateDto.getPhoneNumber(), doctorId)) {
            throw new DuplicateResourceException("Doctor", "phone number", updateDto.getPhoneNumber());
        }

        // 4. Update allowed fields
        doctor.setPhoneNumber(updateDto.getPhoneNumber());
        doctor.setYearsOfExperience(updateDto.getYearsOfExperience());
        doctor.setQualification(updateDto.getQualification());
        // Add other updatable fields (e.g., age) if needed
        // doctor.setAge(updateDto.getAge());

        // 5. Save updated doctor
        Doctor updatedDoctor = doctorRepository.save(doctor);

        // 6. Map to DTO and return
        return mapToDoctorProfileDTO(updatedDoctor);
    }

    @Transactional
    public void changePassword(Long doctorId, PasswordChangeDTO passwordDto, AppUserDetails currentUser) {
        // 1. Authorization Check: Only the doctor themselves can change their password
        if (!Objects.equals(currentUser.getUserId(), doctorId)) {
            throw new AccessDeniedException("You can only change your own password.");
        }
        // Ensure requesting user is actually a doctor (redundant if already checked by route security, but safe)
        if (!currentUser.getUserType().equals("DOCTOR")) {
            throw new AccessDeniedException("User is not a doctor.");
        }


        // 2. Fetch doctor
        Doctor doctor = findDoctorByIdOrThrow(doctorId);

        // 3. Verify current password
        if (!passwordEncoder.matches(passwordDto.getCurrentPassword(), doctor.getPassword())) {
            throw new BadRequestException("Incorrect current password.");
        }

        // 4. Verify new passwords match
        if (!passwordDto.getNewPassword().equals(passwordDto.getConfirmNewPassword())) {
            throw new BadRequestException("New passwords do not match.");
        }

        // 5. Verify new password isn't the same as the old one
        if (passwordEncoder.matches(passwordDto.getNewPassword(), doctor.getPassword())) {
            throw new BadRequestException("New password cannot be the same as the current password.");
        }

        // 6. Encode and set new password
        doctor.setPassword(passwordEncoder.encode(passwordDto.getNewPassword()));

        // 7. Save doctor
        doctorRepository.save(doctor);

        logger.info("Password changed successfully for doctor ID: {}", doctorId);
    }


    // --- NEW Helper Method: Map Entity to Profile DTO ---
    private DoctorProfileDTO mapToDoctorProfileDTO(Doctor doctor) {
        return new DoctorProfileDTO(
                doctor.getId(),
                doctor.getName(),
                doctor.getAge(),
                doctor.getQualification(),
                doctor.getSpecialization(),
                doctor.getPhoneNumber(),
                doctor.getYearsOfExperience(),
                doctor.getEmail(),
                doctor.getCreatedAt()
        );
    }

}