package org.skinAI.mapper;

import org.apache.ibatis.annotations.*;
import org.skinAI.pojo.report.ConceptScore;

import java.util.List;

@Mapper
public interface ReportConceptMapper {

    @Insert("""
        <script>
        INSERT INTO report_concept_score (report_id, concept_index, concept_name_en, concept_name_cn, score, rank_no)
        VALUES
        <foreach collection="scores" item="s" separator=",">
            (#{reportId}, #{s.conceptIndex}, #{s.conceptNameEn}, #{s.conceptNameCn}, #{s.score}, #{s.rankNo})
        </foreach>
        </script>
    """)
    int batchInsert(@Param("reportId") Long reportId, @Param("scores") List<ConceptScore> scores);

    @Select("""
        SELECT concept_index AS conceptIndex,
               concept_name_en AS conceptNameEn,
               concept_name_cn AS conceptNameCn,
               score,
               rank_no AS rankNo
        FROM report_concept_score
        WHERE report_id = #{reportId}
        ORDER BY rank_no ASC
    """)
    List<ConceptScore> selectByReportId(@Param("reportId") Long reportId);
}

