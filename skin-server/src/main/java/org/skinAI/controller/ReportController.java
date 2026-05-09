package org.skinAI.controller;

import org.skinAI.client.AiServiceClient;
import org.skinAI.analyzer.ReportAnalyzer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.skinAI.pojo.Result;
import org.skinAI.pojo.report.Report;
import org.skinAI.services.ReportService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final AiServiceClient aiServiceClient;
    private final ReportService reportService;
    private final ReportAnalyzer reportAnalyzer;
    private final ObjectMapper objectMapper;

    public ReportController(
            AiServiceClient aiServiceClient,
            ReportService reportService,
            ReportAnalyzer reportAnalyzer,
            ObjectMapper objectMapper
    ) {
        this.aiServiceClient = aiServiceClient;
        this.reportService = reportService;
        this.reportAnalyzer = reportAnalyzer;
        this.objectMapper = objectMapper;
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
        String advicePrompt = String.format(
                "Generate concise treatment suggestions for a mock skin report. " +
                        "diseaseType=%s; symptoms=%s; age=%s; gender=%s; value=%s. " +
                        "Do not ask user for more fields. Return plain Chinese.",
                safe(analyzed.getDiseaseType()),
                safe(analyzed.getSymptoms()),
                String.valueOf(analyzed.getAge()),
                safe(analyzed.getGender()),
                safe(analyzed.getValue())
        );
        String introPrompt = String.format(
                "Generate a concise disease introduction in Chinese for diseaseType=%s. " +
                        "Use value=%s as mock severity reference. Do not ask follow-up questions.",
                safe(analyzed.getDiseaseType()),
                safe(analyzed.getValue())
        );

        String advice = aiServiceClient.getAdvice(advicePrompt);
        String introduction = aiServiceClient.getAdvice(introPrompt);
        analyzed.setAdvice(advice);
        analyzed.setIntroduction(introduction);
        return Result.success(analyzed);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
