package org.skinAI.controller;

import org.skinAI.client.AiServiceClient;
import org.skinAI.pojo.Result;
import org.skinAI.pojo.report.Report;
import org.skinAI.services.ReportService;
import org.skinAI.utils.TestAnalyzer;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final AiServiceClient aiServiceClient;
    private final ReportService reportService;

    public ReportController(AiServiceClient aiServiceClient, ReportService reportService) {
        this.aiServiceClient = aiServiceClient;
        this.reportService = reportService;
    }

    @PostMapping
    public Result createReport(@RequestBody Report report) {
        reportService.addReport(report);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result deleteReport(@PathVariable Long id) {
        reportService.deleteReport(id);
        return Result.success();
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
        Report analyzed = TestAnalyzer.analyzeReport(report);
        String advice = aiServiceClient.getAdvice("Generate treatment suggestions for this report: " + analyzed);
        String introduction = aiServiceClient.getAdvice("Generate disease introduction for this report: " + analyzed);
        analyzed.setAdvice(advice);
        analyzed.setIntroduction(introduction);
        return Result.success(analyzed);
    }
}
