package com.healthcareapp.hms.repository;

import com.healthcareapp.hms.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    // Find patient by email (useful for login and checking duplicates)
    Optional<Patient> findByEmail(String email);

    // Check if email exists
    boolean existsByEmail(String email);

    // Check if personal number exists
    boolean existsByPersonalNumber(String personalNumber);

    // *** ADDED: Check if personal number exists for another patient ***
    boolean existsByPersonalNumberAndIdNot(String personalNumber, Long id);
}