package com.healthcareapp.hms.service;

import com.healthcareapp.hms.entity.Admin;
import com.healthcareapp.hms.entity.Doctor;
import com.healthcareapp.hms.entity.Patient;
import com.healthcareapp.hms.repository.AdminRepository;
import com.healthcareapp.hms.repository.DoctorRepository;
import com.healthcareapp.hms.repository.PatientRepository;
import com.healthcareapp.hms.security.AppUserDetails; // Needed for casting/checking instance type
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;


import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock private PatientRepository patientRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private AdminRepository adminRepository;

    @InjectMocks private CustomUserDetailsService customUserDetailsService;

    private Patient patient;
    private Doctor doctor;
    private Admin admin;

    @BeforeEach
    void setUp() {
        patient = new Patient();
        patient.setId(1L);
        patient.setEmail("patient@test.com");
        patient.setPassword("p_hash");
        patient.setName("Test Patient");

        doctor = new Doctor();
        doctor.setId(10L);
        doctor.setEmail("doctor@test.com");
        doctor.setPassword("d_hash");
        doctor.setName("Test Doctor");

        admin = new Admin();
        admin.setId(99L);
        admin.setEmail("admin@test.com");
        admin.setPassword("a_hash");
        admin.setName("Test Admin");
    }

    @Test
    @DisplayName("Load User By Username - Success (Patient Found)")
    void testLoadUserByUsername_PatientFound() {
        // Arrange
        String patientEmail = patient.getEmail();
        // Mock patient repo returns patient
        given(patientRepository.findByEmail(patientEmail)).willReturn(Optional.of(patient));
        // Mock others return empty (although not strictly necessary if patient is found first)
        // given(doctorRepository.findByEmail(patientEmail)).willReturn(Optional.empty());
        // given(adminRepository.findByEmail(patientEmail)).willReturn(Optional.empty());

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(patientEmail);

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails).isInstanceOf(AppUserDetails.class);
        assertThat(userDetails.getUsername()).isEqualTo(patientEmail);
        assertThat(userDetails.getPassword()).isEqualTo(patient.getPassword());
        // Check for ROLE_PATIENT authority
        assertThat(userDetails.getAuthorities()).hasSize(1)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_PATIENT");
        // Check custom fields via cast
        AppUserDetails appUserDetails = (AppUserDetails) userDetails;
        assertThat(appUserDetails.getUserId()).isEqualTo(patient.getId());
        assertThat(appUserDetails.getUserType()).isEqualTo("PATIENT");
        assertThat(appUserDetails.getName()).isEqualTo(patient.getName());

        // Verify correct repo was called
        verify(patientRepository).findByEmail(patientEmail);
        verifyNoInteractions(doctorRepository); // Should not be called if patient found
        verifyNoInteractions(adminRepository);  // Should not be called if patient found
    }

    @Test
    @DisplayName("Load User By Username - Success (Doctor Found)")
    void testLoadUserByUsername_DoctorFound() {
        // Arrange
        String doctorEmail = doctor.getEmail();
        given(patientRepository.findByEmail(doctorEmail)).willReturn(Optional.empty()); // Patient not found
        given(doctorRepository.findByEmail(doctorEmail)).willReturn(Optional.of(doctor)); // Doctor found
        // given(adminRepository.findByEmail(doctorEmail)).willReturn(Optional.empty()); // Not necessary

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(doctorEmail);

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(doctorEmail);
        assertThat(userDetails.getPassword()).isEqualTo(doctor.getPassword());
        assertThat(userDetails.getAuthorities()).hasSize(1)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_DOCTOR"); // Check Doctor role
        AppUserDetails appUserDetails = (AppUserDetails) userDetails;
        assertThat(appUserDetails.getUserId()).isEqualTo(doctor.getId());
        assertThat(appUserDetails.getUserType()).isEqualTo("DOCTOR");
        assertThat(appUserDetails.getName()).isEqualTo(doctor.getName());


        // Verify repos called
        verify(patientRepository).findByEmail(doctorEmail);
        verify(doctorRepository).findByEmail(doctorEmail);
        verifyNoInteractions(adminRepository); // Should not be called
    }

    @Test
    @DisplayName("Load User By Username - Success (Admin Found)")
    void testLoadUserByUsername_AdminFound() {
        // Arrange
        String adminEmail = admin.getEmail();
        given(patientRepository.findByEmail(adminEmail)).willReturn(Optional.empty()); // Patient not found
        given(doctorRepository.findByEmail(adminEmail)).willReturn(Optional.empty()); // Doctor not found
        given(adminRepository.findByEmail(adminEmail)).willReturn(Optional.of(admin)); // Admin found

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(adminEmail);

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(adminEmail);
        assertThat(userDetails.getPassword()).isEqualTo(admin.getPassword());
        assertThat(userDetails.getAuthorities()).hasSize(1)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN"); // Check Admin role
        AppUserDetails appUserDetails = (AppUserDetails) userDetails;
        assertThat(appUserDetails.getUserId()).isEqualTo(admin.getId());
        assertThat(appUserDetails.getUserType()).isEqualTo("ADMIN");
        assertThat(appUserDetails.getName()).isEqualTo(admin.getName());

        // Verify repos called
        verify(patientRepository).findByEmail(adminEmail);
        verify(doctorRepository).findByEmail(adminEmail);
        verify(adminRepository).findByEmail(adminEmail);
    }

    @Test
    @DisplayName("Load User By Username - Failure (User Not Found)")
    void testLoadUserByUsername_NotFound() {
        // Arrange
        String unknownEmail = "unknown@example.com";
        given(patientRepository.findByEmail(unknownEmail)).willReturn(Optional.empty()); // Patient not found
        given(doctorRepository.findByEmail(unknownEmail)).willReturn(Optional.empty()); // Doctor not found
        given(adminRepository.findByEmail(unknownEmail)).willReturn(Optional.empty()); // Admin not found

        // Act & Assert
        UsernameNotFoundException thrown = assertThrows(UsernameNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername(unknownEmail);
        });

        assertEquals("User not found with email: " + unknownEmail, thrown.getMessage());

        // Verify all repos were checked
        verify(patientRepository).findByEmail(unknownEmail);
        verify(doctorRepository).findByEmail(unknownEmail);
        verify(adminRepository).findByEmail(unknownEmail);
    }
}