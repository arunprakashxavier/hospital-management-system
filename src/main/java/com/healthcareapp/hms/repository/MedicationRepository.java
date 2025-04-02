package com.healthcareapp.hms.repository;

import com.healthcareapp.hms.entity.Medication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicationRepository extends JpaRepository<Medication, Long> {

    /**
     * Finds all medications prescribed during a specific appointment.
     * @param appointmentId The ID of the appointment.
     * @return A list of medications for that appointment.
     */
    List<Medication> findByAppointmentId(Long appointmentId);

    /**
     * Finds all medications prescribed for a specific patient across all appointments.
     * Made efficient by the direct patient_id link in the Medication entity.
     * @param patientId The ID of the patient.
     * @return A list of all medications prescribed to the patient.
     */
    List<Medication> findByPatientId(Long patientId);

    /**
     * Finds all medications prescribed by a specific doctor across all appointments.
     * Made efficient by the direct doctor_id link in the Medication entity.
     * @param doctorId The ID of the prescribing doctor.
     * @return A list of all medications prescribed by the doctor.
     */
    List<Medication> findByPrescribingDoctorId(Long doctorId);

    // Add more specific finders if needed later, e.g., by patient and medication name
    // List<Medication> findByPatientIdAndMedicationNameContainingIgnoreCase(Long patientId, String medicationName);

}