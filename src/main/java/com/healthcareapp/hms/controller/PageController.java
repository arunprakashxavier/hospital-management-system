package com.healthcareapp.hms.controller;

import com.healthcareapp.hms.dto.AppointmentDTO;
import com.healthcareapp.hms.dto.MedicationDTO;
import com.healthcareapp.hms.dto.PatientRegistrationDTO;
import com.healthcareapp.hms.dto.DoctorProfileDTO;
import com.healthcareapp.hms.dto.PatientProfileDTO;
import com.healthcareapp.hms.security.AppUserDetails;
import com.healthcareapp.hms.service.AppointmentService;
import com.healthcareapp.hms.service.DoctorService;
import com.healthcareapp.hms.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;

@Controller
public class PageController {

    // Inject Services
    @Autowired
    private AppointmentService appointmentService;
    @Autowired
    private DoctorService doctorService; // *** UNCOMMENTED/ADDED Injection ***
    @Autowired
    private PatientService patientService; // *** ADDED Injection ***


    // Home page
    @GetMapping("/")
    public String homePage() {
        return "index";
    }

    // Registration Page
    @GetMapping("/patient/register")
    public String showPatientRegistrationForm(Model model) {
        model.addAttribute("patientDto", new PatientRegistrationDTO());
        return "patient-register";
    }

    // Login Pages
    @GetMapping("/patient/login")
    public String showPatientLoginForm(@RequestParam(value = "error", required = false) String error,
                                       @RequestParam(value = "logout", required = false) String logout,
                                       Model model) {
        if (error != null) {
            model.addAttribute("loginError", "Invalid username or password.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been logged out successfully.");
        }
        return "patient-login";
    }

    @GetMapping("/doctor/login")
    public String showDoctorLoginForm(Model model) {
        return "doctor-login";
    }

    // Dashboards
    @GetMapping("/patient/dashboard")
    @PreAuthorize("hasRole('PATIENT')")
    public String patientDashboard(Model model) {
        // TODO: Fetch dashboard summary
        return "patient-dashboard";
    }

    @GetMapping("/doctor/dashboard")
    @PreAuthorize("hasRole('DOCTOR')")
    public String doctorDashboard(Model model) {
        // TODO: Fetch dashboard summary
        return "doctor-dashboard";
    }

    // Appointment Booking Page
    @GetMapping("/patient/book-appointment")
    @PreAuthorize("hasRole('PATIENT')")
    public String showBookAppointmentPage(Model model) {
        // TODO: Add specializations if needed
        return "patient-book-appointment";
    }

    // === Doctor Pages ===

    @GetMapping("/doctor/appointments")
    @PreAuthorize("hasRole('DOCTOR')")
    public String doctorAppointmentsPage(Model model, @AuthenticationPrincipal AppUserDetails currentUser) {
        Long doctorId = currentUser.getUserId();
        List<AppointmentDTO> appointments = Collections.emptyList();
        try {
            appointments = appointmentService.getAppointmentsForDoctor(doctorId);
        } catch (Exception e) {
            System.err.println("Error fetching appointments for doctor " + doctorId + ": " + e.getMessage());
            model.addAttribute("errorMessage", "Could not load appointments.");
        }
        model.addAttribute("appointments", appointments);
        return "doctor-appointments";
    }

    // --- UPDATED Doctor Profile Page ---
    @GetMapping("/doctor/profile")
    @PreAuthorize("hasRole('DOCTOR')")
    public String doctorProfilePage(Model model, @AuthenticationPrincipal AppUserDetails currentUser) {
        try {
            // Fetch full profile DTO from DoctorService
            DoctorProfileDTO doctorProfile = doctorService.getDoctorProfile(currentUser.getUserId(), currentUser);
            model.addAttribute("doctorProfile", doctorProfile); // Add DTO to model
        } catch (Exception e) {
            System.err.println("Error fetching profile for doctor " + currentUser.getUserId() + ": " + e.getMessage());
            model.addAttribute("errorMessage", "Could not load profile details.");
            // Add empty DTO to prevent Thymeleaf errors if needed, or handle in template
            model.addAttribute("doctorProfile", new DoctorProfileDTO());
        }
        return "doctor-profile"; // Return view name
    }
    // ---------------------------------

    // === Patient Pages ===

    @GetMapping("/patient/appointments")
    @PreAuthorize("hasRole('PATIENT')")
    public String patientAppointmentsPage(Model model, @AuthenticationPrincipal AppUserDetails currentUser) {
        Long patientId = currentUser.getUserId();
        List<AppointmentDTO> appointments = Collections.emptyList();
        try {
            appointments = appointmentService.getAppointmentsForPatient(patientId);
        } catch (Exception e) {
            System.err.println("Error fetching appointments for patient " + patientId + ": " + e.getMessage());
            model.addAttribute("errorMessage", "Could not load appointments.");
        }
        model.addAttribute("appointments", appointments);
        return "patient-appointments";
    }

    // --- UPDATED Patient Profile Page ---
    @GetMapping("/patient/profile")
    @PreAuthorize("hasRole('PATIENT')")
    public String patientProfilePage(Model model, @AuthenticationPrincipal AppUserDetails currentUser) {
        try {
            // Fetch full profile DTO from PatientService
            PatientProfileDTO patientProfile = patientService.getPatientProfile(currentUser.getUserId(), currentUser);
            model.addAttribute("patientProfile", patientProfile); // Add DTO to model
        } catch (Exception e) {
            System.err.println("Error fetching profile for patient " + currentUser.getUserId() + ": " + e.getMessage());
            model.addAttribute("errorMessage", "Could not load profile details.");
            // Add empty DTO to prevent Thymeleaf errors if needed, or handle in template
            model.addAttribute("patientProfile", new PatientProfileDTO());
        }
        return "patient-profile"; // Return view name
    }
    // ----------------------------------

    @GetMapping("/patient/medications")
    @PreAuthorize("hasRole('PATIENT')")
    public String patientMedicationsPage(Model model, @AuthenticationPrincipal AppUserDetails currentUser) {
        Long patientId = currentUser.getUserId();
        List<MedicationDTO> medications = Collections.emptyList();
        try {
            medications = appointmentService.getMedicationsForPatient(patientId, currentUser);
        } catch (Exception e) {
            System.err.println("Error fetching medications for patient " + patientId + ": " + e.getMessage());
            model.addAttribute("errorMessage", "Could not load medication list.");
        }
        model.addAttribute("medications", medications);
        return "patient-medications";
    }

}