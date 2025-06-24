package org.mypetstore.mypetstore.controller;

import org.mypetstore.mypetstore.pojo.Result;
import org.mypetstore.mypetstore.pojo.family.Family;
import org.mypetstore.mypetstore.pojo.report.Report;
import org.mypetstore.mypetstore.services.DeepSeekService;
import org.mypetstore.mypetstore.services.ReportService;
import org.mypetstore.mypetstore.utils.ReportAnalyzer;
import org.mypetstore.mypetstore.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
        Report report1 = ReportAnalyzer.analyzeReport(report);
        String advice = deepSeekService.getChat("以下是一个新的患者的疾病类型，请返回一个简短的建议，请不要使用markdown语法，而是使用简单的字符串："+report1.toString(),"自动问诊记录");
        String introduction = deepSeekService.getChat("以下是一个新的患者的疾病类型，请返回一个他的疾病的简单介绍，请不要使用markdown语法，而是使用简单的字符串"+report1.toString(),"自动问诊记录");
        report1.setAdvice(advice);
        report1.setIntroduction(introduction);
        return Result.success(report1);
    }
}
