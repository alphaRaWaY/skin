package org.skinAI.mapper;

import org.apache.ibatis.annotations.*;

import java.util.Map;

@Mapper
public interface CaseAnalysisMapper {

    @Insert("""
        INSERT INTO case_analysis (
          case_id, disease_index, disease_type, confidence, model_version, topk_indices_json, topk_scores_json, raw_response_json
        ) VALUES (
          #{caseId}, #{diseaseIndex}, #{diseaseType}, #{confidence}, #{modelVersion}, #{topkIndicesJson}, #{topkScoresJson}, #{rawResponseJson}
        )
        """)
    int insert(
            @Param("caseId") Long caseId,
            @Param("diseaseIndex") Integer diseaseIndex,
            @Param("diseaseType") String diseaseType,
            @Param("confidence") Double confidence,
            @Param("modelVersion") String modelVersion,
            @Param("topkIndicesJson") String topkIndicesJson,
            @Param("topkScoresJson") String topkScoresJson,
            @Param("rawResponseJson") String rawResponseJson
    );

    @Select("""
        SELECT case_id AS caseId, disease_index AS diseaseIndex, disease_type AS diseaseType,
               confidence, model_version AS modelVersion,
               topk_indices_json AS topkIndicesJson, topk_scores_json AS topkScoresJson,
               raw_response_json AS rawResponseJson
        FROM case_analysis
        WHERE case_id = #{caseId}
        ORDER BY id DESC
        LIMIT 1
        """)
    Map<String, Object> selectLatestByCaseId(@Param("caseId") Long caseId);
}
