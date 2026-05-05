package ru.itis.semestr_work3.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.itis.semestr_work3.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Регистрация и доступ — интеграционный тест")
class AuthenticationFlowIT extends IntegrationTestBase {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Полный поток: регистрация нового юзера → запись в БД → доступ к /profile")
    void registerNewUser_persistsToDb_andCanAccessProtectedPage() throws Exception {
        assertThat(userRepository.findByUsername("itest_kylie")).isEmpty();

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "itest_kylie")
                        .param("email", "itest_kylie@example.com")
                        .param("password", "Strong123!")
                        .param("phoneNumber", "+7 (900) 123-45-67"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        var saved = userRepository.findByUsername("itest_kylie");
        assertThat(saved).isPresent();
        assertThat(saved.get().getPassword())
                .as("Пароль должен быть захэширован, а не лежать в plain text")
                .startsWith("$2a$");
        assertThat(saved.get().getEmail()).isEqualTo("itest_kylie@example.com");
        assertThat(saved.get().getRole().getName()).isEqualTo("USER");

        mockMvc.perform(get("/profile").with(user("itest_kylie").authorities(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("USER"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Защита: /profile без аутентификации редиректит на /login")
    void unauthenticatedUser_isRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("Защита: /admin/** с authority USER возвращает 403")
    void userAuthority_cannotAccessAdminPages() throws Exception {
        mockMvc.perform(get("/admin/cars").with(user("regularUser").authorities(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Авторизация: /admin/** с authority ADMIN открывается")
    void adminAuthority_canAccessAdminPages() throws Exception {
        mockMvc.perform(get("/admin/cars").with(user("rootAdmin").authorities(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ADMIN"))))
                .andExpect(status().isOk());
    }
}
