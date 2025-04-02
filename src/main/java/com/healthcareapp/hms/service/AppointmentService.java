package com.healthcareapp.hms.service;

// Import necessary DTOs for medication
import com.healthcareapp.hms.dto.MedicationDTO;
import com.healthcareapp.hms.dto.MedicationInputDTO;

// Import Medication entity and repository
import com.healthcareapp.hms.entity.Medication;
import com.healthcareapp.hms.repository.MedicationRepository;

// Other existing imports
import com.healthcareapp.hms.domain.AppointmentStatus;
import com.healthcareapp.hms.dto.AppointmentBookingDTO;
import com.healthcareapp.hms.dto.AppointmentDTO;
import com.healthcareapp.hms.dto.AvailableSlotDTO;
import com.healthcareapp.hms.entity.Appointment;
import com.healthcareapp.hms.entity.Doctor;
import com.healthcareapp.hms.entity.Patient;
import com.healthcareapp.hms.exception.BadRequestException;
import com.healthcareapp.hms.exception.ResourceNotFoundException;
import com.healthcareapp.hms.repository.AppointmentRepository;
import com.healthcareapp.hms.repository.DoctorRepository;
import com.healthcareapp.hms.repository.PatientRepository;
import com.healthcareapp.hms.security.AppUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentService.class);

    // --- Constants ---
    private static final LocalTime MORNING_SHIFT_START = LocalTime.of(9, 0);
    private static final LocalTime MORNING_SHIFT_END = LocalTime.of(12, 0);
    private static final LocalTime AFTERNOON_SHIFT_START = LocalTime.of(13, 0); // 1 PM
    private static final LocalTime AFTERNOON_SHIFT_END = LocalTime.of(19, 0); // 7 PM
    private static final Duration SLOT_DURATION = Duration.ofMinutes(30);
    private static final List<AppointmentStatus> CONFLICTING_STATUSES = Arrays.asList(
            AppointmentStatus.PENDING, AppointmentStatus.SCHEDULED
    );

    // --- Repositories ---
    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private DoctorRepository doctorRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private MedicationRepository medicationRepository;

    // --- Get Available Slots Logic ---
    @Transactional(readOnly = true)
    public List<AvailableSlotDTO> getAvailableSlots(Long doctorId, LocalDate date) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", doctorId));
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        List<Appointment> existingAppointments = appointmentRepository
                .findByDoctorIdAndAppointmentDateTimeBetweenAndStatusIn(
                        doctorId, startOfDay, endOfDay, CONFLICTING_STATUSES);
        Set<LocalDateTime> bookedSlots = existingAppointments.stream()
                .map(Appointment::getAppointmentDateTime)
                .collect(Collectors.toSet());
        List<AvailableSlotDTO> potentialSlots = generatePotentialSlots(date);
        LocalDateTime now = LocalDateTime.now();
        List<AvailableSlotDTO> availableSlots = potentialSlots.stream()
                .filter(slot -> !bookedSlots.contains(slot.getStartTime()))
                .filter(slot -> slot.getStartTime().isAfter(now))
                .collect(Collectors.toList());
        logger.info("Found {} available slots for Doctor ID {} on {}", availableSlots.size(), doctorId, date);
        return availableSlots;
    }

    private List<AvailableSlotDTO> generatePotentialSlots(LocalDate date) {
        List<AvailableSlotDTO> slots = new ArrayList<>();
        LocalDateTime slotTime = date.atTime(MORNING_SHIFT_START);
        while (slotTime.isBefore(date.atTime(MORNING_SHIFT_END))) {
            slots.add(new AvailableSlotDTO(slotTime, slotTime.plus(SLOT_DURATION)));
            slotTime = slotTime.plus(SLOT_DURATION);
        }
        slotTime = date.atTime(AFTERNOON_SHIFT_START);
        while (slotTime.isBefore(date.atTime(AFTERNOON_SHIFT_END))) {
            slots.add(new AvailableSlotDTO(slotTime, slotTime.plus(SLOT_DURATION)));
            slotTime = slotTime.plus(SLOT_DURATION);
        }
        return slots;
    }

    // --- Book Appointment Logic ---
    @Transactional
    public AppointmentDTO bookAppointment(Long patientId, AppointmentBookingDTO bookingDto) {
        logger.info("Attempting to book appointment for Patient ID {} with Doctor ID {}", patientId, bookingDto.getDoctorId());
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", patientId));
        Doctor doctor = doctorRepository.findById(bookingDto.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", bookingDto.getDoctorId()));
        LocalDateTime requestedTime = bookingDto.getRequestedDateTime();
        validateRequestedTime(requestedTime, doctor.getId(), patient.getId());
        Appointment newAppointment = new Appointment();
        newAppointment.setPatient(patient);
        newAppointment.setDoctor(doctor);
        newAppointment.setAppointmentDateTime(requestedTime);
        newAppointment.setReason(bookingDto.getReason());
        newAppointment.setStatus(AppointmentStatus.PENDING);
        Appointment savedAppointment = appointmentRepository.save(newAppointment);
        logger.info("Appointment booked successfully with ID: {}", savedAppointment.getId());
        return mapToAppointmentDTO(savedAppointment);
    }

    private void validateRequestedTime(LocalDateTime requestedTime, Long doctorId, Long patientId) {
        if (!requestedTime.isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Requested appointment time must be in the future.");
        }
        if (!isWithinWorkingHours(requestedTime) || (requestedTime.getMinute() != 0 && requestedTime.getMinute() != 30)) {
            logger.warn("Requested time {} is outside working hours or not on a 30min boundary.", requestedTime);
            throw new BadRequestException("Requested time is outside of doctor's working hours or not a valid slot time.");
        }
        LocalDateTime slotEnd = requestedTime.plus(SLOT_DURATION);
        List<Appointment> doctorConflicts = appointmentRepository
                .findByDoctorIdAndAppointmentDateTimeBetweenAndStatusIn(doctorId, requestedTime, slotEnd, CONFLICTING_STATUSES);
        if (!doctorConflicts.isEmpty()) {
            logger.warn("Booking conflict: Doctor {} already has an appointment at {}", doctorId, requestedTime);
            throw new BadRequestException("The selected time slot is no longer available for this doctor.");
        }
    }

    private boolean isWithinWorkingHours(LocalDateTime dateTime) {
        LocalTime time = dateTime.toLocalTime();
        DayOfWeek day = dateTime.getDayOfWeek();
        if (day == DayOfWeek.SUNDAY) return false;
        boolean isMorning = !time.isBefore(MORNING_SHIFT_START) && time.isBefore(MORNING_SHIFT_END);
        boolean isAfternoon = !time.isBefore(AFTERNOON_SHIFT_START) && time.isBefore(AFTERNOON_SHIFT_END);
        return isMorning || isAfternoon;
    }


    // --- Appointment Status Change Methods ---
    @Transactional
    public AppointmentDTO approveAppointment(Long appointmentId, AppUserDetails currentUser) {
        Appointment appointment = findAppointmentById(appointmentId);
        validateDoctorOrAdmin(currentUser, appointment.getDoctor().getId(), "approve");
        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new BadRequestException("Only PENDING appointments can be approved.");
        }
        validateNoConflict(appointment.getDoctor().getId(), appointment.getPatient().getId(), appointment.getAppointmentDateTime());
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        Appointment updatedAppointment = appointmentRepository.save(appointment);
        logger.info("Appointment ID {} approved by user ID {}.", appointmentId, currentUser.getUserId());
        return mapToAppointmentDTO(updatedAppointment);
    }

    @Transactional
    public AppointmentDTO rejectAppointment(Long appointmentId, AppUserDetails currentUser) {
        Appointment appointment = findAppointmentById(appointmentId);
        validateDoctorOrAdmin(currentUser, appointment.getDoctor().getId(), "reject");
        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new BadRequestException("Only PENDING appointments can be rejected.");
        }
        appointment.setStatus(AppointmentStatus.REJECTED);
        Appointment updatedAppointment = appointmentRepository.save(appointment);
        logger.info("Appointment ID {} rejected by user ID {}.", appointmentId, currentUser.getUserId());
        return mapToAppointmentDTO(updatedAppointment);
    }

    @Transactional
    public AppointmentDTO cancelAppointment(Long appointmentId, AppUserDetails currentUser) {
        Appointment appointment = findAppointmentById(appointmentId);
        validatePatientOrDoctorOrAdmin(currentUser, appointment.getPatient().getId(), appointment.getDoctor().getId(), "cancel");
        if (appointment.getStatus() == AppointmentStatus.COMPLETED || appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BadRequestException("Cannot cancel an appointment that is already " + appointment.getStatus());
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment updatedAppointment = appointmentRepository.save(appointment);
        logger.info("Appointment ID {} cancelled by user ID {}.", appointmentId, currentUser.getUserId());
        return mapToAppointmentDTO(updatedAppointment);
    }

    @Transactional
    public AppointmentDTO completeAppointment(Long appointmentId, String doctorNotes, AppUserDetails currentUser) {
        Appointment appointment = findAppointmentById(appointmentId);
        validateDoctorOrAdmin(currentUser, appointment.getDoctor().getId(), "complete");
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new BadRequestException("Only SCHEDULED appointments can be marked as completed.");
        }
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setDoctorNotes(doctorNotes);
        Appointment updatedAppointment = appointmentRepository.save(appointment);
        logger.info("Appointment ID {} completed by user ID {}.", appointmentId, currentUser.getUserId());
        return mapToAppointmentDTO(updatedAppointment);
    }

    // --- Get Appointments Methods --- ADDED BACK ---

    @Transactional(readOnly = true)
    public List<AppointmentDTO> getAppointmentsForPatient(Long patientId) {
        // Check if patient exists first for better error message
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient", "id", patientId);
        }
        // Fetch appointments using the repository method
        List<Appointment> appointments = appointmentRepository.findByPatientId(patientId);
        // Map the list of entities to a list of DTOs
        return appointments.stream()
                .map(this::mapToAppointmentDTO) // Use the existing mapping method
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AppointmentDTO> getAppointmentsForDoctor(Long doctorId) {
        // Check if doctor exists first
        if (!doctorRepository.existsById(doctorId)) {
            throw new ResourceNotFoundException("Doctor", "id", doctorId);
        }
        // Fetch appointments using the repository method
        List<Appointment> appointments = appointmentRepository.findByDoctorId(doctorId);
        // Map the list of entities to a list of DTOs
        return appointments.stream()
                .map(this::mapToAppointmentDTO) // Use the existing mapping method
                .collect(Collectors.toList());
    }
    // --- End Get Appointments Methods ---


    // --- Medication Management Methods ---

    @Transactional
    public List<MedicationDTO> assignMedicationsToAppointment(Long appointmentId, List<MedicationInputDTO> medicationInputs, AppUserDetails currentUser) {
        Appointment appointment = findAppointmentById(appointmentId);
        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new BadRequestException("Medications can only be assigned to COMPLETED appointments.");
        }
        validateDoctorOrAdmin(currentUser, appointment.getDoctor().getId(), "assign medication to");
        List<Medication> savedMedications = new ArrayList<>();
        Patient patient = appointment.getPatient();
        Doctor doctor = appointment.getDoctor();
        for (MedicationInputDTO inputDto : medicationInputs) {
            if (inputDto.getMedicationName() == null || inputDto.getMedicationName().isBlank() ||
                    inputDto.getDosage() == null || inputDto.getDosage().isBlank() ||
                    inputDto.getFrequency() == null || inputDto.getFrequency().isBlank()) {
                logger.warn("Skipping medication assignment due to missing required fields for appointment ID {}", appointmentId);
                continue;
            }
            Medication medication = new Medication();
            medication.setMedicationName(inputDto.getMedicationName());
            medication.setDosage(inputDto.getDosage());
            medication.setFrequency(inputDto.getFrequency());
            medication.setDuration(inputDto.getDuration());
            medication.setInstructions(inputDto.getInstructions());
            medication.setAppointment(appointment);
            medication.setPrescribingDoctor(doctor);
            medication.setPatient(patient);
            savedMedications.add(medicationRepository.save(medication));
            logger.info("Assigned medication '{}' for Appointment ID {}", medication.getMedicationName(), appointmentId);
        }
        return savedMedications.stream().map(this::mapToMedicationDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MedicationDTO> getMedicationsForAppointment(Long appointmentId, AppUserDetails currentUser) {
        Appointment appointment = findAppointmentById(appointmentId);
        validatePatientOrDoctorOrAdmin(currentUser, appointment.getPatient().getId(), appointment.getDoctor().getId(), "view medications for");
        List<Medication> medications = medicationRepository.findByAppointmentId(appointmentId);
        return medications.stream().map(this::mapToMedicationDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MedicationDTO> getMedicationsForPatient(Long patientId, AppUserDetails currentUser) {
        if (!Objects.equals(currentUser.getUserId(), patientId) && !hasRole(currentUser.getAuthorities(), "ROLE_ADMIN")) {
            logger.warn("User ID {} attempted to access medications for Patient ID {} without permission.", currentUser.getUserId(), patientId);
            throw new AccessDeniedException("You do not have permission to view medications for this patient.");
        }
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient", "id", patientId);
        }
        List<Medication> medications = medicationRepository.findByPatientId(patientId);
        return medications.stream().map(this::mapToMedicationDTO).collect(Collectors.toList());
    }


    // --- Helper & Mapping Methods ---

    private Appointment findAppointmentById(Long appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentId));
    }

    private void validateNoConflict(Long doctorId, Long patientId, LocalDateTime requestedTime) {
        LocalDateTime slotEnd = requestedTime.plus(SLOT_DURATION);
        List<Appointment> doctorConflicts = appointmentRepository
                .findByDoctorIdAndAppointmentDateTimeBetweenAndStatusIn(doctorId, requestedTime, slotEnd, List.of(AppointmentStatus.SCHEDULED));
        if (!doctorConflicts.isEmpty()) {
            logger.warn("Approval conflict: Doctor {} already has a scheduled appointment at {}", doctorId, requestedTime);
            throw new BadRequestException("Doctor already has a scheduled conflict at this time slot.");
        }
    }

    // --- Authorization Helper Methods ---
    private void validateDoctorOrAdmin(AppUserDetails currentUser, Long expectedDoctorId, String action) {
        boolean isDoctorOwner = currentUser.getUserType().equals("DOCTOR") && Objects.equals(currentUser.getUserId(), expectedDoctorId);
        boolean isAdmin = hasRole(currentUser.getAuthorities(), "ROLE_ADMIN");
        if (!isDoctorOwner && !isAdmin) {
            logger.warn("User ID {} ({}) attempted to {} appointment owned by Doctor ID {} without permission.", currentUser.getUserId(), currentUser.getUserType(), action, expectedDoctorId);
            throw new AccessDeniedException("You do not have permission to " + action + " this appointment.");
        }
    }

    private void validatePatientOrDoctorOrAdmin(AppUserDetails currentUser, Long expectedPatientId, Long expectedDoctorId, String action) {
        boolean isPatientOwner = currentUser.getUserType().equals("PATIENT") && Objects.equals(currentUser.getUserId(), expectedPatientId);
        boolean isDoctorOwner = currentUser.getUserType().equals("DOCTOR") && Objects.equals(currentUser.getUserId(), expectedDoctorId);
        boolean isAdmin = hasRole(currentUser.getAuthorities(), "ROLE_ADMIN");
        if (!isPatientOwner && !isDoctorOwner && !isAdmin) {
            logger.warn("User ID {} ({}) attempted to {} appointment (Patient ID {}, Doctor ID {}) without permission.", currentUser.getUserId(), currentUser.getUserType(), action, expectedPatientId, expectedDoctorId);
            throw new AccessDeniedException("You do not have permission to " + action + " this appointment.");
        }
    }

    private boolean hasRole(Collection<? extends GrantedAuthority> authorities, String roleName) {
        if (authorities == null) return false;
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(roleName::equals);
    }

    // --- DTO Mapping Methods ---
    private AppointmentDTO mapToAppointmentDTO(Appointment appointment) {
        AppointmentDTO dto = new AppointmentDTO();
        dto.setId(appointment.getId());
        dto.setAppointmentDateTime(appointment.getAppointmentDateTime());
        dto.setReason(appointment.getReason());
        dto.setStatus(appointment.getStatus());
        dto.setDoctorNotes(appointment.getDoctorNotes());
        dto.setCreatedAt(appointment.getCreatedAt());
        if (appointment.getPatient() != null) {
            dto.setPatientId(appointment.getPatient().getId());
            dto.setPatientName(appointment.getPatient().getName());
        }
        if (appointment.getDoctor() != null) {
            dto.setDoctorId(appointment.getDoctor().getId());
            dto.setDoctorName(appointment.getDoctor().getName());
            dto.setDoctorSpecialization(appointment.getDoctor().getSpecialization());
        }
        return dto;
    }

    private MedicationDTO mapToMedicationDTO(Medication medication) {
        MedicationDTO dto = new MedicationDTO();
        dto.setId(medication.getId());
        dto.setMedicationName(medication.getMedicationName());
        dto.setDosage(medication.getDosage());
        dto.setFrequency(medication.getFrequency());
        dto.setDuration(medication.getDuration());
        dto.setInstructions(medication.getInstructions());
        dto.setPrescribedDate(medication.getPrescribedDate());
        if (medication.getAppointment() != null) {
            dto.setAppointmentId(medication.getAppointment().getId());
        }
        if (medication.getPrescribingDoctor() != null) {
            dto.setPrescribingDoctorId(medication.getPrescribingDoctor().getId());
            dto.setPrescribingDoctorName(medication.getPrescribingDoctor().getName());
        }
        return dto;
    }

    // --- Get Doctors By Specialization ---
    @Transactional(readOnly = true)
    public List<Doctor> getDoctorsBySpecialization(String specialization) {
        return doctorRepository.findBySpecializationIgnoreCase(specialization);
    }
}