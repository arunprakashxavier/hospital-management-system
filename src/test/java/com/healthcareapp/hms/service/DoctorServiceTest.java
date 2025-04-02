package com.healthcareapp.hms.service;

// Import DTOs
import com.healthcareapp.hms.dto.DoctorProfileDTO;
import com.healthcareapp.hms.dto.DoctorRegistrationDTO;
import com.healthcareapp.hms.dto.DoctorUpdateDTO;
import com.healthcareapp.hms.dto.PasswordChangeDTO;

// Import Entities and Exceptions
import com.healthcareapp.hms.entity.Admin;
import com.healthcareapp.hms.entity.Doctor;
import com.healthcareapp.hms.exception.BadRequestException;
import com.healthcareapp.hms.exception.DuplicateResourceException;
import com.healthcareapp.hms.exception.ResourceNotFoundException;
import com.healthcareapp.hms.repository.DoctorRepository;
import com.healthcareapp.hms.security.AppUserDetails; // To simulate logged-in users

// JUnit 5 and Mockito Imports
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

// Static imports
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DoctorService doctorService;

    @Captor
    ArgumentCaptor<Doctor> doctorArgumentCaptor;

    // Test Data Objects
    private DoctorRegistrationDTO registrationDTO;
    private DoctorUpdateDTO updateDTO;
    private PasswordChangeDTO passwordChangeDTO;
    private Doctor doctor;
    private Doctor doctor2;
    private Admin admin;
    private AppUserDetails doctorUserDetails;
    private AppUserDetails doctor2UserDetails;
    private AppUserDetails adminUserDetails;

    @BeforeEach
    void setUp() {
        // Registration DTO
        registrationDTO = new DoctorRegistrationDTO();
        registrationDTO.setName("Dr. Test");
        registrationDTO.setEmail("dr.test@example.com");
        registrationDTO.setPassword("password123");
        registrationDTO.setPhoneNumber("1112223333");
        registrationDTO.setAge(45);
        registrationDTO.setQualification("MD");
        registrationDTO.setSpecialization("TESTOLOGY");
        registrationDTO.setYearsOfExperience(10);

        // Doctor Entity
        doctor = new Doctor();
        doctor.setId(1L);
        doctor.setName(registrationDTO.getName());
        doctor.setEmail(registrationDTO.getEmail());
        doctor.setPassword("hashedPassword123"); // Example current hashed password
        doctor.setPhoneNumber(registrationDTO.getPhoneNumber());
        doctor.setAge(registrationDTO.getAge());
        doctor.setQualification(registrationDTO.getQualification());
        doctor.setSpecialization(registrationDTO.getSpecialization());
        doctor.setYearsOfExperience(registrationDTO.getYearsOfExperience());
        doctor.setCreatedAt(LocalDateTime.now().minusDays(5));

        // Second Doctor Entity
        doctor2 = new Doctor();
        doctor2.setId(2L);
        doctor2.setName("Dr. Other");
        doctor2.setEmail("dr.other@example.com");
        doctor2.setPassword("hashedPassword456");
        doctor2.setPhoneNumber("4445556666");
        // ... set other fields ...

        // Admin Entity
        admin = new Admin("Test Admin", "admin@test.com", "hashedAdminPass");
        admin.setId(99L);

        // Update DTO
        updateDTO = new DoctorUpdateDTO();
        updateDTO.setPhoneNumber("1112224444"); // New number
        updateDTO.setYearsOfExperience(11);
        updateDTO.setQualification("MD, PhD");

        // Password Change DTO
        passwordChangeDTO = new PasswordChangeDTO();
        passwordChangeDTO.setCurrentPassword("password123"); // Plain text current password
        passwordChangeDTO.setNewPassword("newPassword456");
        passwordChangeDTO.setConfirmNewPassword("newPassword456");

        // AppUserDetails
        doctorUserDetails = new AppUserDetails(doctor);
        doctor2UserDetails = new AppUserDetails(doctor2);
        adminUserDetails = new AppUserDetails(admin);
    }

    // --- Tests for registerDoctor ---

    @Test
    @DisplayName("Register Doctor Success")
    void testRegisterDoctor_Success() {
        // Arrange
        given(doctorRepository.existsByEmail(registrationDTO.getEmail())).willReturn(false);
        given(doctorRepository.existsByPhoneNumber(registrationDTO.getPhoneNumber())).willReturn(false);
        given(passwordEncoder.encode(registrationDTO.getPassword())).willReturn("hashedPasswordXYZ");
        given(doctorRepository.save(any(Doctor.class))).willAnswer(invocation -> {
            Doctor docToSave = invocation.getArgument(0);
            docToSave.setId(5L); // Simulate saved ID
            docToSave.setPassword("hashedPasswordXYZ"); // Ensure hash matches
            docToSave.setCreatedAt(LocalDateTime.now());
            docToSave.setUpdatedAt(LocalDateTime.now());
            return docToSave;
        });

        // Act
        Doctor savedDoctor = doctorService.registerDoctor(registrationDTO);

        // Assert
        assertThat(savedDoctor).isNotNull();
        assertThat(savedDoctor.getId()).isEqualTo(5L);
        assertThat(savedDoctor.getEmail()).isEqualTo(registrationDTO.getEmail());
        assertThat(savedDoctor.getPassword()).isEqualTo("hashedPasswordXYZ");

        // Verify
        verify(doctorRepository).existsByEmail(registrationDTO.getEmail());
        verify(doctorRepository).existsByPhoneNumber(registrationDTO.getPhoneNumber());
        verify(passwordEncoder).encode(registrationDTO.getPassword());
        verify(doctorRepository).save(any(Doctor.class));
    }

    @Test
    @DisplayName("Register Doctor Failure - Duplicate Email")
    void testRegisterDoctor_DuplicateEmail() {
        // Arrange
        given(doctorRepository.existsByEmail(registrationDTO.getEmail())).willReturn(true);

        // Act & Assert
        DuplicateResourceException thrown = assertThrows(DuplicateResourceException.class,
                () -> doctorService.registerDoctor(registrationDTO));
        assertTrue(thrown.getMessage().contains("Doctor already exists with email"));

        // Verify
        verify(doctorRepository).existsByEmail(registrationDTO.getEmail());
        verify(doctorRepository, never()).existsByPhoneNumber(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(doctorRepository, never()).save(any(Doctor.class));
    }

    @Test
    @DisplayName("Register Doctor Failure - Duplicate Phone")
    void testRegisterDoctor_DuplicatePhone() {
        // Arrange
        given(doctorRepository.existsByEmail(registrationDTO.getEmail())).willReturn(false);
        given(doctorRepository.existsByPhoneNumber(registrationDTO.getPhoneNumber())).willReturn(true);

        // Act & Assert
        DuplicateResourceException thrown = assertThrows(DuplicateResourceException.class,
                () -> doctorService.registerDoctor(registrationDTO));
        assertTrue(thrown.getMessage().contains("Doctor already exists with phone number"));

        // Verify
        verify(doctorRepository).existsByEmail(registrationDTO.getEmail());
        verify(doctorRepository).existsByPhoneNumber(registrationDTO.getPhoneNumber());
        verify(passwordEncoder, never()).encode(anyString());
        verify(doctorRepository, never()).save(any(Doctor.class));
    }

    // --- Tests for getDoctorProfile ---

    @Test
    @DisplayName("Get Doctor Profile Success - Self")
    void testGetDoctorProfile_Success_Self() {
        // Arrange
        Long doctorId = doctor.getId();
        given(doctorRepository.findById(doctorId)).willReturn(Optional.of(doctor));

        // Act
        DoctorProfileDTO profile = doctorService.getDoctorProfile(doctorId, doctorUserDetails);

        // Assert
        assertThat(profile).isNotNull();
        assertThat(profile.getId()).isEqualTo(doctorId);
        assertThat(profile.getName()).isEqualTo(doctor.getName());
        assertThat(profile.getEmail()).isEqualTo(doctor.getEmail());
        assertThat(profile.getSpecialization()).isEqualTo(doctor.getSpecialization());

        // Verify
        verify(doctorRepository).findById(doctorId);
    }

    @Test
    @DisplayName("Get Doctor Profile Success - Admin")
    void testGetDoctorProfile_Success_Admin() {
        // Arrange
        Long doctorId = doctor.getId();
        given(doctorRepository.findById(doctorId)).willReturn(Optional.of(doctor));

        // Act
        DoctorProfileDTO profile = doctorService.getDoctorProfile(doctorId, adminUserDetails);

        // Assert
        assertThat(profile).isNotNull();
        assertThat(profile.getId()).isEqualTo(doctorId);
        assertThat(profile.getEmail()).isEqualTo(doctor.getEmail());

        // Verify
        verify(doctorRepository).findById(doctorId);
    }

    @Test
    @DisplayName("Get Doctor Profile Failure - Access Denied")
    void testGetDoctorProfile_AccessDenied() {
        // Arrange
        Long targetDoctorId = doctor.getId(); // Doctor 1's ID
        AppUserDetails currentOtherDoctor = doctor2UserDetails; // Logged in as Doctor 2

        // Act & Assert
        AccessDeniedException thrown = assertThrows(AccessDeniedException.class, () -> {
            doctorService.getDoctorProfile(targetDoctorId, currentOtherDoctor);
        });
        assertEquals("You do not have permission to view this doctor's profile.", thrown.getMessage());

        // Verify
        verify(doctorRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Get Doctor Profile Failure - Not Found")
    void testGetDoctorProfile_NotFound() {
        // Arrange
        Long nonExistentDoctorId = 999L;
        given(doctorRepository.findById(nonExistentDoctorId)).willReturn(Optional.empty());

        // Act & Assert
        // Admin tries to access non-existent doctor
        ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class, () -> {
            doctorService.getDoctorProfile(nonExistentDoctorId, adminUserDetails);
        });
        assertTrue(thrown.getMessage().contains("Doctor not found with id : '999'"));

        // Verify
        verify(doctorRepository).findById(nonExistentDoctorId);
    }


    // --- Tests for updateDoctorProfile ---

    @Test
    @DisplayName("Update Doctor Profile Success")
    void testUpdateDoctorProfile_Success() {
        // Arrange
        Long doctorId = doctor.getId();
        given(doctorRepository.findById(doctorId)).willReturn(Optional.of(doctor));
        given(doctorRepository.existsByPhoneNumberAndIdNot(updateDTO.getPhoneNumber(), doctorId)).willReturn(false);
        given(doctorRepository.save(any(Doctor.class))).willAnswer(invocation -> invocation.getArgument(0));

        // Act
        DoctorProfileDTO updatedProfile = doctorService.updateDoctorProfile(doctorId, updateDTO, doctorUserDetails);

        // Assert
        assertThat(updatedProfile).isNotNull();
        assertThat(updatedProfile.getPhoneNumber()).isEqualTo(updateDTO.getPhoneNumber());
        assertThat(updatedProfile.getQualification()).isEqualTo(updateDTO.getQualification());
        assertThat(updatedProfile.getYearsOfExperience()).isEqualTo(updateDTO.getYearsOfExperience());
        assertThat(updatedProfile.getName()).isEqualTo(doctor.getName()); // Check non-updated field

        // Verify save with updated data
        verify(doctorRepository).save(doctorArgumentCaptor.capture());
        assertThat(doctorArgumentCaptor.getValue().getPhoneNumber()).isEqualTo(updateDTO.getPhoneNumber());

        // Verify other mocks
        verify(doctorRepository).findById(doctorId);
        verify(doctorRepository).existsByPhoneNumberAndIdNot(updateDTO.getPhoneNumber(), doctorId);
    }

    @Test
    @DisplayName("Update Doctor Profile Failure - Access Denied")
    void testUpdateDoctorProfile_AccessDenied() {
        // Arrange
        Long targetDoctorId = doctor.getId();
        AppUserDetails currentOtherDoctor = doctor2UserDetails; // Logged in as another doctor

        // Act & Assert
        AccessDeniedException thrown = assertThrows(AccessDeniedException.class, () -> {
            doctorService.updateDoctorProfile(targetDoctorId, updateDTO, currentOtherDoctor);
        });
        assertEquals("You can only update your own profile.", thrown.getMessage());

        // Verify
        verify(doctorRepository, never()).findById(anyLong());
        verify(doctorRepository, never()).save(any(Doctor.class));
    }

    @Test
    @DisplayName("Update Doctor Profile Failure - Not Found")
    void testUpdateDoctorProfile_NotFound() {
        // Arrange
        Long nonExistentDoctorId = 999L;
        // Need dummy AppUserDetails matching ID to pass permission check
        Doctor dummyDoc = new Doctor(); dummyDoc.setId(nonExistentDoctorId); dummyDoc.setEmail("dummy@doc.com"); dummyDoc.setPassword("d");
        AppUserDetails currentUser = new AppUserDetails(dummyDoc);

        given(doctorRepository.findById(nonExistentDoctorId)).willReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class, () -> {
            doctorService.updateDoctorProfile(nonExistentDoctorId, updateDTO, currentUser);
        });
        assertTrue(thrown.getMessage().contains("Doctor not found with id : '999'"));

        // Verify
        verify(doctorRepository).findById(nonExistentDoctorId);
        verify(doctorRepository, never()).save(any(Doctor.class));
    }

    @Test
    @DisplayName("Update Doctor Profile Failure - Duplicate Phone")
    void testUpdateDoctorProfile_DuplicatePhone() {
        // Arrange
        Long doctorId = doctor.getId();
        given(doctorRepository.findById(doctorId)).willReturn(Optional.of(doctor));
        // Assume new phone number *does* exist for another doctor
        given(doctorRepository.existsByPhoneNumberAndIdNot(updateDTO.getPhoneNumber(), doctorId)).willReturn(true);

        // Act & Assert
        DuplicateResourceException thrown = assertThrows(DuplicateResourceException.class, () -> {
            doctorService.updateDoctorProfile(doctorId, updateDTO, doctorUserDetails);
        });
        assertTrue(thrown.getMessage().contains("Doctor already exists with phone number"));

        // Verify
        verify(doctorRepository).findById(doctorId);
        verify(doctorRepository).existsByPhoneNumberAndIdNot(updateDTO.getPhoneNumber(), doctorId);
        verify(doctorRepository, never()).save(any(Doctor.class));
    }




    @Test
    @DisplayName("Change Doctor Password Failure - Access Denied")
    void testChangeDoctorPassword_AccessDenied() {
        // Arrange
        Long targetDoctorId = doctor.getId();
        AppUserDetails currentOtherDoctor = doctor2UserDetails;

        // Act & Assert
        AccessDeniedException thrown = assertThrows(AccessDeniedException.class, () -> {
            doctorService.changePassword(targetDoctorId, passwordChangeDTO, currentOtherDoctor);
        });
        assertEquals("You can only change your own password.", thrown.getMessage());

        // Verify
        verifyNoInteractions(passwordEncoder); // No password operations should happen
        verify(doctorRepository, never()).findById(anyLong());
        verify(doctorRepository, never()).save(any(Doctor.class));
    }

    @Test
    @DisplayName("Change Doctor Password Failure - Not Found")
    void testChangeDoctorPassword_NotFound() {
        // Arrange
        Long nonExistentDoctorId = 999L;
        Doctor dummyDoc = new Doctor(); dummyDoc.setId(nonExistentDoctorId); dummyDoc.setEmail("d@d.com"); dummyDoc.setPassword("p");
        AppUserDetails currentUser = new AppUserDetails(dummyDoc); // User trying to change pass for self, but self not found

        given(doctorRepository.findById(nonExistentDoctorId)).willReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class, () -> {
            doctorService.changePassword(nonExistentDoctorId, passwordChangeDTO, currentUser);
        });
        assertTrue(thrown.getMessage().contains("Doctor not found with id : '999'"));

        // Verify
        verify(doctorRepository).findById(nonExistentDoctorId);
        verifyNoInteractions(passwordEncoder);
        verify(doctorRepository, never()).save(any(Doctor.class));
    }

    @Test
    @DisplayName("Change Doctor Password Failure - Incorrect Current Password")
    void testChangeDoctorPassword_IncorrectCurrent() {
        // Arrange
        Long doctorId = doctor.getId();
        given(doctorRepository.findById(doctorId)).willReturn(Optional.of(doctor));
        // Assume current password does NOT match
        given(passwordEncoder.matches(passwordChangeDTO.getCurrentPassword(), doctor.getPassword())).willReturn(false);

        // Act & Assert
        BadRequestException thrown = assertThrows(BadRequestException.class, () -> {
            doctorService.changePassword(doctorId, passwordChangeDTO, doctorUserDetails);
        });
        assertEquals("Incorrect current password.", thrown.getMessage());

        // Verify
        verify(doctorRepository).findById(doctorId);
        verify(passwordEncoder).matches(passwordChangeDTO.getCurrentPassword(), doctor.getPassword());
        verify(passwordEncoder, never()).encode(anyString());
        verify(doctorRepository, never()).save(any(Doctor.class));
    }

    @Test
    @DisplayName("Change Doctor Password Failure - New Passwords Mismatch")
    void testChangeDoctorPassword_NewMismatch() {
        // Arrange
        Long doctorId = doctor.getId();
        passwordChangeDTO.setConfirmNewPassword("differentNewPassword"); // Set mismatch

        given(doctorRepository.findById(doctorId)).willReturn(Optional.of(doctor));
        // Assume current password *does* match
        given(passwordEncoder.matches(passwordChangeDTO.getCurrentPassword(), doctor.getPassword())).willReturn(true);

        // Act & Assert
        BadRequestException thrown = assertThrows(BadRequestException.class, () -> {
            doctorService.changePassword(doctorId, passwordChangeDTO, doctorUserDetails);
        });
        assertEquals("New passwords do not match.", thrown.getMessage());

        // Verify
        verify(doctorRepository).findById(doctorId);
        verify(passwordEncoder).matches(passwordChangeDTO.getCurrentPassword(), doctor.getPassword());
        verify(passwordEncoder, never()).encode(anyString());
        verify(doctorRepository, never()).save(any(Doctor.class));
    }

}