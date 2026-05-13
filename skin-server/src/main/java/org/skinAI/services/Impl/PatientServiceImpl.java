package org.skinAI.services.Impl;

import org.skinAI.mapper.PatientMapper;
import org.skinAI.pojo.medical.Patient;
import org.skinAI.services.PatientService;
import org.skinAI.utils.ThreadLocalUtil;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PatientServiceImpl implements PatientService {

    private final PatientMapper patientMapper;

    public PatientServiceImpl(PatientMapper patientMapper) {
        this.patientMapper = patientMapper;
    }

    @Override
    public Patient create(Patient patient) {
        patient.setDoctorId(currentDoctorId());
        patientMapper.insert(patient);
        return patientMapper.selectById(patient.getId(), currentDoctorId());
    }

    @Override
    public Patient update(Patient patient) {
        patient.setDoctorId(currentDoctorId());
        patientMapper.update(patient);
        return patientMapper.selectById(patient.getId(), currentDoctorId());
    }

    @Override
    public int deleteById(Long id) {
        return patientMapper.deleteById(id, currentDoctorId());
    }

    @Override
    public Patient getById(Long id) {
        return patientMapper.selectById(id, currentDoctorId());
    }

    @Override
    public List<Patient> list(String keyword) {
        return patientMapper.selectByDoctor(currentDoctorId(), keyword);
    }

    private Long currentDoctorId() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Object raw = claims.get("userid");
        if (raw instanceof Number number) {
            return number.longValue();
        }
        throw new RuntimeException("invalid login state");
    }
}

