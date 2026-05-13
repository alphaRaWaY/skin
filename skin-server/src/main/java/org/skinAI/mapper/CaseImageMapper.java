package org.skinAI.mapper;

import org.apache.ibatis.annotations.*;
import org.skinAI.pojo.medical.CaseImage;

@Mapper
public interface CaseImageMapper {

    @Insert("""
        INSERT INTO case_image (case_id, object_key, public_url, is_primary)
        VALUES (#{caseId}, #{objectKey}, #{publicUrl}, #{primary})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CaseImage image);

    @Select("""
        SELECT id, case_id AS caseId, object_key AS objectKey, public_url AS publicUrl,
               CASE WHEN is_primary = 1 THEN TRUE ELSE FALSE END AS primary,
               created_at AS createdAt
        FROM case_image
        WHERE case_id = #{caseId}
        ORDER BY is_primary DESC, id DESC
        LIMIT 1
        """)
    CaseImage selectPrimaryByCaseId(@Param("caseId") Long caseId);

    @Delete("""
        DELETE FROM case_image
        WHERE case_id = #{caseId}
        """)
    int deleteByCaseId(@Param("caseId") Long caseId);
}
