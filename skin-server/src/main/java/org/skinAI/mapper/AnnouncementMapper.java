package org.skinAI.mapper;

import org.apache.ibatis.annotations.*;
import org.skinAI.pojo.Announcement;

import java.util.List;

@Mapper
public interface AnnouncementMapper {

    @Select("""
        SELECT id, title, content, status, created_at AS createdAt, updated_at AS updatedAt
        FROM announcement
        WHERE status = 'ENABLED'
        ORDER BY updated_at DESC, id DESC
        """)
    List<Announcement> selectEnabled();

    @Insert("""
        INSERT INTO announcement (title, content, status)
        VALUES (#{title}, #{content}, #{status})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Announcement announcement);

    @Update("""
        UPDATE announcement
        SET title = #{title}, content = #{content}, status = #{status}
        WHERE id = #{id}
        """)
    int update(Announcement announcement);
}
