package com.healthcareapp.hms.security;

import com.healthcareapp.hms.entity.Admin; // Import Admin
import com.healthcareapp.hms.entity.Doctor; // Import Doctor
import com.healthcareapp.hms.entity.Patient;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AppUserDetails implements UserDetails {

    private final String username; // Will store email
    private final String password; // Will store hashed password
    private final Collection<? extends GrantedAuthority> authorities;
    private final Long userId;
    private final String userType; // "PATIENT", "DOCTOR", or "ADMIN"
    private final String name; // Store the actual name for display

    // Constructor for Patient
    public AppUserDetails(Patient patient) {
        this.username = patient.getEmail();
        this.password = patient.getPassword();
        this.userId = patient.getId();
        this.userType = "PATIENT";
        this.name = patient.getName(); // Store name
        Set<GrantedAuthority> auths = new HashSet<>();
        auths.add(new SimpleGrantedAuthority("ROLE_PATIENT"));
        this.authorities = Collections.unmodifiableSet(auths);
    }

    // Constructor for Doctor (NEW)
    public AppUserDetails(Doctor doctor) {
        this.username = doctor.getEmail();
        this.password = doctor.getPassword();
        this.userId = doctor.getId();
        this.userType = "DOCTOR";
        this.name = doctor.getName(); // Store name
        Set<GrantedAuthority> auths = new HashSet<>();
        auths.add(new SimpleGrantedAuthority("ROLE_DOCTOR"));
        this.authorities = Collections.unmodifiableSet(auths);
    }

    // Constructor for Admin (NEW)
    public AppUserDetails(Admin admin) {
        this.username = admin.getEmail();
        this.password = admin.getPassword();
        this.userId = admin.getId();
        this.userType = "ADMIN";
        this.name = admin.getName(); // Store name
        Set<GrantedAuthority> auths = new HashSet<>();
        auths.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        this.authorities = Collections.unmodifiableSet(auths);
    }


    // --- UserDetails Methods --- // (No changes below needed for now)

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override
    public String getPassword() { return password; }
    @Override
    public String getUsername() { return username; } // Return email as username
    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return true; }

    // --- Custom Getters ---
    public Long getUserId() { return userId; }
    public String getUserType() { return userType; }
    public String getName() { return name; } // Getter for name

}