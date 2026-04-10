package org.crythic.pojo;

import lombok.Data;

@Data
public class ChatRequest {
    private String prompt;
    private String chatId;
}
