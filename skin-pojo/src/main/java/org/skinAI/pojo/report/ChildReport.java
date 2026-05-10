package org.skinAI.pojo.report;

import lombok.Data;

@Data
public class ChildReport extends Report {
    private int userid;

    public ChildReport(Report report, int userid) {
        // 复制父类属性
        this.setId(report.getId());
        this.setUsername(report.getUsername());
        this.setGender(report.getGender());
        this.setAge(report.getAge());
        this.setSymptoms(report.getSymptoms());
        this.setDuration(report.getDuration());
        this.setTreatment(report.getTreatment());
        this.setOther(report.getOther());
        this.setCheckTime(report.getCheckTime());
        this.setImageUrl(report.getImageUrl());
        this.setDiseaseType(report.getDiseaseType());
        this.setValue(report.getValue());
        this.setAdvice(report.getAdvice());
        this.setIntroduction(report.getIntroduction());
        this.setConceptScores(report.getConceptScores());

        // 设置子类特有属性
        this.userid = userid;
    }
}
