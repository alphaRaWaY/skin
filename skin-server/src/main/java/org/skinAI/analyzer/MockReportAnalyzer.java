package org.skinAI.analyzer;

import org.skinAI.pojo.report.Report;
import org.skinAI.utils.TestAnalyzer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "services.analyzer", name = "mode", havingValue = "mock", matchIfMissing = true)
public class MockReportAnalyzer implements ReportAnalyzer {
    @Override
    public Report analyze(Report inputReport) {
        return TestAnalyzer.analyzeReport(inputReport);
    }
}

