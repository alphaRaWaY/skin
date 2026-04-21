// ReportService.java
package org.skinAI.services.Impl;

import org.skinAI.mapper.ReportMapper;
import org.skinAI.pojo.report.ChildReport;
import org.skinAI.pojo.report.Report;
import org.skinAI.services.OssService;
import org.skinAI.services.ReportService;
import org.skinAI.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private ReportMapper reportMapper;
    @Autowired
    private OssService ossService;

    @Override
    public int addReport(Report report) {

        Map<String,Object> map = ThreadLocalUtil.get();
        Integer userid = (Integer) map.get("userid");
        ChildReport childReport = new ChildReport(report,userid);

        return reportMapper.insertReport(childReport);
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
        return reportMapper.selectReportById(id,userid);
    }
    @Override
    public List<Report> getAllReports() {
        Map<String,Object> map = ThreadLocalUtil.get();
        Integer userid = (Integer) map.get("userid");
        return reportMapper.selectAllReports(userid);
    }

    @Override
    public List<Report> findReportsByUsername(String username) {
        Map<String,Object> map = ThreadLocalUtil.get();
        Integer userid = (Integer) map.get("userid");
        return reportMapper.selectReportsByUsername(username,userid);
    }
}
