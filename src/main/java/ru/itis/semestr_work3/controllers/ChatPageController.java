package ru.itis.semestr_work3.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.itis.semestr_work3.entity.ChatSession;
import ru.itis.semestr_work3.service.AiChatService;
import ru.itis.semestr_work3.service.UserService;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ChatPageController {

    private final AiChatService aiChatService;
    private final UserService userService;

    @GetMapping("/chat")
    public String chat(@AuthenticationPrincipal UserDetails userDetails,
                       @RequestParam(required = false) Long sessionId,
                       Model model) {
        if (userDetails == null) return "redirect:/login";

        var userOpt = userService.findByUsername(userDetails.getUsername());
        if (userOpt.isEmpty()) return "redirect:/login";

        var user = userOpt.get();
        List<ChatSession> sessions = aiChatService.getUserSessions(user.getId());

        ChatSession activeSession = null;

        if (sessionId != null) {
            for (ChatSession session : sessions) {
                if (session.getId().equals(sessionId)) {
                    activeSession = session;
                    break;
                }
            }
        }

        if (activeSession == null) {
            if (sessions.isEmpty()) {
                activeSession = aiChatService.createSession(user, "Подбор автомобиля");
                sessions = List.of(activeSession);
            } else {
                activeSession = sessions.get(0);
            }
        }

        model.addAttribute("sessionId", activeSession.getId());
        model.addAttribute("messages", aiChatService.getSessionMessages(activeSession.getId()));
        model.addAttribute("chatSessions", sessions);
        model.addAttribute("activeSessionId", activeSession.getId());
        model.addAttribute("activeSessionTitle", activeSession.getTitle());

        return "chat";
    }

    @PostMapping("/chat/new")
    public String createNewChat(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return "redirect:/login";

        var userOpt = userService.findByUsername(userDetails.getUsername());
        if (userOpt.isEmpty()) return "redirect:/login";

        ChatSession newSession = aiChatService.createSession(userOpt.get(), "Новый диалог");
        return "redirect:/chat?sessionId=" + newSession.getId();
    }

    @PostMapping("/chat/{sessionId}/delete")
    public String deleteChat(@PathVariable Long sessionId,
                             @RequestParam(required = false) Long currentSessionId,
                             @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        var userOpt = userService.findByUsername(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        var user = userOpt.get();

        aiChatService.deleteSession(sessionId, user.getId());

        List<ChatSession> remainingSessions = aiChatService.getUserSessions(user.getId());

        if (remainingSessions.isEmpty()) {
            ChatSession newSession = aiChatService.createSession(user, "Новый диалог");
            return "redirect:/chat?sessionId=" + newSession.getId();
        }

        Long redirectSessionId = currentSessionId;
        boolean redirectSessionStillExists = false;

        if (redirectSessionId != null) {
            for (ChatSession session : remainingSessions) {
                if (session.getId().equals(redirectSessionId)) {
                    redirectSessionStillExists = true;
                    break;
                }
            }
        }

        if (!redirectSessionStillExists) {
            redirectSessionId = remainingSessions.get(0).getId();
        }

        return "redirect:/chat?sessionId=" + redirectSessionId;
    }
}
