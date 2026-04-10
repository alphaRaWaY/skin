package org.crythic.mapper;

import org.apache.ibatis.annotations.*;
import org.crythic.pojo.ChatMessage;

import java.util.List;

@Mapper
public interface ChatMessageMapper {

    @Insert("INSERT INTO chat_message (chat_id, role, content) VALUES (#{chatId}, #{role}, #{content})")
    void insert(ChatMessage message);

    @Select("SELECT * FROM chat_message WHERE chat_id = #{chatId} ORDER BY create_time ASC LIMIT #{lastN}")
    List<ChatMessage> findLastNByChatId(@Param("chatId") String chatId, @Param("lastN") int lastN);

    @Delete("DELETE FROM chat_message WHERE chat_id = #{chatId}")
    void deleteByChatId(String chatId);
}
