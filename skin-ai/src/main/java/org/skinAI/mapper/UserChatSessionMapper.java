package org.skinAI.mapper;

import org.apache.ibatis.annotations.*;
import org.skinAI.pojo.UserChatSession;

import java.util.List;

@Mapper
public interface UserChatSessionMapper {

    @Insert("INSERT INTO user_chat_session (user_id, chat_id, title) VALUES (#{userId}, #{chatId}, #{title})")
    void insert(UserChatSession session);

    @Select("""
            SELECT id,
                   user_id AS userId,
                   chat_id AS chatId,
                   title,
                   created_at AS createdAt,
                   updated_at AS updatedAt,
                   last_activity_time AS lastActivityTime
            FROM user_chat_session
            WHERE user_id = #{userId}
            ORDER BY updated_at DESC, last_activity_time DESC
            """)
    List<UserChatSession> findByUserId(@Param("userId") Integer userId);

    @Select("""
            SELECT id,
                   user_id AS userId,
                   chat_id AS chatId,
                   title,
                   created_at AS createdAt,
                   updated_at AS updatedAt,
                   last_activity_time AS lastActivityTime
            FROM user_chat_session
            WHERE user_id = #{userId} AND chat_id = #{chatId}
            LIMIT 1
            """)
    UserChatSession findByUserIdAndChatId(@Param("userId") Integer userId, @Param("chatId") String chatId);

    @Update("""
            UPDATE user_chat_session
            SET title = #{title}, updated_at = CURRENT_TIMESTAMP
            WHERE user_id = #{userId} AND chat_id = #{chatId}
            """)
    int updateTitle(@Param("userId") Integer userId, @Param("chatId") String chatId, @Param("title") String title);

    @Update("""
            UPDATE user_chat_session
            SET last_activity_time = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
            WHERE user_id = #{userId} AND chat_id = #{chatId}
            """)
    int touchActivity(@Param("userId") Integer userId, @Param("chatId") String chatId);

    @Delete("DELETE FROM user_chat_session WHERE user_id = #{userId} AND chat_id = #{chatId}")
    int deleteByUserIdAndChatId(@Param("userId") Integer userId, @Param("chatId") String chatId);
}
