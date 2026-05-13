package org.skinAI.mapper;

import org.apache.ibatis.annotations.*;
import org.skinAI.pojo.medical.MedicalCase;

import java.util.List;

@Mapper
public interface MedicalCaseMapper {

    @Insert("""
        INSERT INTO medical_case (
          doctor_id, patient_id, case_no, status, chief_complaint, present_history,
          treatment_history, duration, extra_notes, diagnosed_type, ai_advice, ai_introduction, check_time
        ) VALUES (
          #{doctorId}, #{patientId}, #{caseNo}, #{status}, #{chiefComplaint}, #{presentHistory},
          #{treatmentHistory}, #{duration}, #{extraNotes}, #{diagnosedType}, #{aiAdvice}, #{aiIntroduction}, #{checkTime}
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MedicalCase medicalCase);

    @Update("""
        UPDATE medical_case
        SET patient_id = #{patientId},
            status = #{status},
            chief_complaint = #{chiefComplaint},
            present_history = #{presentHistory},
            treatment_history = #{treatmentHistory},
            duration = #{duration},
            extra_notes = #{extraNotes},
            diagnosed_type = #{diagnosedType},
            ai_advice = #{aiAdvice},
            ai_introduction = #{aiIntroduction},
            check_time = #{checkTime}
        WHERE id = #{id} AND doctor_id = #{doctorId}
        """)
    int update(MedicalCase medicalCase);

    @Delete("""
        DELETE FROM medical_case
        WHERE id = #{id} AND doctor_id = #{doctorId}
        """)
    int deleteById(@Param("id") Long id, @Param("doctorId") Long doctorId);

    @Update("""
        UPDATE medical_case
        SET status = #{status}
        WHERE id = #{id} AND doctor_id = #{doctorId}
        """)
    int updateStatus(@Param("id") Long id, @Param("doctorId") Long doctorId, @Param("status") String status);

    @Select("""
        SELECT c.id, c.doctor_id AS doctorId, c.patient_id AS patientId, c.case_no AS caseNo, c.status,
               c.chief_complaint AS chiefComplaint, c.present_history AS presentHistory,
               c.treatment_history AS treatmentHistory, c.duration, c.extra_notes AS extraNotes,
               c.diagnosed_type AS diagnosedType, c.ai_advice AS aiAdvice, c.ai_introduction AS aiIntroduction,
               c.check_time AS checkTime, c.created_at AS createdAt, c.updated_at AS updatedAt,
               p.patient_name AS patientName
        FROM medical_case c
        LEFT JOIN patient p ON p.id = c.patient_id
        WHERE c.id = #{id} AND c.doctor_id = #{doctorId}
        """)
    MedicalCase selectById(@Param("id") Long id, @Param("doctorId") Long doctorId);

    @Select({
            "<script>",
            "SELECT c.id, c.doctor_id AS doctorId, c.patient_id AS patientId, c.case_no AS caseNo, c.status,",
            "c.chief_complaint AS chiefComplaint, c.present_history AS presentHistory,",
            "c.treatment_history AS treatmentHistory, c.duration, c.extra_notes AS extraNotes,",
            "c.diagnosed_type AS diagnosedType, c.ai_advice AS aiAdvice, c.ai_introduction AS aiIntroduction,",
            "c.check_time AS checkTime, c.created_at AS createdAt, c.updated_at AS updatedAt,",
            "p.patient_name AS patientName",
            "FROM medical_case c",
            "LEFT JOIN patient p ON p.id = c.patient_id",
            "WHERE c.doctor_id = #{doctorId}",
            "<if test='status != null and status != \"\"'>",
            "AND c.status = #{status}",
            "</if>",
            "<if test='patientId != null'>",
            "AND c.patient_id = #{patientId}",
            "</if>",
            "<if test='keyword != null and keyword != \"\"'>",
            "AND (p.patient_name LIKE CONCAT('%', #{keyword}, '%') OR c.case_no LIKE CONCAT('%', #{keyword}, '%'))",
            "</if>",
            "ORDER BY c.id DESC",
            "</script>"
    })
    List<MedicalCase> selectByDoctor(
            @Param("doctorId") Long doctorId,
            @Param("status") String status,
            @Param("patientId") Long patientId,
            @Param("keyword") String keyword
    );

    @Select("""
        SELECT COUNT(1)
        FROM medical_case
        WHERE doctor_id = #{doctorId}
          AND DATE(check_time) = CURRENT_DATE
        """)
    Integer countTodayDiagnosed(@Param("doctorId") Long doctorId);

    @Select("""
        SELECT COUNT(1)
        FROM medical_case
        WHERE doctor_id = #{doctorId}
          AND status = #{status}
        """)
    Integer countByStatus(@Param("doctorId") Long doctorId, @Param("status") String status);
}
