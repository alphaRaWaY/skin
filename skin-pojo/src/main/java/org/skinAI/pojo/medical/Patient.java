package org.skinAI.pojo.medical;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Patient {
    private Long id;
    private Long doctorId;
    private String patientName;
    private String gender;
    private Integer age;
    private String phone;
    private String idCardMasked;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

