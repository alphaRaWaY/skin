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
    private final UserChatSessionMapper userChatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    //用于AI问诊的对象
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
        bindUserToChat(userid, chatId);
        return client.prompt()
                .user(prompt)
                .advisors(advisorSpec -> advisorSpec.param(CHAT_MEMORY_CONVERSATION_ID_KEY,chatId))
                .call()
                .content();
    }

    public Flux<String> getFluxChat(String prompt, String chatId) {
        Map<String,Object> map = ThreadLocalUtil.get();
        Integer userid = (Integer) map.get("userid");
        bindUserToChat(userid, chatId);
        return client.prompt()
                .user(prompt)
                .stream()
                .content();
    }

    public List<UserChatSession> getUserChats(Integer userId) {
        return userChatSessionMapper.findByUserId(userId);
    }

    private void bindUserToChat(Integer userId, String chatId) {
        if (userChatSessionMapper.findByChatId(chatId) == null) {
            UserChatSession session = new UserChatSession();
            session.setUserId(userId);
            session.setChatId(chatId);
            userChatSessionMapper.insert(session);
        }
    }

    public void deleteUserChats(String chatId) {
        userChatSessionMapper.deleteByChatId(chatId);
        chatMessageMapper.deleteByChatId(chatId);
    }

    public List<ChatMessage> getChatDetails(String chatId) {
        return chatMessageMapper.findLastNByChatId(chatId,MAXN);
    }
}
