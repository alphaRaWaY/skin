package org.crythic.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.crythic.pojo.report.ChildReport;
import org.crythic.pojo.report.Report;

import java.util.List;

@Mapper
public interface ReportMapper {

    @Insert("""
        INSERT INTO report (username, gender, age, symptoms, duration, treatment, other, check_time,
                            image_url, disease_type, value, advice, introduction,userid)
        VALUES (#{username}, #{gender}, #{age}, #{symptoms}, #{duration}, #{treatment}, #{other}, #{checkTime},
                #{imageUrl}, #{diseaseType}, #{value}, #{advice}, #{introduction},#{userid})
    """)
    int insertReport(ChildReport childReport);

    @Delete("DELETE FROM report WHERE id = #{id} and userid = #{userid}")
    int deleteReportById(Long id,Integer userid);

    @Select("SELECT * FROM report WHERE id = #{id} and userid = #{userid}")
    Report selectReportById(Long id,Integer userid);

    @Select("SELECT * FROM report WHERE userid = #{userid} ORDER BY check_time DESC")
    List<Report> selectAllReports(Integer userid);

    @Select("SELECT * FROM report WHERE userid = #{userid} and username=#{username} ORDER BY check_time DESC")
    List<Report> selectReportsByUsername(String username, Integer userid);
}
