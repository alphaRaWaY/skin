package org.skinAI.controller;

import org.skinAI.pojo.Result;
import org.skinAI.pojo.report.Report;
import org.skinAI.services.DeepSeekService;
import org.skinAI.services.ReportService;
import org.skinAI.utils.TestAnalyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private DeepSeekService deepSeekService;

    @Autowired
    private ReportService reportService;

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
    public Result<Report> getAllReports(@RequestBody Report report) {
        System.out.println(report);
//        Report report1 = ReportAnalyzer.analyzeReport(report);
        Report report1 = TestAnalyzer.analyzeReport(report);
        String advice = deepSeekService.getAdvice("以下是一个患者的疾病类型，请返回一个简短的建议，请不要使用markdown语法，而是使用简单的字符串："+report1.toString());
        String introduction = deepSeekService.getAdvice("以下是一个患者的疾病类型，请返回一个他的疾病的简单介绍，请不要使用markdown语法，而是使用简单的字符串"+report1.toString());
        report1.setAdvice(advice);
        report1.setIntroduction(introduction);
        return Result.success(report1);
    }
}
