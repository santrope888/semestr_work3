package ru.itis.semestr_work3.controllers;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;
import ru.itis.semestr_work3.entity.Role;
import ru.itis.semestr_work3.entity.User;
import ru.itis.semestr_work3.repository.RoleRepository;
import ru.itis.semestr_work3.repository.UserRepository;
import ru.itis.semestr_work3.service.UserDetailsServiceImpl;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final RestTemplate restTemplate;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserDetailsServiceImpl userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Value("${oauth.google.client-id}")
    private String clientId;

    @Value("${oauth.google.client-secret}")
    private String clientSecret;

    @Value("${oauth.google.redirect-uri}")
    private String redirectUri;

    @Value("${oauth.google.auth-uri}")
    private String authUri;

    @Value("${oauth.google.token-uri}")
    private String tokenUri;

    @Value("${oauth.google.userinfo-uri}")
    private String userInfoUri;

    @GetMapping("/google/login")
    public String googleLogin(HttpSession session) {
        String state = UUID.randomUUID().toString();
        session.setAttribute("OAUTH_STATE", state);

        String url = UriComponentsBuilder
                .fromUriString(authUri)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("state", state)
                .build()
                .toUriString();

        return "redirect:" + url;
    }

    @GetMapping("/google/callback")
    public String googleCallback(@RequestParam String code,
                                 @RequestParam String state,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        String expectedState = (String) session.getAttribute("OAUTH_STATE");
        if (expectedState == null || !expectedState.equals(state)) {
            ra.addFlashAttribute("error", "Ошибка авторизации через Google. Попробуйте снова.");
            return "redirect:/login";
        }
        session.removeAttribute("OAUTH_STATE");

        try {
            String accessToken = exchangeCodeForToken(code);
            Map<String, Object> userInfo = getUserInfo(accessToken);

            Boolean emailVerified = (Boolean) userInfo.get("email_verified");
            if (Boolean.FALSE.equals(emailVerified)) {
                ra.addFlashAttribute("error", "Email не подтверждён в Google.");
                return "redirect:/login";
            }

            String subject  = (String) userInfo.get("sub");
            String email    = (String) userInfo.get("email");
            String fullName = (String) userInfo.getOrDefault("name", "");

            User user = findOrCreateUser(subject, email, fullName);
            authenticateUser(user, session);

            return "redirect:/";

        } catch (Exception e) {
            log.error("OAuth Google error: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Не удалось войти через Google. Попробуйте снова.");
            return "redirect:/login";
        }
    }

    private String exchangeCodeForToken(String code) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/x-www-form-urlencoded");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUri, request, Map.class);

        if (response.getBody() == null) {
            throw new IllegalStateException("Пустой ответ от Google token endpoint");
        }

        String accessToken = (String) response.getBody().get("access_token");
        if (accessToken == null) {
            throw new IllegalStateException("Google не вернул access_token");
        }

        return accessToken;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                userInfoUri,
                HttpMethod.GET,
                request,
                Map.class
        );

        if (response.getBody() == null) {
            throw new IllegalStateException("Пустой ответ от Google userinfo endpoint");
        }

        return response.getBody();
    }

    private User findOrCreateUser(String subject, String email, String fullName) {
        Optional<User> byOAuth = userRepository.findByOauthProviderAndOauthSubject("GOOGLE", subject);
        if (byOAuth.isPresent()) {
            return byOAuth.get();
        }

        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            User existing = byEmail.get();
            existing.setOauthProvider("GOOGLE");
            existing.setOauthSubject(subject);
            return userRepository.save(existing);
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Роль USER не найдена"));

        String username = generateUsername(email);

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        newUser.setRole(userRole);
        newUser.setOauthProvider("GOOGLE");
        newUser.setOauthSubject(subject);
        newUser.setCreatedAt(LocalDate.now());

        if (!fullName.isBlank()) {
            String[] parts = fullName.split(" ", 2);
            newUser.setFirstName(parts[0]);
            if (parts.length > 1) newUser.setLastName(parts[1]);
        }

        return userRepository.save(newUser);
    }

    private void authenticateUser(User user, HttpSession session) {
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

    private String generateUsername(String email) {
        String base = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "_");
        String candidate = base;
        int suffix = 1;
        while (userRepository.findByUsername(candidate).isPresent()) {
            candidate = base + "_" + suffix++;
        }
        return candidate;
    }
}