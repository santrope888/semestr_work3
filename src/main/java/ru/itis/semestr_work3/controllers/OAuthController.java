package ru.itis.semestr_work3.controllers;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.itis.semestr_work3.entity.User;
import ru.itis.semestr_work3.service.OAuthService;
import ru.itis.semestr_work3.service.UserDetailsServiceImpl;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private static final String OAUTH_STATE_ATTR = "OAUTH_STATE";

    private final OAuthService oAuthService;
    private final UserDetailsServiceImpl userDetailsService;

    @GetMapping("/google/login")
    public String googleLogin(HttpSession session) {
        String state = UUID.randomUUID().toString();
        session.setAttribute(OAUTH_STATE_ATTR, state);
        return "redirect:" + oAuthService.buildAuthorizationUrl(state);
    }

    @GetMapping("/google/callback")
    public String googleCallback(@RequestParam String code,
                                 @RequestParam String state,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        String expectedState = (String) session.getAttribute(OAUTH_STATE_ATTR);
        if (expectedState == null || !expectedState.equals(state)) {
            ra.addFlashAttribute("error", "Ошибка авторизации через Google. Попробуйте снова.");
            return "redirect:/login";
        }
        session.removeAttribute(OAUTH_STATE_ATTR);

        try {
            String accessToken = oAuthService.exchangeCodeForToken(code);
            Map<String, Object> userInfo = oAuthService.getUserInfo(accessToken);

            if (Boolean.FALSE.equals(userInfo.get("email_verified"))) {
                ra.addFlashAttribute("error", "Email не подтверждён в Google.");
                return "redirect:/login";
            }

            String subject = (String) userInfo.get("sub");
            String email = (String) userInfo.get("email");
            String fullName = (String) userInfo.getOrDefault("name", "");

            User user = oAuthService.findOrCreateUser(subject, email, fullName);
            storeAuthenticationInSession(user, session);

            return "redirect:/";

        } catch (Exception e) {
            log.error("OAuth Google error: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Не удалось войти через Google. Попробуйте снова.");
            return "redirect:/login";
        }
    }

    private void storeAuthenticationInSession(User user, HttpSession session) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext()
        );
    }
}