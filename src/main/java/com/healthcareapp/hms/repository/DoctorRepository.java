package com.healthcareapp.hms.repository;

import com.healthcareapp.hms.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);

    List<Doctor> findBySpecializationIgnoreCase(String specialization);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, Long id);
}