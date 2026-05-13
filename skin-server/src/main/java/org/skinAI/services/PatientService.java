package org.skinAI.services;

import org.skinAI.pojo.medical.Patient;

import java.util.List;

public interface PatientService {
    Patient create(Patient patient);

    Patient update(Patient patient);

    int deleteById(Long id);

    Patient getById(Long id);

    List<Patient> list(String keyword);
}

