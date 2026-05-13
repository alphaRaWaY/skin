package org.skinAI.pojo.medical;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CaseImage {
    private Long id;
    private Long caseId;
    private String objectKey;
    private String publicUrl;
    private Boolean primary;
    private LocalDateTime createdAt;
}
