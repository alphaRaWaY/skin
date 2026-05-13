package org.skinAI.pojo.medical;

import lombok.Data;

@Data
public class DashboardSummary {
    private Integer todayDiagnosed;
    private Integer pendingCases;
    private Integer followupCases;
    private Integer historyCases;
}

