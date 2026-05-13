package org.skinAI.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.skinAI.pojo.report.ConceptScore;

import java.util.List;

@Mapper
public interface CaseConceptScoreMapper {

    @Insert("""
        <script>
        INSERT INTO case_concept_score (case_id, concept_index, concept_name_en, concept_name_cn, score, rank_no)
        VALUES
        <foreach collection="scores" item="s" separator=",">
            (#{caseId}, #{s.conceptIndex}, #{s.conceptNameEn}, #{s.conceptNameCn}, #{s.score}, #{s.rankNo})
        </foreach>
        </script>
        """)
    int batchInsert(@Param("caseId") Long caseId, @Param("scores") List<ConceptScore> scores);

    @Select("""
        SELECT concept_index AS conceptIndex,
               concept_name_en AS conceptNameEn,
               concept_name_cn AS conceptNameCn,
               score,
               rank_no AS rankNo
        FROM case_concept_score
        WHERE case_id = #{caseId}
        ORDER BY rank_no ASC, id ASC
        """)
    List<ConceptScore> selectByCaseId(@Param("caseId") Long caseId);
}
