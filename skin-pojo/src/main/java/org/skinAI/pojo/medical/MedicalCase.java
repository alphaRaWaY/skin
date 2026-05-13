package org.skinAI.pojo.medical;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MedicalCase {
    private Long id;
    private Long doctorId;
    private Long patientId;
    private String caseNo;
    private String status;
    private String chiefComplaint;
    private String presentHistory;
    private String treatmentHistory;
    private String duration;
    private String extraNotes;
    private String diagnosedType;
    private String aiAdvice;
    private String aiIntroduction;
    private LocalDateTime checkTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // joined field
    private String patientName;
}

