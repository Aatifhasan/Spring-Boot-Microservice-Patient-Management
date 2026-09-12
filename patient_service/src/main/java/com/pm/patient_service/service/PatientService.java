package com.pm.patient_service.service;

import com.pm.patient_service.dto.PatientRequestDTO;
import com.pm.patient_service.dto.PatientResponseDTO;
import com.pm.patient_service.exception.EmailAlreadyExistsException;
import com.pm.patient_service.exception.PatientNotFoundException;
import com.pm.patient_service.grpc.BillingServiceGrpcClient;
import com.pm.patient_service.kafka.KafkaProducer;
import com.pm.patient_service.mapper.PatientMapper;
import com.pm.patient_service.models.Patient;
import com.pm.patient_service.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final BillingServiceGrpcClient billingServiceGrpcClient;
    private final KafkaProducer kafkaProducer;
    private static final Logger log = LoggerFactory.getLogger(PatientService.class);

    public PatientService(PatientRepository patientRepository,
                          BillingServiceGrpcClient billingServiceGrpcClient,
                          KafkaProducer kafkaProducer) {

        this.patientRepository = patientRepository;
        this.billingServiceGrpcClient= billingServiceGrpcClient;
        this.kafkaProducer = kafkaProducer;
    }

    public List<PatientResponseDTO> getAllPatients() {
        List patients = patientRepository.findAll();

        List<PatientResponseDTO> patientResponseDTOS = patients.stream()
                .map(patient -> PatientMapper.toDTO((Patient) patient))
                .toList();
        return patientResponseDTOS;
    }

    public PatientResponseDTO createPatient(PatientRequestDTO patientRequestDTO) {

        if(patientRepository.existsByEmail(
                patientRequestDTO.getEmail())){
            throw new EmailAlreadyExistsException
                    ("Patient with the same Email already exists : "+patientRequestDTO.getEmail());
        }

        Patient newPatient = patientRepository.save(PatientMapper.toModel(patientRequestDTO));

        // Call the Billing Service to create a billing account for the new patient
        try {
            billingServiceGrpcClient.createBillingAccount(newPatient.getId().toString(),
                    newPatient.getName(),
                    newPatient.getEmail());
        } catch (Exception e) {
            // Handle the exception, e.g., log it, throw a custom exception, etc.
            log.error("Billing Service is down, but continuing...", e.getMessage());
        }
        //Kafka
        kafkaProducer.sendEvent(newPatient);
        return PatientMapper.toDTO(newPatient);
    }

    public PatientResponseDTO updatePatient(UUID id, PatientRequestDTO patientRequestDTO) {
        Patient patient= patientRepository.findById(id).orElseThrow(()-> new PatientNotFoundException("Patient not found with id : "+id));

        // Check if the email is being updated and if it already exists in the database
        //patientRepository.existByEmailIdNot(patientRequestDTO.getEmail(),id)==!patient.getEmail().equals(patientRequestDTO.getEmail())
        //                && patientRepository.existsByEmail(patientRequestDTO.getEmail()))
        if(!patient.getEmail().equals(patientRequestDTO.getEmail())
                && patientRepository.existsByEmail(patientRequestDTO.getEmail())){
            throw new EmailAlreadyExistsException("Patient with the same Email already exists : "+patientRequestDTO.getEmail());
        }

        patient.setName(patientRequestDTO.getName());
        patient.setEmail(patientRequestDTO.getEmail());
        patient.setAddress(patientRequestDTO.getAddress());
        patient.setDateOfBirth(LocalDate.parse(patientRequestDTO.getDateOfBirth()));

        Patient updatedPatient = patientRepository.save(patient);
        return PatientMapper.toDTO(updatedPatient);
    }

    public void deletePatient(UUID id) {
        Patient patient= patientRepository.findById(id).orElseThrow(()-> new PatientNotFoundException("Patient not found with id : "+id));
        patientRepository.deleteById(id);
    }
    
}
