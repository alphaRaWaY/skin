package org.skinAI.mapper;

import org.apache.ibatis.annotations.*;
import org.skinAI.pojo.medical.Patient;

import java.util.List;

@Mapper
public interface PatientMapper {

    @Insert("""
        INSERT INTO patient (doctor_id, patient_name, gender, age, phone, id_card_masked, notes)
        VALUES (#{doctorId}, #{patientName}, #{gender}, #{age}, #{phone}, #{idCardMasked}, #{notes})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Patient patient);

    @Update("""
        UPDATE patient
        SET patient_name = #{patientName},
            gender = #{gender},
            age = #{age},
            phone = #{phone},
            id_card_masked = #{idCardMasked},
            notes = #{notes}
        WHERE id = #{id} AND doctor_id = #{doctorId}
        """)
    int update(Patient patient);

    @Delete("""
        DELETE FROM patient
        WHERE id = #{id} AND doctor_id = #{doctorId}
        """)
    int deleteById(@Param("id") Long id, @Param("doctorId") Long doctorId);

    @Select("""
        SELECT id, doctor_id AS doctorId, patient_name AS patientName, gender, age, phone,
               id_card_masked AS idCardMasked, notes, created_at AS createdAt, updated_at AS updatedAt
        FROM patient
        WHERE id = #{id} AND doctor_id = #{doctorId}
        """)
    Patient selectById(@Param("id") Long id, @Param("doctorId") Long doctorId);

    @Select({
            "<script>",
            "SELECT id, doctor_id AS doctorId, patient_name AS patientName, gender, age, phone,",
            "id_card_masked AS idCardMasked, notes, created_at AS createdAt, updated_at AS updatedAt",
            "FROM patient",
            "WHERE doctor_id = #{doctorId}",
            "<if test='keyword != null and keyword != \"\"'>",
            "AND patient_name LIKE CONCAT('%', #{keyword}, '%')",
            "</if>",
            "ORDER BY id DESC",
            "</script>"
    })
    List<Patient> selectByDoctor(@Param("doctorId") Long doctorId, @Param("keyword") String keyword);

    @Select("""
        SELECT id, doctor_id AS doctorId, patient_name AS patientName, gender, age, phone,
               id_card_masked AS idCardMasked, notes, created_at AS createdAt, updated_at AS updatedAt
        FROM patient
        WHERE doctor_id = #{doctorId}
          AND patient_name = #{patientName}
        ORDER BY id DESC
        LIMIT 1
        """)
    Patient selectByDoctorAndName(@Param("doctorId") Long doctorId, @Param("patientName") String patientName);
}
