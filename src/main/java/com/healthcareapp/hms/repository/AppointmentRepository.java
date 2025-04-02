package com.healthcareapp.hms.repository;

import com.healthcareapp.hms.domain.AppointmentStatus;
import com.healthcareapp.hms.entity.Appointment;
import com.healthcareapp.hms.entity.Doctor;
import com.healthcareapp.hms.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Find appointments for a specific patient
    List<Appointment> findByPatientId(Long patientId);

    // Find appointments for a specific doctor
    List<Appointment> findByDoctorId(Long doctorId);

    // Find appointments for a doctor within a specific time range (useful for checking slots)
    List<Appointment> findByDoctorIdAndAppointmentDateTimeBetween(Long doctorId, LocalDateTime start, LocalDateTime end);

    // Find appointments for a doctor on a specific date with certain statuses
    List<Appointment> findByDoctorIdAndAppointmentDateTimeBetweenAndStatusIn(
            Long doctorId,
            LocalDateTime startOfDay,
            LocalDateTime endOfDay,
            List<AppointmentStatus> statuses
    );

    // Find appointments for a patient with specific statuses
    List<Appointment> findByPatientIdAndStatusIn(Long patientId, List<AppointmentStatus> statuses);

    // Find appointments for a doctor with specific statuses
    List<Appointment> findByDoctorIdAndStatusIn(Long doctorId, List<AppointmentStatus> statuses);

    // Example using @Query for more complex scenarios if needed later
    @Query("SELECT a FROM Appointment a WHERE a.doctor = :doctor AND a.appointmentDateTime >= :start AND a.appointmentDateTime < :end AND a.status = :status")
    List<Appointment> findDoctorAppointmentsByTimeAndStatus(
            @Param("doctor") Doctor doctor,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("status") AppointmentStatus status
    );

}