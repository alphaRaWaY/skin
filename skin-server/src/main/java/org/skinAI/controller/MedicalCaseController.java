package org.skinAI.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.skinAI.analyzer.ReportAnalyzer;
import org.skinAI.client.AiServiceClient;
import org.skinAI.mapper.ConceptDictionaryMapper;
import org.skinAI.pojo.Result;
import org.skinAI.pojo.medical.CaseFollowup;
import org.skinAI.pojo.medical.MedicalCase;
import org.skinAI.pojo.report.ConceptScore;
import org.skinAI.pojo.report.Report;
import org.skinAI.services.MedicalCaseService;
import org.skinAI.services.ReportService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cases")
public class MedicalCaseController {

    private final MedicalCaseService medicalCaseService;
    private final ReportService reportService;
    private final ReportAnalyzer reportAnalyzer;
    private final AiServiceClient aiServiceClient;
    private final ConceptDictionaryMapper conceptDictionaryMapper;
    private final ObjectMapper objectMapper;

    public MedicalCaseController(
            MedicalCaseService medicalCaseService,
            ReportService reportService,
            ReportAnalyzer reportAnalyzer,
            AiServiceClient aiServiceClient,
            ConceptDictionaryMapper conceptDictionaryMapper,
            ObjectMapper objectMapper
    ) {
        this.medicalCaseService = medicalCaseService;
        this.reportService = reportService;
        this.reportAnalyzer = reportAnalyzer;
        this.aiServiceClient = aiServiceClient;
        this.conceptDictionaryMapper = conceptDictionaryMapper;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public Result<MedicalCase> create(@RequestBody MedicalCase medicalCase) {
        return Result.success(medicalCaseService.create(medicalCase));
    }

    @PostMapping("/save")
    public Result saveDiagnosisResult(@RequestBody Report report) {
        reportService.addReport(report);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<MedicalCase> update(@PathVariable("id") Long id, @RequestBody MedicalCase medicalCase) {
        medicalCase.setId(id);
        MedicalCase updated = medicalCaseService.update(medicalCase);
        if (updated == null) {
            return Result.error("case not found");
        }
        return Result.success(updated);
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable("id") Long id) {
        int affected = medicalCaseService.deleteById(id);
        if (affected > 0) {
            return Result.success();
        }
        return Result.error("case not found");
    }

    @GetMapping("/{id}")
    public Result<MedicalCase> getById(@PathVariable("id") Long id) {
        MedicalCase medicalCase = medicalCaseService.getById(id);
        if (medicalCase == null) {
            return Result.error("case not found");
        }
        return Result.success(medicalCase);
    }

    @GetMapping
    public Result<List<MedicalCase>> list(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "patientId", required = false) Long patientId,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        return Result.success(medicalCaseService.list(status, patientId, keyword));
    }

    @GetMapping("/search/{username}")
    public Result<List<Report>> searchByUsername(@PathVariable("username") String username) {
        return Result.success(reportService.findReportsByUsername(username));
    }

    @PostMapping("/analyze")
    public Result<Report> analyze(@RequestBody Report report) {
        Report analyzed = reportAnalyzer.analyze(report);
        return enrichAiContent(analyzed);
    }

    @PostMapping("/analyze-upload")
    public Result<Report> analyzeUpload(
            @RequestParam("reportJson") String reportJson,
            @RequestParam("image") MultipartFile image
    ) {
        try {
            Report report = objectMapper.readValue(reportJson, Report.class);
            Report analyzed = reportAnalyzer.analyze(report, image);
            return enrichAiContent(analyzed);
        } catch (Exception ex) {
            return Result.error("analyze failed: " + ex.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    public Result updateStatus(@PathVariable("id") Long id, @RequestBody Map<String, String> req) {
        String status = req.get("status");
        if (status == null || status.isBlank()) {
            return Result.error("status is required");
        }
        int affected = medicalCaseService.updateStatus(id, status);
        if (affected > 0) {
            return Result.success();
        }
        return Result.error("case not found");
    }

    @GetMapping("/{id}/followups")
    public Result<List<CaseFollowup>> followups(@PathVariable("id") Long id) {
        return Result.success(medicalCaseService.listFollowups(id));
    }

    @PostMapping("/{id}/followups")
    public Result<CaseFollowup> addFollowup(@PathVariable("id") Long id, @RequestBody CaseFollowup followup) {
        return Result.success(medicalCaseService.addFollowup(id, followup));
    }

    private Result<Report> enrichAiContent(Report analyzed) {
        fillConceptNames(analyzed);
        String advicePrompt = String.format(
                "Generate concise treatment suggestions for a mock skin report. " +
                        "diseaseType=%s; symptoms=%s; age=%s; gender=%s; value=%s. " +
                        "Do not ask user for more fields. Return plain Chinese text only. " +
                        "Do NOT use markdown, headings, bullet points, asterisks, or numbering syntax.",
                safe(analyzed.getDiseaseType()),
                safe(analyzed.getSymptoms()),
                String.valueOf(analyzed.getAge()),
                safe(analyzed.getGender()),
                safe(analyzed.getValue())
        );
        String introPrompt = String.format(
                "Generate a concise disease introduction in Chinese for diseaseType=%s. " +
                        "Use value=%s as mock severity reference. Return plain Chinese text only. " +
                        "Do NOT use markdown, headings, bullet points, asterisks, or numbering syntax. " +
                        "Do not ask follow-up questions.",
                safe(analyzed.getDiseaseType()),
                safe(analyzed.getValue())
        );

        analyzed.setAdvice(aiServiceClient.getAdvice(advicePrompt));
        analyzed.setIntroduction(aiServiceClient.getAdvice(introPrompt));
        return Result.success(analyzed);
    }

    private void fillConceptNames(Report report) {
        if (report == null || report.getConceptScores() == null || report.getConceptScores().isEmpty()) {
            return;
        }
        List<Integer> indices = report.getConceptScores().stream()
                .map(ConceptScore::getConceptIndex)
                .filter(i -> i != null)
                .distinct()
                .collect(Collectors.toList());
        if (indices.isEmpty()) {
            return;
        }
        List<ConceptScore> dictionaryList = conceptDictionaryMapper.selectByIndices(indices);
        Map<Integer, ConceptScore> dict = new HashMap<>();
        for (ConceptScore d : dictionaryList) {
            if (d.getConceptIndex() != null) {
                dict.put(d.getConceptIndex(), d);
            }
        }
        for (ConceptScore item : report.getConceptScores()) {
            if (item.getConceptIndex() == null) {
                continue;
            }
            ConceptScore d = dict.get(item.getConceptIndex());
            if (d == null) {
                continue;
            }
            if (item.getConceptNameCn() == null || item.getConceptNameCn().isBlank()) {
                item.setConceptNameCn(d.getConceptNameCn());
            }
            if (item.getConceptNameEn() == null || item.getConceptNameEn().isBlank()) {
                item.setConceptNameEn(d.getConceptNameEn());
            }
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
