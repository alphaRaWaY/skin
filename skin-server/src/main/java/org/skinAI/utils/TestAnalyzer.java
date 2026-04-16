package org.skinAI.utils;

import org.skinAI.pojo.report.Report;

import java.util.Random;

public class TestAnalyzer {

    private static final Random random = new Random();

    public static Report analyzeReport(Report inputReport) {
        Report outputReport = new Report();
        // 保留原始输入信息
        outputReport.setId(inputReport.getId());
        outputReport.setUsername(inputReport.getUsername());
        outputReport.setGender(inputReport.getGender());
        outputReport.setAge(inputReport.getAge());
        outputReport.setSymptoms(inputReport.getSymptoms());
        outputReport.setDuration(inputReport.getDuration());
        outputReport.setTreatment(inputReport.getTreatment());
        outputReport.setOther(inputReport.getOther());
        outputReport.setCheckTime(inputReport.getCheckTime());
        outputReport.setImageUrl(inputReport.getImageUrl());


        // 随机选择疾病类型
        String[] diseaseTypes = {"瘢痕", "特应性皮炎", "银屑病"};


        switch (inputReport.getDuration())
        {
            case "2天":
                outputReport.setDiseaseType(diseaseTypes[0]);
                break;
            case "3天":
                outputReport.setDiseaseType(diseaseTypes[1]);
                break;
            case "4天":
                outputReport.setDiseaseType(diseaseTypes[2]);
                break;
            default:
                outputReport.setDiseaseType(diseaseTypes[random.nextInt(diseaseTypes.length)]);
                break;
        }
        String diseaseType = inputReport.getDiseaseType();
        // 根据疾病类型生成合理的评分值
        String value;
        switch (diseaseType) {
            case "瘢痕":
                // VSS评分: 色素沉着(0-3)、高度(0-5)、血管分布(0-4)、柔软度(0-3)
                value = String.format("%.1f,%.1f,%.1f,%.1f",
                        random.nextDouble(4),    // 0-3
                        random.nextDouble(6),    // 0-5
                        random.nextDouble(5),   // 0-4
                        random.nextDouble(4));   // 0-3
                break;
            case "特应性皮炎":
                // EASI评分: 红斑(0-3)、浸润(0-3)、脱屑(0-3)、苔藓样变(0-3)
                value = String.format("%.1f,%.1f,%.1f,%.1f",
                        random.nextDouble(4),    // 0-3
                        random.nextDouble(4),   // 0-3
                        random.nextDouble(4),    // 0-3
                        random.nextDouble(4));   // 0-3
                break;
            case "银屑病":
                // PASI评分: 皮肤面积(0-6)、红斑(0-4)、脱屑(0-4)、硬化(0-4)
                value = String.format("%.1f,%.1f,%.1f,%.1f",
                        random.nextDouble(7),    // 0-6
                        random.nextDouble(5),   // 0-4
                        random.nextDouble(5),    // 0-4
                        random.nextDouble(5));  // 0-4
                break;
            default:
                value = "3.2,3.0,2.6,4.1";
        }
        outputReport.setValue(value);

        // 清空建议和介绍（根据要求）
        outputReport.setAdvice("");
        outputReport.setIntroduction("");

        return outputReport;
    }
}