package org.skinAI.pojo.medical;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CaseFollowup {
    private Long id;
    private Long caseId;
    private LocalDateTime followupTime;
    private String summary;
    private String nextPlan;
    private LocalDateTime createdAt;
}
