package com.healthcareapp.hms.config;

import com.healthcareapp.hms.entity.Admin;
import com.healthcareapp.hms.repository.AdminRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // Use the same encoder bean

    // Define default admin credentials (consider moving to application.properties for better security)
    private static final String ADMIN_EMAIL = "admin@healthcare.com";
    private static final String ADMIN_PASSWORD = "adminpassword"; // Change this!
    private static final String ADMIN_NAME = "System Admin";


    @Override
    @Transactional // Ensures the check and creation happen atomically
    public void run(String... args) throws Exception {
        // Check if an Admin user already exists
        if (adminRepository.count() == 0) {
            // No admin found, create one
            logger.info("No admin user found. Creating default admin...");
            Admin defaultAdmin = new Admin();
            defaultAdmin.setName(ADMIN_NAME);
            defaultAdmin.setEmail(ADMIN_EMAIL);
            // IMPORTANT: Encode the password before saving
            defaultAdmin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));

            adminRepository.save(defaultAdmin);
            logger.info("Default admin user created with email: {}", ADMIN_EMAIL);
            logger.warn("Default admin password is '{}'. Please change it immediately through secure means!", ADMIN_PASSWORD);
        } else {
            logger.info("Admin user already exists. Skipping default admin creation.");
            // You could log the existing admin email for reference if needed
            adminRepository.findFirstByOrderByIdAsc().ifPresent(admin ->
                    logger.info("Existing admin email: {}", admin.getEmail())
            );
        }
    }
}