package com.pm.patient_service.repository;

import com.pm.patient_service.models.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {
    boolean existsByEmail(String email);
//    boolean existsByEmailIdNot(String email, UUID id);
}
