package org.skinAI.services.Impl;

import org.skinAI.mapper.CaseFollowupMapper;
import org.skinAI.mapper.MedicalCaseMapper;
import org.skinAI.pojo.medical.CaseFollowup;
import org.skinAI.pojo.medical.MedicalCase;
import org.skinAI.services.MedicalCaseService;
import org.skinAI.utils.ThreadLocalUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class MedicalCaseServiceImpl implements MedicalCaseService {

    private final MedicalCaseMapper medicalCaseMapper;
    private final CaseFollowupMapper caseFollowupMapper;

    public MedicalCaseServiceImpl(MedicalCaseMapper medicalCaseMapper, CaseFollowupMapper caseFollowupMapper) {
        this.medicalCaseMapper = medicalCaseMapper;
        this.caseFollowupMapper = caseFollowupMapper;
    }

    @Override
    public MedicalCase create(MedicalCase medicalCase) {
        medicalCase.setDoctorId(currentDoctorId());
        if (medicalCase.getStatus() == null || medicalCase.getStatus().isBlank()) {
            medicalCase.setStatus("PENDING");
        }
        if (medicalCase.getCaseNo() == null || medicalCase.getCaseNo().isBlank()) {
            medicalCase.setCaseNo(genCaseNo(medicalCase.getDoctorId()));
        }
        if (medicalCase.getCheckTime() == null) {
            medicalCase.setCheckTime(LocalDateTime.now());
        }
        medicalCaseMapper.insert(medicalCase);
        return medicalCaseMapper.selectById(medicalCase.getId(), currentDoctorId());
    }

    @Override
    public MedicalCase update(MedicalCase medicalCase) {
        medicalCase.setDoctorId(currentDoctorId());
        medicalCaseMapper.update(medicalCase);
        return medicalCaseMapper.selectById(medicalCase.getId(), currentDoctorId());
    }

    @Override
    public int deleteById(Long id) {
        return medicalCaseMapper.deleteById(id, currentDoctorId());
    }

    @Override
    public MedicalCase getById(Long id) {
        return medicalCaseMapper.selectById(id, currentDoctorId());
    }

    @Override
    public List<MedicalCase> list(String status, Long patientId, String keyword) {
        return medicalCaseMapper.selectByDoctor(currentDoctorId(), status, patientId, keyword);
    }

    @Override
    public int updateStatus(Long id, String status) {
        return medicalCaseMapper.updateStatus(id, currentDoctorId(), status);
    }

    @Override
    public CaseFollowup addFollowup(Long caseId, CaseFollowup followup) {
        ensureCaseOwned(caseId);
        followup.setCaseId(caseId);
        if (followup.getFollowupTime() == null) {
            followup.setFollowupTime(LocalDateTime.now());
        }
        caseFollowupMapper.insert(followup);
        List<CaseFollowup> list = caseFollowupMapper.selectByCaseId(caseId);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<CaseFollowup> listFollowups(Long caseId) {
        ensureCaseOwned(caseId);
        return caseFollowupMapper.selectByCaseId(caseId);
    }

    private void ensureCaseOwned(Long caseId) {
        MedicalCase medicalCase = medicalCaseMapper.selectById(caseId, currentDoctorId());
        if (medicalCase == null) {
            throw new RuntimeException("case not found");
        }
    }

    private Long currentDoctorId() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Object raw = claims.get("userid");
        if (raw instanceof Number number) {
            return number.longValue();
        }
        throw new RuntimeException("invalid login state");
    }

    private String genCaseNo(Long doctorId) {
        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rand = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "CASE-" + doctorId + "-" + timePart + "-" + rand;
    }
}
