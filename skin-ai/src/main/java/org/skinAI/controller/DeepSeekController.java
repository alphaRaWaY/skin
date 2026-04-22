package org.skinAI.controller;

import org.skinAI.pojo.ChatMessage;
import org.skinAI.pojo.ChatRequest;
import org.skinAI.pojo.ChatSessionTitleRequest;
import org.skinAI.pojo.Result;
import org.skinAI.pojo.UserChatSession;
import org.skinAI.services.DeepSeekService;
import org.skinAI.utils.ThreadLocalUtil;
import org.springframework.ai.chat.messages.Message;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/deepseek")
public class DeepSeekController {

    private final DeepSeekService deepSeekService;

    public DeepSeekController(DeepSeekService deepSeekService) {
        this.deepSeekService = deepSeekService;
    }

    @GetMapping("/history")
    public Result<List<Message>> getHistory() {
        return Result.success(new ArrayList<>());
    }

    @PostMapping("/advice")
    public Result<String> getAdvice(@RequestBody String prompt) {
        return Result.success(deepSeekService.getAdvice(prompt));
    }

    @PostMapping("/chat")
    public Result<String> getChat(@RequestBody ChatRequest request) {
        return Result.success(deepSeekService.getChat(request.getPrompt(), request.getChatId()));
    }

    @GetMapping("/chat")
    public Result<List<UserChatSession>> getUserChats() {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userid = (Integer) map.get("userid");
        List<UserChatSession> list = deepSeekService.getUserChats(userid);
        return Result.success(list);
    }

    @DeleteMapping("/chat/{chatId}")
    public Result deleteUserChats(@PathVariable String chatId) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userid = (Integer) map.get("userid");
        deepSeekService.deleteUserChats(userid, chatId);
        return Result.success();
    }

    @PatchMapping("/chat/{chatId}/title")
    public Result updateChatTitle(@PathVariable String chatId, @RequestBody ChatSessionTitleRequest request) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userid = (Integer) map.get("userid");
        deepSeekService.renameUserChat(userid, chatId, request.getTitle());
        return Result.success();
    }

    @GetMapping("/chat/{chatId}")
    public Result<List<ChatMessage>> getChatDetails(@PathVariable String chatId) {
        return Result.success(deepSeekService.getChatDetails(chatId));
    }

    @PostMapping(value = "/flux_chat", produces = "text/html;charset=utf-8")
    public Flux<String> getFluxChat(@RequestBody ChatRequest request) {
        return deepSeekService.getFluxChat(request.getPrompt(), request.getChatId());
    }
}
