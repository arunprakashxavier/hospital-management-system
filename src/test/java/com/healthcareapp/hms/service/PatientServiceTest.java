package com.healthcareapp.hms.service;

// Import necessary DTOs
import com.healthcareapp.hms.dto.PasswordChangeDTO;
import com.healthcareapp.hms.dto.PatientProfileDTO;
import com.healthcareapp.hms.dto.PatientRegistrationDTO;
import com.healthcareapp.hms.dto.PatientUpdateDTO;

// Import Entities and Exceptions
import java.util.List;
import com.healthcareapp.hms.entity.Admin;
import com.healthcareapp.hms.entity.Patient;
import com.healthcareapp.hms.exception.BadRequestException;
import com.healthcareapp.hms.exception.DuplicateResourceException;
import com.healthcareapp.hms.exception.ResourceNotFoundException;
import com.healthcareapp.hms.repository.PatientRepository;
import com.healthcareapp.hms.security.AppUserDetails; // Import AppUserDetails

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


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority; // Import for roles

// Static imports
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PatientService patientService;

    // Argument Captor for verifying saved entities
    @Captor
    ArgumentCaptor<Patient> patientArgumentCaptor;

    // Test Data Objects
    private PatientRegistrationDTO registrationDTO;
    private PatientUpdateDTO updateDTO;
    private PasswordChangeDTO passwordChangeDTO;
    private Patient patient;
    private Patient patient2;
    private Admin admin;
    private AppUserDetails patientUserDetails;
    private AppUserDetails patient2UserDetails;
    private AppUserDetails adminUserDetails;


    @BeforeEach
    void setUp() {
        // Registration DTO
        registrationDTO = new PatientRegistrationDTO();
        registrationDTO.setName("Test Patient");
        registrationDTO.setEmail("test.patient@example.com");
        registrationDTO.setPassword("password123");
        registrationDTO.setConfirmPassword("password123");
        registrationDTO.setPersonalNumber("9876543210");
        registrationDTO.setAge(35);
        registrationDTO.setDateOfBirth(LocalDate.of(1989, 5, 15));
        registrationDTO.setGender("FEMALE");
        registrationDTO.setAddress("456 Test Ave");
        registrationDTO.setGuardianName("Test Guardian");
        registrationDTO.setGuardianRelation("Spouse");
        registrationDTO.setGuardianPhoneNumber("1234567890");

        // Patient Entity
        patient = new Patient();
        patient.setId(1L);
        patient.setName(registrationDTO.getName());
        patient.setEmail(registrationDTO.getEmail());
        patient.setPassword("hashedPassword123"); // Assume this is the current hashed password
        patient.setPersonalNumber(registrationDTO.getPersonalNumber());
        patient.setAge(registrationDTO.getAge());
        patient.setDateOfBirth(registrationDTO.getDateOfBirth());
        patient.setGender(registrationDTO.getGender());
        patient.setAddress(registrationDTO.getAddress());
        patient.setGuardianName(registrationDTO.getGuardianName());
        patient.setGuardianRelation(registrationDTO.getGuardianRelation());
        patient.setGuardianPhoneNumber(registrationDTO.getGuardianPhoneNumber());
        patient.setCreatedAt(LocalDateTime.now().minusDays(1)); // Example creation date

        // Second Patient Entity
        patient2 = new Patient();
        patient2.setId(2L);
        patient2.setName("Other Patient");
        patient2.setEmail("other.patient@example.com");
        patient2.setPassword("hashedPassword456");
        patient2.setPersonalNumber("1122334455");
        // ... set other fields if needed ...


        // Admin Entity (for creating admin UserDetails)
        admin = new Admin("Test Admin", "admin@test.com", "hashedAdminPass");
        admin.setId(99L);


        // Update DTO
        updateDTO = new PatientUpdateDTO();
        updateDTO.setAddress("789 New Address St");
        updateDTO.setPersonalNumber("5555555555"); // New number
        updateDTO.setGuardianName("New Guardian");
        updateDTO.setGuardianRelation("Friend");
        updateDTO.setGuardianPhoneNumber("1112223333");

        // Password Change DTO
        passwordChangeDTO = new PasswordChangeDTO();
        passwordChangeDTO.setCurrentPassword("password123"); // Plain text current password
        passwordChangeDTO.setNewPassword("newPassword456");
        passwordChangeDTO.setConfirmNewPassword("newPassword456");

        // AppUserDetails for different roles/users
        patientUserDetails = new AppUserDetails(patient);
        patient2UserDetails = new AppUserDetails(patient2);
        adminUserDetails = new AppUserDetails(admin); // Assumes AppUserDetails can take Admin
    }

    // --- Tests for registerPatient (Existing) ---

    @Test @DisplayName("Register Patient Success")
    void testRegisterPatient_Success() { /* ... test code from previous response ... */
        given(patientRepository.existsByEmail(registrationDTO.getEmail())).willReturn(false);
        given(patientRepository.existsByPersonalNumber(registrationDTO.getPersonalNumber())).willReturn(false);
        given(passwordEncoder.encode(registrationDTO.getPassword())).willReturn("hashedPassword123");
        given(patientRepository.save(any(Patient.class))).willAnswer(invocation -> {
            Patient patientToSave = invocation.getArgument(0);
            patientToSave.setId(1L); patientToSave.setPassword("hashedPassword123");
            patientToSave.setCreatedAt(LocalDateTime.now()); patientToSave.setUpdatedAt(LocalDateTime.now());
            return patientToSave;
        });
        Patient savedPatient = patientService.registerPatient(registrationDTO);
        assertThat(savedPatient).isNotNull();
        assertThat(savedPatient.getId()).isEqualTo(1L);
        assertThat(savedPatient.getPassword()).isEqualTo("hashedPassword123");
        verify(patientRepository).save(any(Patient.class));
    }
    @Test @DisplayName("Register Patient Failure - Password Mismatch")
    void testRegisterPatient_PasswordMismatch() { /* ... test code from previous response ... */
        registrationDTO.setConfirmPassword("differentPassword");
        BadRequestException thrown = assertThrows(BadRequestException.class, () -> patientService.registerPatient(registrationDTO));
        assertEquals("Passwords do not match", thrown.getMessage());
        verify(patientRepository, never()).save(any(Patient.class));
    }
    @Test @DisplayName("Register Patient Failure - Duplicate Email")
    void testRegisterPatient_DuplicateEmail() { /* ... test code from previous response ... */
        given(patientRepository.existsByEmail(registrationDTO.getEmail())).willReturn(true);
        DuplicateResourceException thrown = assertThrows(DuplicateResourceException.class, () -> patientService.registerPatient(registrationDTO));
        assertTrue(thrown.getMessage().contains("Patient already exists with email"));
        verify(patientRepository, never()).save(any(Patient.class));
    }
    @Test @DisplayName("Register Patient Failure - Duplicate Personal Number")
    void testRegisterPatient_DuplicatePersonalNumber() { /* ... test code from previous response ... */
        given(patientRepository.existsByEmail(registrationDTO.getEmail())).willReturn(false);
        given(patientRepository.existsByPersonalNumber(registrationDTO.getPersonalNumber())).willReturn(true);
        DuplicateResourceException thrown = assertThrows(DuplicateResourceException.class, () -> patientService.registerPatient(registrationDTO));
        assertTrue(thrown.getMessage().contains("Patient already exists with personal number"));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    // --- Tests for getPatientProfile (Existing) ---

    @Test @DisplayName("Get Patient Profile Success - Self")
    void testGetPatientProfile_Success_Self() { /* ... test code from previous response ... */
        Long patientId = patient.getId();
        given(patientRepository.findById(patientId)).willReturn(Optional.of(patient));
        PatientProfileDTO profileDTO = patientService.getPatientProfile(patientId, patientUserDetails);
        assertThat(profileDTO).isNotNull();
        assertThat(profileDTO.getId()).isEqualTo(patientId);
        verify(patientRepository).findById(patientId);
    }
    @Test @DisplayName("Get Patient Profile Success - Admin")
    void testGetPatientProfile_Success_Admin() { /* ... test code from previous response ... */
        Long patientId = patient.getId();
        given(patientRepository.findById(patientId)).willReturn(Optional.of(patient));
        PatientProfileDTO profileDTO = patientService.getPatientProfile(patientId, adminUserDetails);
        assertThat(profileDTO).isNotNull();
        assertThat(profileDTO.getId()).isEqualTo(patientId);
        verify(patientRepository).findById(patientId);
    }
    @Test @DisplayName("Get Patient Profile Failure - Access Denied")
    void testGetPatientProfile_AccessDenied() { /* ... test code from previous response ... */
        Long targetPatientId = patient.getId();
        AppUserDetails currentOtherPatient = patient2UserDetails;
        AccessDeniedException thrown = assertThrows(AccessDeniedException.class, () -> patientService.getPatientProfile(targetPatientId, currentOtherPatient));
        assertEquals("You do not have permission to view this patient's profile.", thrown.getMessage());
        verify(patientRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Update Patient Profile Success")
    void testUpdatePatientProfile_Success() {
        // Arrange
        Long patientId = patient.getId();
        AppUserDetails currentUser = patientUserDetails;
        given(patientRepository.findById(patientId)).willReturn(Optional.of(patient));
        given(patientRepository.existsByPersonalNumberAndIdNot(updateDTO.getPersonalNumber(), patientId)).willReturn(false);
        given(patientRepository.save(any(Patient.class))).willAnswer(invocation -> invocation.getArgument(0));

        // Act
        PatientProfileDTO updatedProfile = patientService.updatePatientProfile(patientId, updateDTO, currentUser);

        // Assert
        assertThat(updatedProfile).isNotNull();
        assertThat(updatedProfile.getAddress()).isEqualTo(updateDTO.getAddress());
        assertThat(updatedProfile.getPersonalNumber()).isEqualTo(updateDTO.getPersonalNumber());
        assertThat(updatedProfile.getGuardianName()).isEqualTo(updateDTO.getGuardianName());
        assertThat(updatedProfile.getName()).isEqualTo(patient.getName()); // Unchanged field

        // Verify save with updated data using ArgumentCaptor
        verify(patientRepository).save(patientArgumentCaptor.capture());
        Patient savedPatient = patientArgumentCaptor.getValue();
        assertThat(savedPatient.getId()).isEqualTo(patientId); // Ensure ID remains same
        assertThat(savedPatient.getAddress()).isEqualTo(updateDTO.getAddress());
        assertThat(savedPatient.getPersonalNumber()).isEqualTo(updateDTO.getPersonalNumber());
        assertThat(savedPatient.getGuardianName()).isEqualTo(updateDTO.getGuardianName());
        assertThat(savedPatient.getGuardianRelation()).isEqualTo(updateDTO.getGuardianRelation());
        assertThat(savedPatient.getGuardianPhoneNumber()).isEqualTo(updateDTO.getGuardianPhoneNumber());

        verify(patientRepository).findById(patientId);
        verify(patientRepository).existsByPersonalNumberAndIdNot(updateDTO.getPersonalNumber(), patientId);
    }

    @Test
    @DisplayName("Update Patient Profile Failure - Access Denied")
    void testUpdatePatientProfile_AccessDenied() {
        // Arrange
        Long targetPatientId = patient.getId();
        AppUserDetails currentOtherPatient = patient2UserDetails;

        // Act & Assert
        AccessDeniedException thrown = assertThrows(AccessDeniedException.class, () -> {
            patientService.updatePatientProfile(targetPatientId, updateDTO, currentOtherPatient);
        });
        assertEquals("You can only update your own profile.", thrown.getMessage());
        verify(patientRepository, never()).findById(anyLong());
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("Update Patient Profile Failure - Patient Not Found")
    void testUpdatePatientProfile_NotFound() {
        // Arrange
        Long nonExistentPatientId = 999L;
        // Need a currentUser whose ID matches the one being requested to pass permission check
        AppUserDetails currentUser = mock(AppUserDetails.class);
        when(currentUser.getUserId()).thenReturn(nonExistentPatientId);

        given(patientRepository.findById(nonExistentPatientId)).willReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class, () -> {
            patientService.updatePatientProfile(nonExistentPatientId, updateDTO, currentUser);
        });
        assertTrue(thrown.getMessage().contains("Patient not found with id : '999'"));

        // Verify
        verify(patientRepository).findById(nonExistentPatientId);
        verify(patientRepository, never()).existsByPersonalNumberAndIdNot(anyString(), anyLong());
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("Update Patient Profile Failure - Duplicate Personal Number")
    void testUpdatePatientProfile_DuplicatePersonalNumber() {
        // Arrange
        Long patientId = patient.getId();
        AppUserDetails currentUser = patientUserDetails;
        given(patientRepository.findById(patientId)).willReturn(Optional.of(patient));
        // Assume new personal number *does* exist for another patient
        given(patientRepository.existsByPersonalNumberAndIdNot(updateDTO.getPersonalNumber(), patientId)).willReturn(true);

        // Act & Assert
        DuplicateResourceException thrown = assertThrows(DuplicateResourceException.class, () -> {
            patientService.updatePatientProfile(patientId, updateDTO, currentUser);
        });
        assertTrue(thrown.getMessage().contains("Patient already exists with personal number"));
        assertTrue(thrown.getMessage().contains(updateDTO.getPersonalNumber()));

        // Verify
        verify(patientRepository).findById(patientId);
        verify(patientRepository).existsByPersonalNumberAndIdNot(updateDTO.getPersonalNumber(), patientId);
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("Change Password Failure - Access Denied")
    void testChangePassword_AccessDenied() {
        // Arrange
        Long targetPatientId = patient.getId(); // Patient 1's ID
        AppUserDetails currentOtherPatient = patient2UserDetails; // Logged in as Patient 2

        // Act & Assert
        AccessDeniedException thrown = assertThrows(AccessDeniedException.class, () -> {
            patientService.changePassword(targetPatientId, passwordChangeDTO, currentOtherPatient);
        });
        assertEquals("You can only change your own password.", thrown.getMessage());

        // Verify no sensitive operations occurred
        verify(patientRepository, never()).findById(anyLong());
        verifyNoInteractions(passwordEncoder); // No password checks or encoding
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("Change Password Failure - Patient Not Found")
    void testChangePassword_NotFound() {
        // Arrange
        Long nonExistentPatientId = 999L;
        AppUserDetails currentUser = mock(AppUserDetails.class); // Use mock user
        when(currentUser.getUserId()).thenReturn(nonExistentPatientId); // Match ID for permission check

        given(patientRepository.findById(nonExistentPatientId)).willReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class, () -> {
            patientService.changePassword(nonExistentPatientId, passwordChangeDTO, currentUser);
        });
        assertTrue(thrown.getMessage().contains("Patient not found with id : '999'"));

        // Verify
        verify(patientRepository).findById(nonExistentPatientId);
        verifyNoInteractions(passwordEncoder);
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("Change Password Failure - Incorrect Current Password")
    void testChangePassword_IncorrectCurrent() {
        // Arrange
        Long patientId = patient.getId();
        AppUserDetails currentUser = patientUserDetails;

        given(patientRepository.findById(patientId)).willReturn(Optional.of(patient));
        // Mock current password check to FAIL
        given(passwordEncoder.matches(passwordChangeDTO.getCurrentPassword(), patient.getPassword())).willReturn(false);

        // Act & Assert
        BadRequestException thrown = assertThrows(BadRequestException.class, () -> {
            patientService.changePassword(patientId, passwordChangeDTO, currentUser);
        });
        assertEquals("Incorrect current password.", thrown.getMessage());

        // Verify checks up to password match, but not beyond
        verify(patientRepository).findById(patientId);
        verify(passwordEncoder).matches(passwordChangeDTO.getCurrentPassword(), patient.getPassword());
        verify(passwordEncoder, never()).encode(anyString());
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("Change Password Failure - New Passwords Mismatch")
    void testChangePassword_NewMismatch() {
        // Arrange
        Long patientId = patient.getId();
        AppUserDetails currentUser = patientUserDetails;
        passwordChangeDTO.setConfirmNewPassword("differentNewPassword"); // Set mismatch

        given(patientRepository.findById(patientId)).willReturn(Optional.of(patient));
        // Assume current password *does* match
        given(passwordEncoder.matches(passwordChangeDTO.getCurrentPassword(), patient.getPassword())).willReturn(true);

        // Act & Assert
        BadRequestException thrown = assertThrows(BadRequestException.class, () -> {
            patientService.changePassword(patientId, passwordChangeDTO, currentUser);
        });
        assertEquals("New passwords do not match.", thrown.getMessage());

        // Verify checks up to current password match, but not beyond
        verify(patientRepository).findById(patientId);
        verify(passwordEncoder).matches(passwordChangeDTO.getCurrentPassword(), patient.getPassword());
        verify(passwordEncoder, never()).encode(anyString());
        verify(patientRepository, never()).save(any(Patient.class));
    }

} // End of class