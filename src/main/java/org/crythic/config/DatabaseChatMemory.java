package org.crythic.config;

import lombok.extern.slf4j.Slf4j;
import org.crythic.mapper.ChatMessageMapper;
import org.crythic.pojo.ChatMessage;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class DatabaseChatMemory implements ChatMemory {

    private final ChatMessageMapper mapper;

    public DatabaseChatMemory(ChatMessageMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        for (Message message : messages) {
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setChatId(conversationId);
            chatMessage.setRole(message.getMessageType().name().toLowerCase()); // user, assistant, system
            chatMessage.setContent(message.getContent());
            mapper.insert(chatMessage);
        }
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        List<ChatMessage> records = mapper.findLastNByChatId(conversationId, lastN);
        return records.stream().map(msg -> {
            switch (msg.getRole()) {
                case "user": return new UserMessage(msg.getContent());
                case "assistant": return new AssistantMessage(msg.getContent());
                case "system": return new SystemMessage(msg.getContent());
                default:
                    log.warn("Unknown role type: {}", msg.getRole());
                    return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public void clear(String conversationId) {
        mapper.deleteByChatId(conversationId);
    }
}
