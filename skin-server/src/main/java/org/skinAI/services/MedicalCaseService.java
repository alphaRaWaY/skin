package org.skinAI.services;

import org.skinAI.pojo.medical.CaseFollowup;
import org.skinAI.pojo.medical.MedicalCase;

import java.util.List;

public interface MedicalCaseService {
    MedicalCase create(MedicalCase medicalCase);

    MedicalCase update(MedicalCase medicalCase);

    int deleteById(Long id);

    MedicalCase getById(Long id);

    List<MedicalCase> list(String status, Long patientId, String keyword);

    int updateStatus(Long id, String status);

    CaseFollowup addFollowup(Long caseId, CaseFollowup followup);

    List<CaseFollowup> listFollowups(Long caseId);
}
