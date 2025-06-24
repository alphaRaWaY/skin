package org.mypetstore.mypetstore.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserChatSession {
    private Long id;
    private Integer userId;
    private String chatId;
    private LocalDateTime createdAt;
}
