package ru.itis.semestr_work3.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import ru.itis.semestr_work3.converter.ChatMapper;
import ru.itis.semestr_work3.dto.ChatMessageDto;
import ru.itis.semestr_work3.dto.ChatSessionDto;
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
    private final ChatMapper chatMapper;

    @Operation(summary = "Получить сессии текущего пользователя")
    @GetMapping("/sessions")
    public List<ChatSessionDto> getMySessions(@AuthenticationPrincipal UserDetails principal) {
        User user = resolveUser(principal);
        return chatMapper.toSessionDtoList(aiChatService.getUserSessions(user.getId()));
    }

    @Operation(summary = "Получить сообщения сессии")
    @GetMapping("/sessions/{sessionId}/messages")
    public List<ChatMessageDto> getMessages(@PathVariable Long sessionId,
                                            @AuthenticationPrincipal UserDetails principal) {
        ChatSession session = aiChatService.findSessionById(sessionId);
        checkSessionOwner(session, principal);
        return chatMapper.toMessageDtoList(aiChatService.getSessionMessages(sessionId));
    }

    @Operation(summary = "Создать новую сессию")
    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatSessionDto createSession(@AuthenticationPrincipal UserDetails principal,
                                        @RequestParam(required = false) String title) {
        User user = resolveUser(principal);
        return chatMapper.toDto(aiChatService.createSession(user, title));
    }

    @Operation(summary = "Отправить сообщение в сессию")
    @PostMapping("/sessions/{sessionId}/send")
    public Map<String, String> sendMessage(@PathVariable Long sessionId,
                                           @RequestBody Map<String, String> body,
                                           @AuthenticationPrincipal UserDetails principal) {
        ChatSession session = aiChatService.findSessionById(sessionId);
        checkSessionOwner(session, principal);
        String userMessage = body.get("message");
        if (userMessage == null || userMessage.isBlank()) {
            throw new IllegalArgumentException("Сообщение не может быть пустым");
        }
        String response = aiChatService.sendMessage(sessionId, userMessage);
        return Map.of("response", response);
    }

    private void checkSessionOwner(ChatSession session, UserDetails principal) {
        if (!session.getUser().getUsername().equals(principal.getUsername())) {
            throw new AccessDeniedException("Доступ запрещён");
        }
    }

    private User resolveUser(UserDetails principal) {
        return userService.findByUsername(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }
}