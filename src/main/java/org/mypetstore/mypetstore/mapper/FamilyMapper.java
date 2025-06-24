package org.mypetstore.mypetstore.mapper;

import org.apache.ibatis.annotations.*;
import org.mypetstore.mypetstore.pojo.family.Family;
import java.util.List;

@Mapper
public interface FamilyMapper {
    @Delete("DELETE from family where name = #{name} and userid = #{userid}")
    boolean deleteByName(String name, Integer userid);

    @Select("select * from family where userid = #{userid}")
    List<Family> findByUserid(@Param("userid") Integer userid);

    @Select("select * from family where id = #{id}")
    Family findById(@Param("id") Long id);

    @Insert("insert into family (userid, name, gender, age, relation) " +
            "values (#{userid}, #{name}, #{gender}, #{age}, #{relation})")
    void insert(Family family);

    @Update("update family set " +
            "userid = #{userid}, " +
            "name = #{name}, " +
            "gender = #{gender}, " +
            "age = #{age}, " +
            "relation = #{relation} " +
            "where id = #{id}")
    int update(Family family);

    @Delete("delete from family where id = #{id}")
    int deleteById(@Param("id") Long id);

    @Select("select * from family where name = #{name} and userid = #{userid}")
    Family findByName(@Param("name") String name, @Param("userid") Integer userid);

    @Select("select * from family where relation = #{relation} and userid = #{userid}")
    Family findByRelationAndUserid(@Param("relation") String relation,
                                         @Param("userid") Integer userid);
}