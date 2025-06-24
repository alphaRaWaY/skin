package org.mypetstore.mypetstore.utils;

import org.mypetstore.mypetstore.pojo.report.Report;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class ReportAnalyzer {

    private static final String ANALYZE_API_URL = "http://localhost:5000/ana";
    public static Report analyzeReport(Report inputReport) {
        RestTemplate restTemplate = new RestTemplate();

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 封装请求实体
        HttpEntity<Report> requestEntity = new HttpEntity<>(inputReport, headers);

        try {
            // 发送 POST 请求到远程分析服务
            ResponseEntity<Report> response = restTemplate.exchange(
                    ANALYZE_API_URL,
                    HttpMethod.POST,
                    requestEntity,
                    Report.class
            );

            // 返回服务器返回的结果
            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                System.err.println("分析服务返回非200状态：" + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("调用远程分析服务失败：" + e.getMessage());
        }

        // 失败情况返回原始输入（或抛出异常 / 返回 null 也可以）
        return inputReport;
    }
}
