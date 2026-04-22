package org.skinAI.services;

import org.skinAI.mapper.ChatMessageMapper;
import org.skinAI.mapper.UserChatSessionMapper;
import org.skinAI.pojo.ChatMessage;
import org.skinAI.pojo.UserChatSession;
import org.skinAI.utils.ThreadLocalUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;

@Service
public class DeepSeekService {
    private final Integer MAXN = 100;
    private static final int MAX_TITLE_LENGTH = 40;
    private final UserChatSessionMapper userChatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    //用于AI问诊的对�?
    private final ChatClient client;
    private final ChatClient adviceClient;

    public DeepSeekService(@Qualifier("chatClient") ChatClient chatClient,
                           @Qualifier("statelessChatClient") ChatClient statelessChatClient,
                           UserChatSessionMapper userChatSessionMapper,
                           ChatMessageMapper chatMessageMapper) {
        this.client = chatClient;
        this.adviceClient = statelessChatClient;
        this.userChatSessionMapper = userChatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
    }


    public String getAdvice(String prompt) {
        return adviceClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    public String getChat(String prompt, String chatId) {
        Map<String,Object> map = ThreadLocalUtil.get();
        Integer userid = (Integer) map.get("userid");
        bindUserToChat(userid, chatId, prompt);
        userChatSessionMapper.touchActivity(userid, chatId);
        return client.prompt()
                .user(prompt)
                .advisors(advisorSpec -> advisorSpec.param(CHAT_MEMORY_CONVERSATION_ID_KEY,chatId))
                .call()
                .content();
    }

    public Flux<String> getFluxChat(String prompt, String chatId) {
        Map<String,Object> map = ThreadLocalUtil.get();
        Integer userid = (Integer) map.get("userid");
        bindUserToChat(userid, chatId, prompt);
        userChatSessionMapper.touchActivity(userid, chatId);
        return client.prompt()
                .user(prompt)
                .stream()
                .content();
    }

    public List<UserChatSession> getUserChats(Integer userId) {
        return userChatSessionMapper.findByUserId(userId);
    }

    private void bindUserToChat(Integer userId, String chatId, String prompt) {
        if (userChatSessionMapper.findByUserIdAndChatId(userId, chatId) == null) {
            UserChatSession session = new UserChatSession();
            session.setUserId(userId);
            session.setChatId(chatId);
            session.setTitle(buildDefaultTitle(prompt));
            userChatSessionMapper.insert(session);
        }
    }

    public void deleteUserChats(Integer userId, String chatId) {
        UserChatSession session = userChatSessionMapper.findByUserIdAndChatId(userId, chatId);
        if (session == null) {
            return;
        }
        userChatSessionMapper.deleteByUserIdAndChatId(userId, chatId);
        chatMessageMapper.deleteByChatId(chatId);
    }

    public void renameUserChat(Integer userId, String chatId, String title) {
        userChatSessionMapper.updateTitle(userId, chatId, normalizeTitle(title));
    }

    public List<ChatMessage> getChatDetails(String chatId) {
        return chatMessageMapper.findLastNByChatId(chatId,MAXN);
    }

    private String buildDefaultTitle(String prompt) {
        String normalizedPrompt = normalizeTitle(prompt);
        if (normalizedPrompt == null || normalizedPrompt.isBlank()) {
            return "New Chat";
        }
        return normalizedPrompt;
    }

    private String normalizeTitle(String title) {
        if (title == null) {
            return null;
        }
        String normalized = title.replaceAll("\\s+", " ").trim();
        if (normalized.length() > MAX_TITLE_LENGTH) {
            return normalized.substring(0, MAX_TITLE_LENGTH);
        }
        return normalized;
    }
}

