package org.skinAI.services;

import org.skinAI.pojo.report.Report;

import java.util.List;

public interface ReportService {
    int addReport(Report report);

    int deleteReport(Long id);

    Report getReportById(Long id);

    List<Report> getAllReports();

    List<Report> findReportsByUsername(String username);
}
