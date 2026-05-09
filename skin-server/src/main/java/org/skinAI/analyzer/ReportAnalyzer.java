package org.skinAI.analyzer;

import org.skinAI.pojo.report.Report;
import org.springframework.web.multipart.MultipartFile;

public interface ReportAnalyzer {
    Report analyze(Report inputReport);

    default Report analyze(Report inputReport, MultipartFile imageFile) {
        return analyze(inputReport);
    }
}
