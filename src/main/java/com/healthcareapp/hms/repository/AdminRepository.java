package com.healthcareapp.hms.repository;

import com.healthcareapp.hms.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByEmail(String email);
    boolean existsByEmail(String email);
    // Find the first admin (useful for checking if one exists)
    Optional<Admin> findFirstByOrderByIdAsc();
    long count(); // To check if any admin exists
}