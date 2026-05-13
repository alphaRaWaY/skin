package org.skinAI.services.Impl;

import org.skinAI.mapper.MedicalCaseMapper;
import org.skinAI.pojo.medical.DashboardSummary;
import org.skinAI.services.DashboardService;
import org.skinAI.utils.ThreadLocalUtil;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final MedicalCaseMapper medicalCaseMapper;

    public DashboardServiceImpl(MedicalCaseMapper medicalCaseMapper) {
        this.medicalCaseMapper = medicalCaseMapper;
    }

    @Override
    public DashboardSummary summary() {
        Long doctorId = currentDoctorId();
        DashboardSummary summary = new DashboardSummary();
        summary.setTodayDiagnosed(safeCount(medicalCaseMapper.countTodayDiagnosed(doctorId)));
        summary.setPendingCases(safeCount(medicalCaseMapper.countByStatus(doctorId, "PENDING")));
        summary.setFollowupCases(safeCount(medicalCaseMapper.countByStatus(doctorId, "FOLLOWUP")));
        summary.setHistoryCases(safeCount(medicalCaseMapper.countByStatus(doctorId, "DONE")));
        return summary;
    }

    private Integer safeCount(Integer value) {
        return value == null ? 0 : value;
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

