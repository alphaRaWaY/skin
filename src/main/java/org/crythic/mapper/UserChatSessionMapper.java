package org.crythic.mapper;

import org.apache.ibatis.annotations.*;
import org.crythic.pojo.UserChatSession;

import java.util.List;

@Mapper
public interface UserChatSessionMapper {



    @Insert("INSERT INTO user_chat_session (user_id, chat_id) VALUES (#{userId}, #{chatId})")
    void insert(UserChatSession session);

    @Select("SELECT * FROM user_chat_session WHERE user_id = #{userId}")
    List<UserChatSession> findByUserId(Integer userId);

    @Select("SELECT * FROM user_chat_session WHERE chat_id = #{chatId}")
    UserChatSession findByChatId(String chatId);

    @Delete("DELETE FROM user_chat_session WHERE chat_id = #{chatId}")
    void deleteByChatId(String chatId);
}
