package org.skinAI.pojo.report;

import lombok.Data;

@Data
public class ConceptScore {
    private Integer conceptIndex;
    private String conceptNameEn;
    private String conceptNameCn;
    private Double score;
    private Integer rankNo;
}

