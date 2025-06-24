// ReportService.java
package org.mypetstore.mypetstore.services.Impl;

import org.mypetstore.mypetstore.mapper.ReportMapper;
import org.mypetstore.mypetstore.pojo.report.ChildReport;
import org.mypetstore.mypetstore.pojo.report.Report;
import org.mypetstore.mypetstore.services.ReportService;
import org.mypetstore.mypetstore.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private ReportMapper reportMapper;

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
        return reportMapper.deleteReportById(id,userid);
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
