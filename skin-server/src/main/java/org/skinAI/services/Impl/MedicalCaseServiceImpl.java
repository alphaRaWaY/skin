package org.skinAI.services.Impl;

import org.skinAI.mapper.CaseFollowupMapper;
import org.skinAI.mapper.CaseImageMapper;
import org.skinAI.mapper.MedicalCaseMapper;
import org.skinAI.pojo.medical.CaseFollowup;
import org.skinAI.pojo.medical.CaseImage;
import org.skinAI.pojo.medical.MedicalCase;
import org.skinAI.services.MedicalCaseService;
import org.skinAI.services.OssService;
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
    private final CaseImageMapper caseImageMapper;
    private final OssService ossService;

    public MedicalCaseServiceImpl(
            MedicalCaseMapper medicalCaseMapper,
            CaseFollowupMapper caseFollowupMapper,
            CaseImageMapper caseImageMapper,
            OssService ossService
    ) {
        this.medicalCaseMapper = medicalCaseMapper;
        this.caseFollowupMapper = caseFollowupMapper;
        this.caseImageMapper = caseImageMapper;
        this.ossService = ossService;
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
        Long doctorId = currentDoctorId();
        MedicalCase medicalCase = medicalCaseMapper.selectById(id, doctorId);
        if (medicalCase == null) {
            return 0;
        }
        List<CaseImage> images = caseImageMapper.selectByCaseId(id);
        int affected = medicalCaseMapper.deleteById(id, doctorId);
        if (affected > 0) {
            for (CaseImage image : images) {
                if (image.getObjectKey() == null || image.getObjectKey().isBlank()) {
                    continue;
                }
                try {
                    ossService.deleteFile(image.getObjectKey());
                } catch (Exception ignored) {
                }
            }
        }
        return affected;
    }

    @Override
    public MedicalCase getById(Long id) {
        MedicalCase medicalCase = medicalCaseMapper.selectById(id, currentDoctorId());
        if (medicalCase == null) {
            return null;
        }
        medicalCase.setImageUrl(resolveImageKey(id, "ORIGINAL"));
        medicalCase.setHeatmapUrl(resolveImageKey(id, "HEATMAP"));
        return medicalCase;
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

    private String resolveImageKey(Long caseId, String imageType) {
        CaseImage image = caseImageMapper.selectByCaseIdAndType(caseId, imageType);
        if (image == null && "ORIGINAL".equals(imageType)) {
            image = caseImageMapper.selectPrimaryByCaseId(caseId);
        }
        if (image == null) {
            return null;
        }
        return image.getObjectKey() != null && !image.getObjectKey().isBlank()
                ? image.getObjectKey()
                : image.getPublicUrl();
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
