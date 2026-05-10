package org.skinAI.analyzer;

import org.skinAI.pojo.report.Report;
import org.skinAI.pojo.report.ConceptScore;
import org.skinAI.services.OssService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "services.analyzer", name = "mode", havingValue = "python")
public class PythonReportAnalyzer implements ReportAnalyzer {

    private final RestTemplate restTemplate;
    private final OssService ossService;

    @Value("${services.model.base-url:http://127.0.0.1:5000}")
    private String modelBaseUrl;

    @Value("${services.model.predict-path:/api/v1/predict}")
    private String predictPath;

    public PythonReportAnalyzer(RestTemplate restTemplate, OssService ossService) {
        this.restTemplate = restTemplate;
        this.ossService = ossService;
    }

    @Override
    public Report analyze(Report inputReport) {
        return analyze(inputReport, null);
    }

    @Override
    public Report analyze(Report inputReport, MultipartFile imageFile) {
        if (imageFile != null && !imageFile.isEmpty()) {
            try (InputStream in = imageFile.getInputStream()) {
                return analyzeByStream(inputReport, in, imageFile.getOriginalFilename());
            } catch (Exception ex) {
                throw new RuntimeException("python analyzer call failed: " + ex.getMessage(), ex);
            }
        }

        if (inputReport == null || inputReport.getImageUrl() == null || inputReport.getImageUrl().isBlank()) {
            throw new IllegalArgumentException("image file or imageUrl is required for python analyzer");
        }

        String objectKey = ossService.normalizeObjectKey(inputReport.getImageUrl());
        try (InputStream in = ossService.getObjectStream(objectKey)) {
            return analyzeByStream(inputReport, in, extractFileName(objectKey));
        } catch (Exception ex) {
            throw new RuntimeException("python analyzer call failed: " + ex.getMessage(), ex);
        }
    }

    private Report analyzeByStream(Report inputReport, InputStream in, String filename) {
        HttpEntity<InputStreamResource> fileEntity = buildFileEntity(in, filename);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", fileEntity);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(
                modelBaseUrl + predictPath, request, Map.class);

        if (response == null) {
            throw new RuntimeException("model response is null");
        }

        Number code = (Number) response.get("code");
        if (code == null || code.intValue() != 200) {
            throw new RuntimeException("model response invalid: " + response);
        }

        Object dataObj = response.get("data");
        if (!(dataObj instanceof Map<?, ?> dataMap)) {
            throw new RuntimeException("model response data missing: " + response);
        }

        Report output = copyBaseFields(inputReport);
        Object diseaseTypeObj = dataMap.get("diseaseType");
        output.setDiseaseType(diseaseTypeObj == null ? "unknown" : String.valueOf(diseaseTypeObj));
        output.setConceptScores(parseConceptScores(dataMap));
        output.setValue(buildValueFromModelData(dataMap));
        output.setAdvice("");
        output.setIntroduction("");
        return output;
    }

    private HttpEntity<InputStreamResource> buildFileEntity(InputStream in, String filename) {
        InputStreamResource resource = new InputStreamResource(in) {
            @Override
            public String getFilename() {
                return filename;
            }

            @Override
            public long contentLength() {
                return -1;
            }
        };

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return new HttpEntity<>(resource, fileHeaders);
    }

    private Report copyBaseFields(Report inputReport) {
        Report output = new Report();
        output.setId(inputReport.getId());
        output.setUsername(inputReport.getUsername());
        output.setGender(inputReport.getGender());
        output.setAge(inputReport.getAge());
        output.setSymptoms(inputReport.getSymptoms());
        output.setDuration(inputReport.getDuration());
        output.setTreatment(inputReport.getTreatment());
        output.setOther(inputReport.getOther());
        output.setCheckTime(inputReport.getCheckTime());
        output.setImageUrl(inputReport.getImageUrl());
        return output;
    }

    private String buildValueFromModelData(Map<?, ?> dataMap) {
        Object confidenceObj = dataMap.get("confidence");
        double confidence = confidenceObj instanceof Number n ? n.doubleValue() : 0.0;

        Object topScoresObj = dataMap.get("topKScores");
        if (topScoresObj instanceof List<?> scores && !scores.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            builder.append("confidence=").append(String.format("%.4f", confidence)).append(";topScores=");
            int limit = Math.min(scores.size(), 4);
            for (int i = 0; i < limit; i++) {
                Object scoreObj = scores.get(i);
                double score = scoreObj instanceof Number n ? n.doubleValue() : 0.0;
                if (i > 0) {
                    builder.append(",");
                }
                builder.append(String.format("%.4f", score));
            }
            return builder.toString();
        }

        return "confidence=" + String.format("%.4f", confidence);
    }

    private List<ConceptScore> parseConceptScores(Map<?, ?> dataMap) {
        Object topIndicesObj = dataMap.get("topKIndices");
        Object topScoresObj = dataMap.get("topKScores");
        if (!(topIndicesObj instanceof List<?> indices) || !(topScoresObj instanceof List<?> scores)) {
            return List.of();
        }

        List<ConceptScore> result = new ArrayList<>();
        int size = Math.min(indices.size(), scores.size());
        for (int i = 0; i < size; i++) {
            Object idxObj = indices.get(i);
            Object scoreObj = scores.get(i);
            if (!(idxObj instanceof Number idxNumber) || !(scoreObj instanceof Number scoreNumber)) {
                continue;
            }

            ConceptScore item = new ConceptScore();
            item.setConceptIndex(idxNumber.intValue());
            item.setScore(scoreNumber.doubleValue());
            item.setRankNo(i + 1);

            result.add(item);
        }
        return result;
    }

    private String extractFileName(String objectKey) {
        int lastSlash = objectKey.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < objectKey.length() - 1) {
            return objectKey.substring(lastSlash + 1);
        }
        return "image.jpg";
    }
}
