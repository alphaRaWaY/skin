package org.skinAI.utils;

import org.skinAI.pojo.report.Report;

import java.util.Random;

public class TestAnalyzer {

    private static final Random RANDOM = new Random();
    private static final String[] DISEASE_TYPES = {"ACNE", "ECZEMA", "PSORIASIS"};

    public static Report analyzeReport(Report inputReport) {
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

        String diseaseType = pickDiseaseType(inputReport.getDuration());
        output.setDiseaseType(diseaseType);
        output.setValue(buildMockValue(diseaseType));

        output.setAdvice("");
        output.setIntroduction("");
        return output;
    }

    private static String pickDiseaseType(String duration) {
        if (duration == null) {
            return DISEASE_TYPES[RANDOM.nextInt(DISEASE_TYPES.length)];
        }
        switch (duration.trim()) {
            case "2周":
                return "ACNE";
            case "3周":
                return "ECZEMA";
            case "4周":
                return "PSORIASIS";
            default:
                return DISEASE_TYPES[RANDOM.nextInt(DISEASE_TYPES.length)];
        }
    }

    private static String buildMockValue(String diseaseType) {
        switch (diseaseType) {
            case "ACNE":
                return String.format("%.1f,%.1f,%.1f,%.1f",
                        RANDOM.nextDouble() * 4,
                        RANDOM.nextDouble() * 6,
                        RANDOM.nextDouble() * 5,
                        RANDOM.nextDouble() * 4);
            case "ECZEMA":
                return String.format("%.1f,%.1f,%.1f,%.1f",
                        RANDOM.nextDouble() * 4,
                        RANDOM.nextDouble() * 4,
                        RANDOM.nextDouble() * 4,
                        RANDOM.nextDouble() * 4);
            case "PSORIASIS":
                return String.format("%.1f,%.1f,%.1f,%.1f",
                        RANDOM.nextDouble() * 7,
                        RANDOM.nextDouble() * 5,
                        RANDOM.nextDouble() * 5,
                        RANDOM.nextDouble() * 5);
            default:
                return "3.2,3.0,2.6,4.1";
        }
    }
}
