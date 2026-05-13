package org.skinAI.services.Impl;

import org.skinAI.mapper.*;
import org.skinAI.pojo.medical.CaseImage;
import org.skinAI.pojo.medical.MedicalCase;
import org.skinAI.pojo.medical.Patient;
import org.skinAI.pojo.report.ConceptScore;
import org.skinAI.pojo.report.Report;
import org.skinAI.services.OssService;
import org.skinAI.services.ReportService;
import org.skinAI.utils.ThreadLocalUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    private final ConceptDictionaryMapper conceptDictionaryMapper;
    private final OssService ossService;
    private final PatientMapper patientMapper;
    private final MedicalCaseMapper medicalCaseMapper;
    private final CaseAnalysisMapper caseAnalysisMapper;
    private final CaseConceptScoreMapper caseConceptScoreMapper;
    private final CaseImageMapper caseImageMapper;

    public ReportServiceImpl(
            ConceptDictionaryMapper conceptDictionaryMapper,
            OssService ossService,
            PatientMapper patientMapper,
            MedicalCaseMapper medicalCaseMapper,
            CaseAnalysisMapper caseAnalysisMapper,
            CaseConceptScoreMapper caseConceptScoreMapper,
            CaseImageMapper caseImageMapper
    ) {
        this.conceptDictionaryMapper = conceptDictionaryMapper;
        this.ossService = ossService;
        this.patientMapper = patientMapper;
        this.medicalCaseMapper = medicalCaseMapper;
        this.caseAnalysisMapper = caseAnalysisMapper;
        this.caseConceptScoreMapper = caseConceptScoreMapper;
        this.caseImageMapper = caseImageMapper;
    }

    @Override
    public int addReport(Report report) {
        Long doctorId = currentDoctorId();
        Patient patient = findOrCreatePatient(report, doctorId);

        MedicalCase medicalCase = new MedicalCase();
        medicalCase.setDoctorId(doctorId);
        medicalCase.setPatientId(patient.getId());
        medicalCase.setStatus("DONE");
        medicalCase.setChiefComplaint(report.getSymptoms());
        medicalCase.setPresentHistory(report.getSymptoms());
        medicalCase.setTreatmentHistory(report.getTreatment());
        medicalCase.setDuration(report.getDuration());
        medicalCase.setExtraNotes(report.getOther());
        medicalCase.setDiagnosedType(report.getDiseaseType());
        medicalCase.setAiAdvice(report.getAdvice());
        medicalCase.setAiIntroduction(report.getIntroduction());
        medicalCase.setCheckTime(report.getCheckTime() == null ? LocalDateTime.now() : report.getCheckTime());
        medicalCase.setCaseNo(genCaseNo(doctorId));
        medicalCaseMapper.insert(medicalCase);

        List<ConceptScore> conceptScores = report.getConceptScores() == null ? new ArrayList<>() : report.getConceptScores();
        if (!conceptScores.isEmpty()) {
            enrichConceptNames(conceptScores);
        }

        caseAnalysisMapper.insert(
                medicalCase.getId(),
                null,
                report.getDiseaseType(),
                inferConfidence(conceptScores),
                "skin-model-v1",
                toTopkIndicesJson(conceptScores),
                toTopkScoresJson(conceptScores),
                null
        );

        if (!conceptScores.isEmpty()) {
            caseConceptScoreMapper.batchInsert(medicalCase.getId(), conceptScores);
            for (ConceptScore score : conceptScores) {
                if (score.getConceptNameEn() != null || score.getConceptNameCn() != null) {
                    conceptDictionaryMapper.upsert(score.getConceptIndex(), score.getConceptNameEn(), score.getConceptNameCn());
                }
            }
        }

        persistCaseImage(medicalCase.getId(), report.getImageUrl());
        report.setId(medicalCase.getId());
        return 1;
    }

    @Override
    public int deleteReport(Long id) {
        Long doctorId = currentDoctorId();
        MedicalCase medicalCase = medicalCaseMapper.selectById(id, doctorId);
        if (medicalCase == null) {
            return 0;
        }
        CaseImage image = caseImageMapper.selectPrimaryByCaseId(id);
        int affected = medicalCaseMapper.deleteById(id, doctorId);
        if (affected > 0 && image != null && image.getObjectKey() != null && !image.getObjectKey().isBlank()) {
            try {
                ossService.deleteFile(image.getObjectKey());
            } catch (Exception ignored) {
            }
        }
        return affected;
    }

    @Override
    public Report getReportById(Long id) {
        MedicalCase medicalCase = medicalCaseMapper.selectById(id, currentDoctorId());
        if (medicalCase == null) {
            return null;
        }
        return toReport(medicalCase);
    }

    @Override
    public List<Report> getAllReports() {
        return medicalCaseMapper.selectByDoctor(currentDoctorId(), null, null, null)
                .stream()
                .map(this::toReport)
                .collect(Collectors.toList());
    }

    @Override
    public List<Report> findReportsByUsername(String username) {
        return medicalCaseMapper.selectByDoctor(currentDoctorId(), null, null, username)
                .stream()
                .map(this::toReport)
                .collect(Collectors.toList());
    }

    private Report toReport(MedicalCase medicalCase) {
        Report report = new Report();
        report.setId(medicalCase.getId());
        report.setUsername(medicalCase.getPatientName());
        report.setGender(null);
        report.setAge(null);
        report.setSymptoms(medicalCase.getChiefComplaint());
        report.setDuration(medicalCase.getDuration());
        report.setTreatment(medicalCase.getTreatmentHistory());
        report.setOther(medicalCase.getExtraNotes());
        report.setCheckTime(medicalCase.getCheckTime());
        report.setDiseaseType(medicalCase.getDiagnosedType());
        report.setAdvice(medicalCase.getAiAdvice());
        report.setIntroduction(medicalCase.getAiIntroduction());

        List<ConceptScore> conceptScores = caseConceptScoreMapper.selectByCaseId(medicalCase.getId());
        report.setConceptScores(conceptScores);
        report.setValue(toTopkScoresCsv(conceptScores));

        CaseImage image = caseImageMapper.selectPrimaryByCaseId(medicalCase.getId());
        if (image != null) {
            report.setImageUrl(image.getObjectKey() != null && !image.getObjectKey().isBlank()
                    ? image.getObjectKey()
                    : image.getPublicUrl());
        }
        return report;
    }

    private void persistCaseImage(Long caseId, String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        CaseImage image = new CaseImage();
        image.setCaseId(caseId);
        image.setPrimary(true);

        if (isRemoteUrl(imageUrl)) {
            image.setObjectKey(ossService.normalizeObjectKey(imageUrl));
            image.setPublicUrl(imageUrl);
        } else if (looksLikeObjectKey(imageUrl)) {
            image.setObjectKey(ossService.normalizeObjectKey(imageUrl));
            image.setPublicUrl(null);
        } else {
            image.setObjectKey("local/" + System.currentTimeMillis());
            image.setPublicUrl(imageUrl);
        }
        caseImageMapper.insert(image);
    }

    private boolean isRemoteUrl(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }

    private boolean looksLikeObjectKey(String value) {
        return !value.startsWith("wxfile://") && !value.startsWith("/") && !value.contains("://");
    }

    private void enrichConceptNames(List<ConceptScore> scores) {
        List<Integer> indices = scores.stream()
                .map(ConceptScore::getConceptIndex)
                .filter(i -> i != null)
                .distinct()
                .collect(Collectors.toList());
        if (indices.isEmpty()) {
            return;
        }

        Map<Integer, ConceptScore> dictionary = conceptDictionaryMapper.selectByIndices(indices).stream()
                .collect(Collectors.toMap(ConceptScore::getConceptIndex, x -> x, (a, b) -> a));

        int rank = 1;
        for (ConceptScore score : scores) {
            if (score.getRankNo() == null || score.getRankNo() <= 0) {
                score.setRankNo(rank);
            }
            rank++;
            if (score.getConceptIndex() == null) {
                continue;
            }
            ConceptScore ref = dictionary.get(score.getConceptIndex());
            if (ref == null) {
                continue;
            }
            if (score.getConceptNameEn() == null || score.getConceptNameEn().isBlank()) {
                score.setConceptNameEn(ref.getConceptNameEn());
            }
            if (score.getConceptNameCn() == null || score.getConceptNameCn().isBlank()) {
                score.setConceptNameCn(ref.getConceptNameCn());
            }
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

    private Patient findOrCreatePatient(Report report, Long doctorId) {
        String patientName = report.getUsername();
        if (patientName == null || patientName.isBlank()) {
            patientName = "Unknown Patient";
        }
        Patient existing = patientMapper.selectByDoctorAndName(doctorId, patientName);
        if (existing != null) {
            return existing;
        }
        Patient patient = new Patient();
        patient.setDoctorId(doctorId);
        patient.setPatientName(patientName);
        patient.setGender(report.getGender());
        patient.setAge(report.getAge());
        patient.setNotes("created from diagnosis flow");
        patientMapper.insert(patient);
        return patientMapper.selectById(patient.getId(), doctorId);
    }

    private String genCaseNo(Long doctorId) {
        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rand = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "CASE-" + doctorId + "-" + timePart + "-" + rand;
    }

    private Double inferConfidence(List<ConceptScore> conceptScores) {
        if (conceptScores == null || conceptScores.isEmpty() || conceptScores.get(0).getScore() == null) {
            return null;
        }
        return conceptScores.get(0).getScore();
    }

    private String toTopkIndicesJson(List<ConceptScore> conceptScores) {
        if (conceptScores == null || conceptScores.isEmpty()) {
            return "[]";
        }
        return conceptScores.stream()
                .map(s -> String.valueOf(s.getConceptIndex()))
                .collect(Collectors.joining(",", "[", "]"));
    }

    private String toTopkScoresJson(List<ConceptScore> conceptScores) {
        if (conceptScores == null || conceptScores.isEmpty()) {
            return "[]";
        }
        return conceptScores.stream()
                .map(s -> s.getScore() == null ? "0" : String.valueOf(s.getScore()))
                .collect(Collectors.joining(",", "[", "]"));
    }

    private String toTopkScoresCsv(List<ConceptScore> conceptScores) {
        if (conceptScores == null || conceptScores.isEmpty()) {
            return "";
        }
        return conceptScores.stream()
                .map(s -> s.getScore() == null ? "0" : String.valueOf(s.getScore()))
                .collect(Collectors.joining(","));
    }
}

