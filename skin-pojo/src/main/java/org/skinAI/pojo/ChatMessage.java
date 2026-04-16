package org.skinAI.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessage {
    private Long id;
    private String chatId;
    private String role;
    private String content;
    private LocalDateTime createTime;
}
