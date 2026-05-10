package org.skinAI.mapper;

import org.apache.ibatis.annotations.*;
import org.skinAI.pojo.User;

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

    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(@Param("username") String username);

    @Select("SELECT * FROM user WHERE (mobile = #{account} OR job_number = #{account}) LIMIT 1")
    User findByMobileOrJobNumber(@Param("account") String account);

    @Update("UPDATE user SET password_hash = #{passwordHash}, job_number = #{jobNumber} WHERE id = #{id}")
    int updatePasswordAndJobNumber(@Param("id") Long id,
                                   @Param("passwordHash") String passwordHash,
                                   @Param("jobNumber") String jobNumber);
}
