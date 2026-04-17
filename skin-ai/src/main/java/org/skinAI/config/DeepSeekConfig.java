package org.skinAI.config;

import org.skinAI.mapper.ChatMessageMapper;
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

    // --- 新增的 Bean ---
    // 这个 ChatClient 专门用于不需要保存聊天记录的场景（例如生成标题）
    @Bean("statelessChatClient") // 明确指定一个 Bean 名称，方便注入
    public ChatClient statelessChatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient
                .builder(openAiChatModel)
                .defaultSystem("你一个用于生成AI问诊建议和疾病介绍的AI，请根据用户给予的信息进行回答") // 可以为这个无状态客户端设置一个专门的默认系统提示
                .defaultAdvisors(
                        new SimpleLoggerAdvisor() // 仍然可以保留日志顾问，但不再包含聊天记忆顾问
                )
                .build();
    }

}
