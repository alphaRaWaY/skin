package org.skinAI.controller;

import org.skinAI.client.AiServiceClient;
import org.skinAI.analyzer.ReportAnalyzer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.skinAI.mapper.ConceptDictionaryMapper;
import org.skinAI.pojo.Result;
import org.skinAI.pojo.report.ConceptScore;
import org.skinAI.pojo.report.Report;
import org.skinAI.services.ReportService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final AiServiceClient aiServiceClient;
    private final ReportService reportService;
    private final ReportAnalyzer reportAnalyzer;
    private final ObjectMapper objectMapper;
    private final ConceptDictionaryMapper conceptDictionaryMapper;
    @Value("${services.analyzer.mode:mock}")
    private String analyzerMode;

    public ReportController(
            AiServiceClient aiServiceClient,
            ReportService reportService,
            ReportAnalyzer reportAnalyzer,
            ObjectMapper objectMapper,
            ConceptDictionaryMapper conceptDictionaryMapper
    ) {
        this.aiServiceClient = aiServiceClient;
        this.reportService = reportService;
        this.reportAnalyzer = reportAnalyzer;
        this.objectMapper = objectMapper;
        this.conceptDictionaryMapper = conceptDictionaryMapper;
    }

    @PostMapping
    public Result createReport(@RequestBody Report report) {
        reportService.addReport(report);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result deleteReport(@PathVariable Long id) {
        int affected = reportService.deleteReport(id);
        if (affected > 0) {
            return Result.success();
        }
        return Result.error("report not found");
    }

    @GetMapping("/{id}")
    public Result<Report> getReport(@PathVariable Long id) {
        return Result.success(reportService.getReportById(id));
    }

    @GetMapping("/search/{username}")
    public Result<List<Report>> getFamilyByUsername(@PathVariable String username) {
        return Result.success(reportService.findReportsByUsername(username));
    }

    @GetMapping
    public Result<List<Report>> getAllReports() {
        return Result.success(reportService.getAllReports());
    }

    @PostMapping("/analys")
    public Result<Report> analyzeReport(@RequestBody Report report) {
        if ("python".equalsIgnoreCase(analyzerMode)) {
            return Result.error("python analyzer requires file upload, please call /api/reports/analys-upload");
        }
        Report analyzed = reportAnalyzer.analyze(report);
        return enrichAiContent(analyzed);
    }

    @PostMapping("/analys-upload")
    public Result<Report> analyzeReportWithImage(
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

        String advice = aiServiceClient.getAdvice(advicePrompt);
        String introduction = aiServiceClient.getAdvice(introPrompt);
        analyzed.setAdvice(advice);
        analyzed.setIntroduction(introduction);
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
