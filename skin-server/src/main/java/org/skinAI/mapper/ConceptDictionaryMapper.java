package org.skinAI.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.skinAI.pojo.report.ConceptScore;

import java.util.List;

@Mapper
public interface ConceptDictionaryMapper {

    @Insert("""
        INSERT INTO concept_dictionary (concept_index, name_en, name_cn)
        VALUES (#{conceptIndex}, #{nameEn}, #{nameCn})
        ON DUPLICATE KEY UPDATE
            name_en = IFNULL(VALUES(name_en), name_en),
            name_cn = IFNULL(VALUES(name_cn), name_cn)
    """)
    int upsert(
            @Param("conceptIndex") Integer conceptIndex,
            @Param("nameEn") String nameEn,
            @Param("nameCn") String nameCn
    );

    @Select("""
        <script>
        SELECT concept_index AS conceptIndex,
               name_en AS conceptNameEn,
               name_cn AS conceptNameCn
        FROM concept_dictionary
        WHERE concept_index IN
        <foreach collection="indices" item="idx" open="(" separator="," close=")">
            #{idx}
        </foreach>
        </script>
    """)
    List<ConceptScore> selectByIndices(@Param("indices") List<Integer> indices);
}
