package org.crythic.config;

import org.crythic.mapper.ChatMessageMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeepSeekConfig {

    @Bean
    public ChatMemory chatMemory(ChatMessageMapper mapper) {
        return new DatabaseChatMemory(mapper);
    }

    @Bean
    public ChatClient chatClient(OpenAiChatModel openAiChatModel, ChatMemory chatMemory) {
        return ChatClient
                .builder(openAiChatModel)
//                .defaultSystem("你是由alphaRaWaY开发的个人学习用小助手alphaAI，请你帮助alphaRaWaY完成他们的问题。")
                .defaultSystem("你是一名皮肤科医生，请回答用户问题进行AI问诊，如果用户问了与问诊过于无关的问题请不要回答，而是提醒用户请提出和皮肤问诊有关的问题")
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        new MessageChatMemoryAdvisor(chatMemory)
                )
                .build();
    }
}
