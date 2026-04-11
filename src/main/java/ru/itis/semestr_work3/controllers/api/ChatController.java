package ru.itis.semestr_work3.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.itis.semestr_work3.entity.ChatMessage;
import ru.itis.semestr_work3.entity.ChatSession;
import ru.itis.semestr_work3.entity.User;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.service.AiChatService;
import ru.itis.semestr_work3.service.UserService;

import java.util.List;
import java.util.Map;

@Tag(name = "Chat", description = "AI-чат для подбора автомобилей")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final AiChatService aiChatService;
    private final UserService userService;

    @Operation(summary = "Получить все сессии пользователя")
    @GetMapping("/sessions/user/{userId}")
    public List<ChatSession> getUserSessions(@PathVariable Long userId) {
        return aiChatService.getUserSessions(userId);
    }

    @Operation(summary = "Получить сообщения сессии")
    @GetMapping("/sessions/{sessionId}/messages")
    public List<ChatMessage> getMessages(@PathVariable Long sessionId) {
        return aiChatService.getSessionMessages(sessionId);
    }

    @Operation(summary = "Создать новую сессию")
    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatSession createSession(@RequestParam Long userId,
                                     @RequestParam(required = false) String title) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        return aiChatService.createSession(user, title);
    }

    @Operation(summary = "Отправить сообщение в сессию")
    @PostMapping("/sessions/{sessionId}/send")
    public Map<String, String> sendMessage(@PathVariable Long sessionId,
                                           @RequestBody Map<String, String> body) {
        String userMessage = body.get("message");
        if (userMessage == null || userMessage.isBlank()) {
            throw new IllegalArgumentException("Сообщение не может быть пустым");
        }
        String response = aiChatService.sendMessage(sessionId, userMessage);
        return Map.of("response", response);
    }
}