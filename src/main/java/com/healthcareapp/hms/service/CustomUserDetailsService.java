package com.healthcareapp.hms.service;

import com.healthcareapp.hms.entity.Admin; // Import Admin
import com.healthcareapp.hms.entity.Doctor; // Import Doctor
import com.healthcareapp.hms.entity.Patient;
import com.healthcareapp.hms.repository.AdminRepository; // Import Admin Repo
import com.healthcareapp.hms.repository.DoctorRepository; // Import Doctor Repo
import com.healthcareapp.hms.repository.PatientRepository;
import com.healthcareapp.hms.security.AppUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired // Inject Doctor Repository
    private DoctorRepository doctorRepository;

    @Autowired // Inject Admin Repository
    private AdminRepository adminRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Username is expected to be the email address

        // 1. Try finding a Patient
        Optional<Patient> patientOptional = patientRepository.findByEmail(username);
        if (patientOptional.isPresent()) {
            return new AppUserDetails(patientOptional.get());
        }

        // 2. If not Patient, try finding a Doctor
        Optional<Doctor> doctorOptional = doctorRepository.findByEmail(username);
        if (doctorOptional.isPresent()) {
            return new AppUserDetails(doctorOptional.get());
        }

        // 3. If not Doctor, try finding an Admin
        Optional<Admin> adminOptional = adminRepository.findByEmail(username);
        if (adminOptional.isPresent()) {
            return new AppUserDetails(adminOptional.get());
        }

        // 4. If user not found in any table
        throw new UsernameNotFoundException("User not found with email: " + username);
    }
}