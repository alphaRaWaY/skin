// ReportService.java
package org.skinAI.services.Impl;

import org.skinAI.mapper.ReportMapper;
import org.skinAI.mapper.ReportConceptMapper;
import org.skinAI.mapper.ConceptDictionaryMapper;
import org.skinAI.pojo.report.ChildReport;
import org.skinAI.pojo.report.ConceptScore;
import org.skinAI.pojo.report.Report;
import org.skinAI.services.OssService;
import org.skinAI.services.ReportService;
import org.skinAI.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private ReportMapper reportMapper;
    @Autowired
    private ReportConceptMapper reportConceptMapper;
    @Autowired
    private ConceptDictionaryMapper conceptDictionaryMapper;
    @Autowired
    private OssService ossService;

    @Override
    public int addReport(Report report) {

        Map<String,Object> map = ThreadLocalUtil.get();
        Integer userid = (Integer) map.get("userid");
        ChildReport childReport = new ChildReport(report,userid);

        int affected = reportMapper.insertReport(childReport);
        if (affected > 0 && childReport.getId() != null && report.getConceptScores() != null && !report.getConceptScores().isEmpty()) {
            enrichConceptNames(report.getConceptScores());
            reportConceptMapper.batchInsert(childReport.getId(), report.getConceptScores());
            for (ConceptScore score : report.getConceptScores()) {
                if (score.getConceptNameEn() != null || score.getConceptNameCn() != null) {
                    conceptDictionaryMapper.upsert(score.getConceptIndex(), score.getConceptNameEn(), score.getConceptNameCn());
                }
            }
        }
        return affected;
    }
    @Override
    public int deleteReport(Long id) {
        Map<String,Object> map = ThreadLocalUtil.get();
        Integer userid = (Integer) map.get("userid");
        Report report = reportMapper.selectReportById(id, userid);
        int affected = reportMapper.deleteReportById(id, userid);

        if (affected > 0 && report != null && report.getImageUrl() != null && !report.getImageUrl().isBlank()) {
            try {
                ossService.deleteFile(report.getImageUrl());
            } catch (Exception ignored) {
                // keep DB deletion result as success; object cleanup can be retried manually if needed
            }
        }
        return affected;
    }
    @Override
    public Report getReportById(Long id) {
        Map<String,Object> map = ThreadLocalUtil.get();
        Integer userid = (Integer) map.get("userid");
        Report report = reportMapper.selectReportById(id,userid);
        attachConceptScores(report);
        return report;
    }
    @Override
    public List<Report> getAllReports() {
        Map<String,Object> map = ThreadLocalUtil.get();
        Integer userid = (Integer) map.get("userid");
        List<Report> reports = reportMapper.selectAllReports(userid);
        reports.forEach(this::attachConceptScores);
        return reports;
    }

    @Override
    public List<Report> findReportsByUsername(String username) {
        Map<String,Object> map = ThreadLocalUtil.get();
        Integer userid = (Integer) map.get("userid");
        List<Report> reports = reportMapper.selectReportsByUsername(username,userid);
        reports.forEach(this::attachConceptScores);
        return reports;
    }

    private void attachConceptScores(Report report) {
        if (report == null || report.getId() == null) {
            return;
        }
        report.setConceptScores(reportConceptMapper.selectByReportId(report.getId()));
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

        for (ConceptScore score : scores) {
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
}
