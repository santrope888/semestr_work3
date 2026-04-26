package ru.itis.semestr_work3.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import ru.itis.semestr_work3.entity.ChatSession;
import ru.itis.semestr_work3.entity.User;
import ru.itis.semestr_work3.service.AiChatService;
import ru.itis.semestr_work3.service.UserService;

import java.util.List;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final UserService userService;
    private final AiChatService aiChatService;

    @ModelAttribute("currentUser")
    public User currentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return null;
        return userService.findByUsername(userDetails.getUsername()).orElse(null);
    }

    @ModelAttribute("chatSessionId")
    public Long chatSessionId(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return null;
        User user = userService.findByUsername(userDetails.getUsername()).orElse(null);
        if (user == null) return null;

        List<ChatSession> sessions = aiChatService.getUserSessions(user.getId());
        ChatSession session = sessions.isEmpty()
                ? aiChatService.createSession(user, "Подбор автомобиля")
                : sessions.get(0);
        return session.getId();
    }

    @ModelAttribute
    public void primeCsrfToken(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token != null) {
            token.getToken();
        }
    }
}