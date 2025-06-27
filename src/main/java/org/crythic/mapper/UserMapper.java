package org.crythic.mapper;

import org.apache.ibatis.annotations.*;
import org.crythic.pojo.User;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM user WHERE openid = #{openid}")
    User findByOpenid(@Param("openid") String openid);

    @Insert("INSERT INTO user (openid, username, nickname, avatar, mobile, create_time) " +
            "VALUES (#{openid}, #{username}, #{nickname}, #{avatar}, #{mobile}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(User user);

    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(@Param("id") Integer id);

    @Select("SELECT * from user where username= #{yourTestUsername}")
    User findByUsername(String yourTestUsername);
}
