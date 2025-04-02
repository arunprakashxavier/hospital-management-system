package com.healthcareapp.hms.service;

// Import necessary DTOs
import com.healthcareapp.hms.dto.*; // Wildcard import for brevity

// Import Domain, Entities, Exceptions, Repositories, Security
import com.healthcareapp.hms.domain.AppointmentStatus;
import com.healthcareapp.hms.entity.Admin;
import com.healthcareapp.hms.entity.Appointment;
import com.healthcareapp.hms.entity.Doctor;
import com.healthcareapp.hms.entity.Medication;
import com.healthcareapp.hms.entity.Patient;
import com.healthcareapp.hms.exception.BadRequestException;
import com.healthcareapp.hms.exception.ResourceNotFoundException;
import com.healthcareapp.hms.repository.AppointmentRepository;
import com.healthcareapp.hms.repository.DoctorRepository;
import com.healthcareapp.hms.repository.MedicationRepository;
import com.healthcareapp.hms.repository.PatientRepository;
import com.healthcareapp.hms.security.AppUserDetails;

// JUnit 5 and Mockito
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

// Java Time API and Collections
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


// Static imports
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private MedicationRepository medicationRepository;

    @InjectMocks private AppointmentService appointmentService;

    @Captor ArgumentCaptor<Appointment> appointmentArgumentCaptor;
    @Captor ArgumentCaptor<Medication> medicationArgumentCaptor; // Captor for single medication
    @Captor ArgumentCaptor<List<Medication>> medicationListArgumentCaptor; // If saving list

    // Test Data Objects
    private Patient patient1;
    private Patient patient2;
    private Doctor doctor1;
    private Doctor doctor2;
    private Admin admin;
    private AppUserDetails patientUserDetails;
    private AppUserDetails patient2UserDetails;
    private AppUserDetails doctor1UserDetails;
    private AppUserDetails doctor2UserDetails;
    private AppUserDetails adminUserDetails;
    private Appointment pendingAppointment; // Patient1 with Doctor1
    private Appointment scheduledAppointment; // Patient1 with Doctor1
    private Appointment completedAppointment; // Patient1 with Doctor1
    private Appointment cancelledAppointment; // Patient1 with Doctor1
    private LocalDate testDate;
    private LocalDateTime testDateTimeSlot1; // 10:00
    private LocalDateTime testDateTimeSlot2; // 14:30
    private MedicationInputDTO medInput1;
    private MedicationInputDTO medInput2;
    private Medication medication1;


    @BeforeEach
    void setUp() {
        // Entities
        patient1 = new Patient("Patient One", 30, LocalDate.now().minusYears(30), "M", "111", "Addr1", "p1@test.com", "G1", "R1", "P1", "p1hash");
        patient1.setId(1L);
        patient2 = new Patient("Patient Two", 40, LocalDate.now().minusYears(40), "F", "222", "Addr2", "p2@test.com", "G2", "R2", "P2", "p2hash");
        patient2.setId(2L);


        doctor1 = new Doctor("Doctor One", 40, "MD", "Cardiology", "doc1phone", 10, "d1@test.com", "d1hash");
        doctor1.setId(10L);
        doctor2 = new Doctor("Doctor Two", 50, "PhD", "Neurology", "doc2phone", 20, "d2@test.com", "d2hash");
        doctor2.setId(11L);

        admin = new Admin("Admin User", "admin@test.com", "adminhash");
        admin.setId(99L);

        // AppUserDetails
        patientUserDetails = new AppUserDetails(patient1);
        patient2UserDetails = new AppUserDetails(patient2);
        doctor1UserDetails = new AppUserDetails(doctor1);
        doctor2UserDetails = new AppUserDetails(doctor2);
        adminUserDetails = new AppUserDetails(admin);

        // Test Date/Time
        testDate = LocalDate.now().plusDays(1);
        while(testDate.getDayOfWeek() == DayOfWeek.SUNDAY) { testDate = testDate.plusDays(1); }
        testDateTimeSlot1 = testDate.atTime(10, 0);
        testDateTimeSlot2 = testDate.atTime(14, 30);

        // Sample Appointments (All Patient1 with Doctor1)
        pendingAppointment = new Appointment(patient1, doctor1, testDateTimeSlot1, "Checkup PENDING");
        pendingAppointment.setId(101L); pendingAppointment.setStatus(AppointmentStatus.PENDING);
        pendingAppointment.setCreatedAt(LocalDateTime.now().minusHours(1));

        scheduledAppointment = new Appointment(patient1, doctor1, testDateTimeSlot2, "Follow-up SCHEDULED");
        scheduledAppointment.setId(102L); scheduledAppointment.setStatus(AppointmentStatus.SCHEDULED);
        scheduledAppointment.setCreatedAt(LocalDateTime.now().minusHours(2));

        completedAppointment = new Appointment(patient1, doctor1, testDate.minusDays(1).atTime(11,0), "Past COMPLETED");
        completedAppointment.setId(103L); completedAppointment.setStatus(AppointmentStatus.COMPLETED);
        completedAppointment.setCreatedAt(LocalDateTime.now().minusDays(2));
        completedAppointment.setDoctorNotes("All clear.");

        cancelledAppointment = new Appointment(patient1, doctor1, testDate.plusDays(2).atTime(15, 0), "Future CANCELLED");
        cancelledAppointment.setId(104L); cancelledAppointment.setStatus(AppointmentStatus.CANCELLED);
        cancelledAppointment.setCreatedAt(LocalDateTime.now().minusHours(1));


        // Medication DTOs
        medInput1 = new MedicationInputDTO();
        medInput1.setMedicationName("Lisinopril");
        medInput1.setDosage("10mg");
        medInput1.setFrequency("Once Daily");
        medInput1.setDuration("30 days");
        medInput1.setInstructions("Take in morning");

        medInput2 = new MedicationInputDTO();
        medInput2.setMedicationName("Aspirin");
        medInput2.setDosage("81mg");
        medInput2.setFrequency("Once Daily");
        // Duration and Instructions null for this one

        medication1 = new Medication("Lisinopril", "10mg", "Once Daily", "30 days", "Take in morning", completedAppointment, doctor1, patient1);
        medication1.setId(201L);
        medication1.setPrescribedDate(completedAppointment.getCreatedAt().plusMinutes(30)); // Example prescribed date
    }

    // --- Test for getDoctorsBySpecialization (Existing) ---
    @Test @DisplayName("Get Doctors By Specialization")
    void testGetDoctorsBySpecialization() { /* ... test code ... */
        String specialization = "Cardiology";
        given(doctorRepository.findBySpecializationIgnoreCase(specialization)).willReturn(List.of(doctor1));
        List<Doctor> result = appointmentService.getDoctorsBySpecialization(specialization);
        assertThat(result).isNotNull().hasSize(1);
        assertThat(result.get(0).getSpecialization()).isEqualTo(specialization);
        verify(doctorRepository).findBySpecializationIgnoreCase(specialization);
    }

    // --- Tests for getAvailableSlots (Existing) ---
    @Nested @DisplayName("Tests for getAvailableSlots")
    class GetAvailableSlotsTests { /* ... existing test methods ... */
        @Test @DisplayName("Success - Empty Day") void testGetAvailableSlots_EmptyDay() {/*...*/}
        @Test @DisplayName("Success - With Conflicts") void testGetAvailableSlots_WithConflicts() {/*...*/}
        @Test @DisplayName("Failure - Doctor Not Found") void testGetAvailableSlots_DoctorNotFound() {/*...*/}
        @Test @DisplayName("Empty List - Non-Working Day") void testGetAvailableSlots_NonWorkingDay() {/*...*/}
    }

    // --- Tests for bookAppointment (Existing) ---
    @Nested @DisplayName("Tests for bookAppointment")
    class BookAppointmentTests {
        // Assume bookAppointment tests from previous steps are here...
        @Test @DisplayName("Book Appointment Success") void testBookAppointment_Success() {/*...*/}
        @Test @DisplayName("Failure - Patient Not Found") void testBookAppointment_PatientNotFound() {/*...*/}
        @Test @DisplayName("Failure - Doctor Not Found") void testBookAppointment_DoctorNotFound() {/*...*/}
        @Test @DisplayName("Failure - Time in Past") void testBookAppointment_TimeInPast() {/*...*/}
        @Test @DisplayName("Failure - Outside Working Hours") void testBookAppointment_OutsideWorkingHours() {/*...*/}
        @Test @DisplayName("Failure - Invalid Minute Mark") void testBookAppointment_InvalidMinuteMark() {/*...*/}
        @Test @DisplayName("Failure - Doctor Conflict") void testBookAppointment_DoctorConflict() {/*...*/}
    }


    // --- Tests for approveAppointment (Existing) ---
    @Nested @DisplayName("Tests for approveAppointment")
    class ApproveAppointmentTests { /* ... existing test methods ... */
        @Test @DisplayName("Success - Doctor approves own") void testApproveAppointment_DoctorSuccess() {/*...*/}
        @Test @DisplayName("Success - Admin approves") void testApproveAppointment_AdminSuccess() {/*...*/}
        @Test @DisplayName("Failure - Wrong Status") void testApproveAppointment_WrongStatus() {/*...*/}
        @Test @DisplayName("Failure - Access Denied (Patient)") void testApproveAppointment_AccessDenied_Patient() {/*...*/}
        @Test @DisplayName("Failure - Access Denied (Other Doctor)") void testApproveAppointment_AccessDenied_OtherDoctor() {/*...*/}
        @Test @DisplayName("Failure - Not Found") void testApproveAppointment_NotFound() {/*...*/}
        @Test @DisplayName("Failure - Conflict on Approval") void testApproveAppointment_Conflict() {/*...*/}
    }

    // --- Tests for rejectAppointment (Existing) ---
    @Nested @DisplayName("Tests for rejectAppointment")
    class RejectAppointmentTests { /* ... existing test methods ... */
        @Test @DisplayName("Success - Doctor rejects own") void testRejectAppointment_DoctorSuccess() {/*...*/}
        @Test @DisplayName("Success - Admin rejects") void testRejectAppointment_AdminSuccess() {/*...*/}
        @Test @DisplayName("Failure - Wrong Status") void testRejectAppointment_WrongStatus() {/*...*/}
        @Test @DisplayName("Failure - Access Denied (Patient)") void testRejectAppointment_AccessDenied_Patient() {/*...*/}
        @Test @DisplayName("Failure - Access Denied (Other Doctor)") void testRejectAppointment_AccessDenied_OtherDoctor() {/*...*/}
        @Test @DisplayName("Failure - Not Found") void testRejectAppointment_NotFound() {/*...*/}
    }

    // --- ** NEW: Tests for cancelAppointment ** ---
    @Nested @DisplayName("Tests for cancelAppointment")
    class CancelAppointmentTests {

        @Test @DisplayName("Success - Patient cancels own PENDING appointment")
        void testCancelAppointment_PatientSuccess_Pending() {
            // Arrange
            Long appointmentId = pendingAppointment.getId();
            given(appointmentRepository.findById(appointmentId)).willReturn(Optional.of(pendingAppointment));
            given(appointmentRepository.save(any(Appointment.class))).willAnswer(inv -> inv.getArgument(0));

            // Act
            AppointmentDTO result = appointmentService.cancelAppointment(appointmentId, patientUserDetails);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
            verify(appointmentRepository).save(appointmentArgumentCaptor.capture());
            assertThat(appointmentArgumentCaptor.getValue().getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        }

        @Test @DisplayName("Success - Doctor cancels own SCHEDULED appointment")
        void testCancelAppointment_DoctorSuccess_Scheduled() {
            // Arrange
            Long appointmentId = scheduledAppointment.getId();
            given(appointmentRepository.findById(appointmentId)).willReturn(Optional.of(scheduledAppointment));
            given(appointmentRepository.save(any(Appointment.class))).willAnswer(inv -> inv.getArgument(0));

            // Act
            AppointmentDTO result = appointmentService.cancelAppointment(appointmentId, doctor1UserDetails);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
            verify(appointmentRepository).save(appointmentArgumentCaptor.capture());
            assertThat(appointmentArgumentCaptor.getValue().getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        }

        @Test @DisplayName("Success - Admin cancels appointment")
        void testCancelAppointment_AdminSuccess() {
            // Arrange
            Long appointmentId = scheduledAppointment.getId();
            given(appointmentRepository.findById(appointmentId)).willReturn(Optional.of(scheduledAppointment));
            given(appointmentRepository.save(any(Appointment.class))).willAnswer(inv -> inv.getArgument(0));

            // Act
            AppointmentDTO result = appointmentService.cancelAppointment(appointmentId, adminUserDetails);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
            verify(appointmentRepository).save(appointmentArgumentCaptor.capture());
            assertThat(appointmentArgumentCaptor.getValue().getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        }

        @Test @DisplayName("Failure - Already COMPLETED")
        void testCancelAppointment_AlreadyCompleted() {
            // Arrange
            Long appointmentId = completedAppointment.getId();
            given(appointmentRepository.findById(appointmentId)).willReturn(Optional.of(completedAppointment));

            // Act & Assert
            BadRequestException thrown = assertThrows(BadRequestException.class, () -> {
                appointmentService.cancelAppointment(appointmentId, patientUserDetails); // Patient tries
            });
            assertTrue(thrown.getMessage().contains("Cannot cancel an appointment that is already COMPLETED"));
            verify(appointmentRepository, never()).save(any(Appointment.class));
        }

        @Test @DisplayName("Failure - Already CANCELLED")
        void testCancelAppointment_AlreadyCancelled() {
            // Arrange
            Long appointmentId = cancelledAppointment.getId();
            given(appointmentRepository.findById(appointmentId)).willReturn(Optional.of(cancelledAppointment));

            // Act & Assert
            BadRequestException thrown = assertThrows(BadRequestException.class, () -> {
                appointmentService.cancelAppointment(appointmentId, patientUserDetails); // Patient tries
            });
            assertTrue(thrown.getMessage().contains("Cannot cancel an appointment that is already CANCELLED"));
            verify(appointmentRepository, never()).save(any(Appointment.class));
        }

        @Test @DisplayName("Failure - Access Denied (Unrelated User)")
        void testCancelAppointment_AccessDenied() {
            // Arrange
            Long appointmentId = pendingAppointment.getId(); // Belongs to Patient 1 / Doctor 1
            given(appointmentRepository.findById(appointmentId)).willReturn(Optional.of(pendingAppointment));

            // Act & Assert
            AccessDeniedException thrown = assertThrows(AccessDeniedException.class, () -> {
                appointmentService.cancelAppointment(appointmentId, patient2UserDetails); // Patient 2 tries
            });
            assertEquals("You do not have permission to cancel this appointment.", thrown.getMessage());
            verify(appointmentRepository, never()).save(any(Appointment.class));
        }

        @Test @DisplayName("Failure - Appointment Not Found")
        void testCancelAppointment_NotFound() {
            // Arrange
            Long nonExistentId = 999L;
            given(appointmentRepository.findById(nonExistentId)).willReturn(Optional.empty());

            // Act & Assert
            ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class, () -> {
                appointmentService.cancelAppointment(nonExistentId, patientUserDetails); // Any valid user
            });
            assertTrue(thrown.getMessage().contains("Appointment not found"));
        }
    } // End Nested class for cancelAppointment


    // --- ** NEW: Tests for completeAppointment ** ---
    @Nested @DisplayName("Tests for completeAppointment")
    class CompleteAppointmentTests {

        @Test @DisplayName("Success - Doctor completes own SCHEDULED appointment")
        void testCompleteAppointment_DoctorSuccess() {
            // Arrange
            Long appointmentId = scheduledAppointment.getId();
            String notes = "Patient responded well.";
            given(appointmentRepository.findById(appointmentId)).willReturn(Optional.of(scheduledAppointment));
            given(appointmentRepository.save(any(Appointment.class))).willAnswer(inv -> inv.getArgument(0));

            // Act
            AppointmentDTO result = appointmentService.completeAppointment(appointmentId, notes, doctor1UserDetails);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
            assertThat(result.getDoctorNotes()).isEqualTo(notes);
            verify(appointmentRepository).save(appointmentArgumentCaptor.capture());
            Appointment savedAppt = appointmentArgumentCaptor.getValue();
            assertThat(savedAppt.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
            assertThat(savedAppt.getDoctorNotes()).isEqualTo(notes);
        }

        @Test @DisplayName("Success - Admin completes SCHEDULED appointment")
        void testCompleteAppointment_AdminSuccess() {
            // Arrange
            Long appointmentId = scheduledAppointment.getId();
            String notes = "Admin completed - follow up needed.";
            given(appointmentRepository.findById(appointmentId)).willReturn(Optional.of(scheduledAppointment));
            given(appointmentRepository.save(any(Appointment.class))).willAnswer(inv -> inv.getArgument(0));

            // Act
            AppointmentDTO result = appointmentService.completeAppointment(appointmentId, notes, adminUserDetails);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
            assertThat(result.getDoctorNotes()).isEqualTo(notes);
            verify(appointmentRepository).save(appointmentArgumentCaptor.capture());
            assertThat(appointmentArgumentCaptor.getValue().getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
            assertThat(appointmentArgumentCaptor.getValue().getDoctorNotes()).isEqualTo(notes);
        }

        @Test @DisplayName("Failure - Appointment not SCHEDULED")
        void testCompleteAppointment_WrongStatus() {
            // Arrange
            Long appointmentId = pendingAppointment.getId(); // PENDING status
            String notes = "Some notes";
            given(appointmentRepository.findById(appointmentId)).willReturn(Optional.of(pendingAppointment));

            // Act & Assert
            BadRequestException thrown = assertThrows(BadRequestException.class, () -> {
                appointmentService.completeAppointment(appointmentId, notes, doctor1UserDetails);
            });
            assertEquals("Only SCHEDULED appointments can be marked as completed.", thrown.getMessage());
            verify(appointmentRepository, never()).save(any(Appointment.class));
        }

        @Test @DisplayName("Failure - Access Denied (Patient tries)")
        void testCompleteAppointment_AccessDenied_Patient() {
            // Arrange
            Long appointmentId = scheduledAppointment.getId();
            String notes = "Patient notes";
            given(appointmentRepository.findById(appointmentId)).willReturn(Optional.of(scheduledAppointment));

            // Act & Assert
            AccessDeniedException thrown = assertThrows(AccessDeniedException.class, () -> {
                appointmentService.completeAppointment(appointmentId, notes, patientUserDetails);
            });
            assertEquals("You do not have permission to complete this appointment.", thrown.getMessage());
            verify(appointmentRepository, never()).save(any(Appointment.class));
        }

        @Test @DisplayName("Failure - Access Denied (Different Doctor tries)")
        void testCompleteAppointment_AccessDenied_OtherDoctor() {
            // Arrange
            Long appointmentId = scheduledAppointment.getId(); // Belongs to Doctor 1
            String notes = "Other doc notes";
            given(appointmentRepository.findById(appointmentId)).willReturn(Optional.of(scheduledAppointment));

            // Act & Assert
            AccessDeniedException thrown = assertThrows(AccessDeniedException.class, () -> {
                appointmentService.completeAppointment(appointmentId, notes, doctor2UserDetails); // Doctor 2 tries
            });
            assertEquals("You do not have permission to complete this appointment.", thrown.getMessage());
            verify(appointmentRepository, never()).save(any(Appointment.class));
        }

        @Test @DisplayName("Failure - Appointment Not Found")
        void testCompleteAppointment_NotFound() {
            // Arrange
            Long nonExistentId = 999L;
            String notes = "Notes";
            given(appointmentRepository.findById(nonExistentId)).willReturn(Optional.empty());

            // Act & Assert
            ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class, () -> {
                appointmentService.completeAppointment(nonExistentId, notes, doctor1UserDetails);
            });
            assertTrue(thrown.getMessage().contains("Appointment not found"));
        }

    } // End Nested class for completeAppointment


    // --- ** NEW: Tests for assignMedicationsToAppointment ** ---
    @Nested @DisplayName("Tests for assignMedicationsToAppointment")
    class AssignMedicationsTests {

        @Test @DisplayName("Success - Doctor assigns meds to COMPLETED appointment")
        void testAssignMedications_DoctorSuccess() {
            // Arrange
            Long appointmentId = completedAppointment.getId(); // Must be completed
            List<MedicationInputDTO> inputs = List.of(medInput1, medInput2);

            given(appointmentRepository.findById(appointmentId)).willReturn(Optional.of(completedAppointment));
            // Mock save for each medication
            given(medicationRepository.save(any(Medication.class))).willAnswer(inv -> {
                Medication med = inv.getArgument(0);
                // Assign a dummy ID if needed, or just return the input
                if (med.getMedicationName().equals(medInput1.getMedicationName())) med.setId(301L);
                if (med.getMedicationName().equals(medInput2.getMedicationName())) med.setId(302L);
                med.setPrescribedDate(LocalDateTime.now()); // Simulate PrePersist
                return med;
            });

            // Act
            List<MedicationDTO> result = appointmentService.assignMedicationsToAppointment(appointmentId, inputs, doctor1UserDetails);

            // Assert
            assertThat(result).isNotNull().hasSize(2);
            assertThat(result.get(0).getMedicationName()).isEqualTo(medInput1.getMedicationName());
            assertThat(result.get(1).getMedicationName()).isEqualTo(medInput2.getMedicationName());
            assertThat(result.get(0).getAppointmentId()).isEqualTo(appointmentId);

            // Verify medication repo save called twice with correct data
            verify(medicationRepository, times(2)).save(medicationArgumentCaptor.capture());
            List<Medication> capturedMeds = medicationArgumentCaptor.getAllValues();
            assertThat(capturedMeds.get(0).getMedicationName()).isEqualTo(medInput1.getMedicationName());
            assertThat(capturedMeds.get(0).getAppointment()).isEqualTo(completedAppointment);
            assertThat(capturedMeds.get(0).getPatient()).isEqualTo(patient1);
            assertThat(capturedMeds.get(0).getPrescribingDoctor()).isEqualTo(doctor1);
            assertThat(capturedMeds.get(1).getMedicationName()).isEqualTo(medInput2.getMedicationName());

            verify(appointmentRepository).findById(appointmentId);
        }

        @Test @DisplayName("Success - Admin assigns meds")
        void testAssignMedications_AdminSuccess() {
            // Arrange
            Long appointmentId = completedAppointment.getId();
            List<MedicationInputDTO> inputs = List.of(medInput1);
            given(appointmentRepository.findById(appointmentId)).willReturn(Optional.of(completedAppointment));
            given(medicationRepository.save(any(Medication.class))).willAnswer(inv -> inv.getArgument(0)); // Simple mock save

            // Act
            List<MedicationDTO> result = appointmentService.assignMedicationsToAppointment(appointmentId, inputs, adminUserDetails); // Use Admin

            // Assert
            assertThat(result).isNotNull().hasSize(1);
            verify(medicationRepository).save(any(Medication.class));
        }


        @Test @DisplayName("Failure - Appointment not COMPLETED")
        void testAssignMedications_WrongStatus() {
            // Arrange
            Long appointmentId = scheduledAppointment.getId(); // SCHEDULED status
            List<MedicationInputDTO> inputs = List.of(medInput1);
            given(appointmentRepository.findById(appointmentId)).willReturn(Optional.of(scheduledAppointment));

            // Act & Assert
            BadRequestException thrown = assertThrows(BadRequestException.class, () -> {
                appointmentService.assignMedicationsToAppointment(appointmentId, inputs, doctor1UserDetails);
            });
            assertEquals("Medications can only be assigned to COMPLETED appointments.", thrown.getMessage());
            verifyNoInteractions(medicationRepository); // Save should not be called
        }

        @Test @DisplayName("Failure - Access Denied (Patient tries)")
        void testAssignMedications_AccessDenied_Patient() {
            // Arrange
            Long appointmentId = completedAppointment.getId();
            List<MedicationInputDTO> inputs = List.of(medInput1);
            given(appointmentRepository.findById(appointmentId)).willReturn(Optional.of(completedAppointment));

            // Act & Assert
            AccessDeniedException thrown = assertThrows(AccessDeniedException.class, () -> {
                appointmentService.assignMedicationsToAppointment(appointmentId, inputs, patientUserDetails); // Patient tries
            });
            assertEquals("You do not have permission to assign medication to this appointment.", thrown.getMessage());
            verifyNoInteractions(medicationRepository);
        }

        @Test @DisplayName("Failure - Access Denied (Different Doctor tries)")
        void testAssignMedications_AccessDenied_OtherDoctor() {
            // Arrange
            Long appointmentId = completedAppointment.getId(); // Assigned to Doctor 1
            List<MedicationInputDTO> inputs = List.of(medInput1);
            given(appointmentRepository.findById(appointmentId)).willReturn(Optional.of(completedAppointment));

            // Act & Assert
            AccessDeniedException thrown = assertThrows(AccessDeniedException.class, () -> {
                appointmentService.assignMedicationsToAppointment(appointmentId, inputs, doctor2UserDetails); // Doctor 2 tries
            });
            assertEquals("You do not have permission to assign medication to this appointment.", thrown.getMessage());
            verifyNoInteractions(medicationRepository);
        }

        @Test @DisplayName("Failure - Appointment Not Found")
        void testAssignMedications_AppointmentNotFound() {
            // Arrange
            Long nonExistentId = 999L;
            List<MedicationInputDTO> inputs = List.of(medInput1);
            given(appointmentRepository.findById(nonExistentId)).willReturn(Optional.empty());

            // Act & Assert
            ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class, () -> {
                appointmentService.assignMedicationsToAppointment(nonExistentId, inputs, doctor1UserDetails);
            });
            assertTrue(thrown.getMessage().contains("Appointment not found"));
            verifyNoInteractions(medicationRepository);
        }

        @Test @DisplayName("Success - Empty medication list input")
        void testAssignMedications_EmptyInput() {
            // Arrange
            Long appointmentId = completedAppointment.getId();
            List<MedicationInputDTO> inputs = Collections.emptyList(); // Empty list
            given(appointmentRepository.findById(appointmentId)).willReturn(Optional.of(completedAppointment));

            // Act
            List<MedicationDTO> result = appointmentService.assignMedicationsToAppointment(appointmentId, inputs, doctor1UserDetails);

            // Assert
            assertThat(result).isNotNull().isEmpty(); // Expect empty list back
            verifyNoInteractions(medicationRepository); // Verify save was never called
            verify(appointmentRepository).findById(appointmentId); // Verify appointment was still looked up
        }

    } // End Nested class for assignMedications

    // --- ** NEW: Tests for getMedicationsForAppointment ** ---
    @Nested @DisplayName("Tests for getMedicationsForAppointment")
    class GetMedicationsForAppointmentTests {

        @Test @DisplayName("Success - Patient gets own appointment meds")
        void testGetMedicationsForAppointment_PatientSuccess() {
            // Arrange
            Long appointmentId = completedAppointment.getId();
            given(appointmentRepository.findById(appointmentId)).willReturn(Optional.of(completedAppointment));
            given(medicationRepository.findByAppointmentId(appointmentId)).willReturn(List.of(medication1));

            // Act
            List<MedicationDTO> result = appointmentService.getMedicationsForAppointment(appointmentId, patientUserDetails);

            // Assert
            assertThat(result).isNotNull().hasSize(1);
            assertThat(result.get(0).getMedicationName()).isEqualTo(medication1.getMedicationName());
            verify(appointmentRepository).findById(appointmentId);
            verify(medicationRepository).findByAppointmentId(appointmentId);
        }

        // Similar success tests for Doctor owner and Admin...

        @Test @DisplayName("Failure - Access Denied (Unrelated User)")
        void testGetMedicationsForAppointment_AccessDenied() {
            // Arrange
            Long appointmentId = completedAppointment.getId(); // Belongs to P1/D1
            given(appointmentRepository.findById(appointmentId)).willReturn(Optional.of(completedAppointment));

            // Act & Assert
            AccessDeniedException thrown = assertThrows(AccessDeniedException.class, () -> {
                appointmentService.getMedicationsForAppointment(appointmentId, patient2UserDetails); // Patient 2 tries
            });
            assertEquals("You do not have permission to view medications for this appointment.", thrown.getMessage());
            verify(appointmentRepository).findById(appointmentId); // Find appointment called
            verify(medicationRepository, never()).findByAppointmentId(anyLong()); // But get meds not called
        }

        @Test @DisplayName("Failure - Appointment Not Found")
        void testGetMedicationsForAppointment_NotFound() {
            // Arrange
            Long nonExistentId = 999L;
            given(appointmentRepository.findById(nonExistentId)).willReturn(Optional.empty());

            // Act & Assert
            ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class, () -> {
                appointmentService.getMedicationsForAppointment(nonExistentId, patientUserDetails);
            });
            assertTrue(thrown.getMessage().contains("Appointment not found"));
            verifyNoInteractions(medicationRepository);
        }
    } // End Nested class for getMedicationsForAppointment


    // --- ** NEW: Tests for getMedicationsForPatient ** ---
    @Nested @DisplayName("Tests for getMedicationsForPatient")
    class GetMedicationsForPatientTests {

        @Test
        @DisplayName("Success - Patient gets own meds")
        void testGetMedicationsForPatient_SuccessSelf() {
            // Arrange
            Long patientId = patient1.getId();
            given(patientRepository.existsById(patientId)).willReturn(true); // Assume patient exists
            given(medicationRepository.findByPatientId(patientId)).willReturn(List.of(medication1));

            // Act
            List<MedicationDTO> result = appointmentService.getMedicationsForPatient(patientId, patientUserDetails);

            // Assert
            assertThat(result).isNotNull().hasSize(1);
            assertThat(result.get(0).getMedicationName()).isEqualTo(medication1.getMedicationName());
            verify(patientRepository).existsById(patientId);
            verify(medicationRepository).findByPatientId(patientId);
        }

        @Test
        @DisplayName("Success - Admin gets patient meds")
        void testGetMedicationsForPatient_SuccessAdmin() {
            // Arrange
            Long patientId = patient1.getId();
            given(patientRepository.existsById(patientId)).willReturn(true);
            given(medicationRepository.findByPatientId(patientId)).willReturn(List.of(medication1));

            // Act
            List<MedicationDTO> result = appointmentService.getMedicationsForPatient(patientId, adminUserDetails); // Use Admin user

            // Assert
            assertThat(result).isNotNull().hasSize(1);
            verify(patientRepository).existsById(patientId);
            verify(medicationRepository).findByPatientId(patientId);
        }

        @Test
        @DisplayName("Failure - Access Denied (Other user tries)")
        void testGetMedicationsForPatient_AccessDenied() {
            // Arrange
            Long targetPatientId = patient1.getId();
            // Logged in as Doctor 1 or Patient 2
            AppUserDetails otherUser = doctor1UserDetails;

            // Act & Assert
            AccessDeniedException thrown = assertThrows(AccessDeniedException.class, () -> {
                appointmentService.getMedicationsForPatient(targetPatientId, otherUser);
            });
            assertEquals("You do not have permission to view medications for this patient.", thrown.getMessage());
            verifyNoInteractions(patientRepository); // Doesn't even check if patient exists if permission fails
            verifyNoInteractions(medicationRepository);
        }

    }

} // End of AppointmentServiceTest class