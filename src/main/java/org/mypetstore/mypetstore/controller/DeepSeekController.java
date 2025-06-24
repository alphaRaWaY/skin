package org.mypetstore.mypetstore.controller;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.mypetstore.mypetstore.mapper.UserChatSessionMapper;
import org.mypetstore.mypetstore.pojo.ChatMessage;
import org.mypetstore.mypetstore.pojo.ChatRequest;
import org.mypetstore.mypetstore.pojo.Result;
import org.mypetstore.mypetstore.pojo.UserChatSession;
import org.mypetstore.mypetstore.services.DeepSeekService;
import org.mypetstore.mypetstore.utils.ThreadLocalUtil;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/deepseek")
public class DeepSeekController {
    @Autowired
    private DeepSeekService deepSeekService;
    @GetMapping("/history")
    public Result<List<Message>> getHistory() {
        return Result.success(new ArrayList<>());
    }
    @PostMapping("/chat")
    public Result<String> getChat(@RequestBody ChatRequest request) {
        String prompt = request.getPrompt();
        String chatId = request.getChatId();
        return Result.success(deepSeekService.getChat(prompt, chatId));
    }
    @GetMapping("/chat")
    public Result<List<UserChatSession>> getUserChats() {
        Map<String,Object> map = ThreadLocalUtil.get();
        Integer userid = (Integer) map.get("userid");
        List<UserChatSession> list = deepSeekService.getUserChats(userid);
       return Result.success(list);
    }
    @DeleteMapping("/chat/{chatId}")
    public Result deleteUserChats(@PathVariable String chatId) {
        deepSeekService.deleteUserChats(chatId);
        return Result.success();
    }

    @GetMapping("/chat/{chatId}")
    public Result<List<ChatMessage>> getChatDetails(@PathVariable String chatId) {
        return Result.success(deepSeekService.getChatDetails(chatId));
    }

    @PostMapping(value = "/flux_chat",produces = "text/html;charset=utf-8")
    public Flux<String> getFluxChat(@RequestBody ChatRequest request) {
        String prompt = request.getPrompt();
        String chatId = request.getChatId();
        return deepSeekService.getFluxChat(prompt,chatId);
    }
}
