package org.crythic.services;

import lombok.RequiredArgsConstructor;
import org.crythic.mapper.ChatMessageMapper;
import org.crythic.mapper.UserChatSessionMapper;
import org.crythic.pojo.ChatMessage;
import org.crythic.pojo.UserChatSession;
import org.crythic.utils.ThreadLocalUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;

@RequiredArgsConstructor
@Service
public class DeepSeekService {
    private final Integer MAXN = 100;
    @Autowired
    UserChatSessionMapper userChatSessionMapper;
    @Autowired
    ChatMessageMapper chatMessageMapper;

    private final ChatClient client;

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
