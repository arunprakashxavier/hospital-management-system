package com.healthcareapp.hms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "patients", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email"),
        @UniqueConstraint(columnNames = "personalNumber")
})
@Data
@NoArgsConstructor
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name cannot be blank")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Column(nullable = false, length = 100)
    private String name;

    @NotNull(message = "Age cannot be null")
    @Min(value = 0, message = "Age must be positive")
    @Column(nullable = false)
    private Integer age;

    @NotNull(message = "Date of birth cannot be null")
    @Past(message = "Date of birth must be in the past")
    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @NotBlank(message = "Gender cannot be blank")
    @Column(nullable = false, length = 10)
    private String gender;

    @NotBlank(message = "Personal number cannot be blank")
    @Column(nullable = false, unique = true, length = 20)
    private String personalNumber;

    @NotBlank(message = "Address cannot be blank")
    @Column(nullable = false, length = 255)
    private String address;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be valid")
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @NotBlank(message = "Guardian name cannot be blank")
    @Column(nullable = false, length = 100)
    private String guardianName;

    @NotBlank(message = "Guardian relation cannot be blank")
    @Column(nullable = false, length = 50)
    private String guardianRelation;

    @NotBlank(message = "Guardian phone number cannot be blank")
    @Pattern(regexp = "^\\+?[0-9. ()-]{7,25}$", message = "Invalid phone number format")
    @Column(nullable = false, length = 25)
    private String guardianPhoneNumber;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Column(nullable = false, length = 60)
    private String password;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @OneToMany(
            mappedBy = "patient",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<Appointment> appointments = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Patient(String name, Integer age, LocalDate dateOfBirth, String gender, String personalNumber, String address, String email, String guardianName, String guardianRelation, String guardianPhoneNumber, String password) {
        this.name = name;
        this.age = age;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.personalNumber = personalNumber;
        this.address = address;
        this.email = email;
        this.guardianName = guardianName;
        this.guardianRelation = guardianRelation;
        this.guardianPhoneNumber = guardianPhoneNumber;
        this.password = password;
    }

    public void addAppointment(Appointment appointment) {
        appointments.add(appointment);
        appointment.setPatient(this);
    }

    public void removeAppointment(Appointment appointment) {
        appointments.remove(appointment);
        appointment.setPatient(null);
    }
}