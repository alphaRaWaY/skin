package org.skinAI.mapper;

import org.apache.ibatis.annotations.*;
import org.skinAI.pojo.medical.CaseFollowup;

import java.util.List;

@Mapper
public interface CaseFollowupMapper {

    @Insert("""
        INSERT INTO case_followup (case_id, followup_time, summary, next_plan)
        VALUES (#{caseId}, #{followupTime}, #{summary}, #{nextPlan})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CaseFollowup followup);

    @Select("""
        SELECT id, case_id AS caseId, followup_time AS followupTime, summary, next_plan AS nextPlan, created_at AS createdAt
        FROM case_followup
        WHERE case_id = #{caseId}
        ORDER BY followup_time DESC, id DESC
        """)
    List<CaseFollowup> selectByCaseId(@Param("caseId") Long caseId);
}
