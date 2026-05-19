package ru.itis.semestr_work3.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.security.web.server.header.XXssProtectionServerHttpHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Set<String> CSRF_SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http,
                                              UserDetailsService uds) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        http
                .headers(headers -> headers
                        .contentSecurityPolicy(csp ->
                                csp.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'")
                        )
                        .contentTypeOptions(Customizer.withDefaults())
                        .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                        .frameOptions(frameOptions -> frameOptions.deny())
                )
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/payments/convert").permitAll()

                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST, "/api/cars"
                        ).hasAuthority("ADMIN")
                        .requestMatchers(
                                org.springframework.http.HttpMethod.PUT, "/api/cars/{id}"
                        ).hasAuthority("ADMIN")
                        .requestMatchers(
                                org.springframework.http.HttpMethod.DELETE, "/api/cars/{id}"
                        ).hasAuthority("ADMIN")
                        .requestMatchers("/api/bookings/*/confirm").hasAuthority("ADMIN")
                        .requestMatchers("/api/bookings/*/complete").hasAuthority("ADMIN")
                        .requestMatchers("/api/payments/*/refund").hasAuthority("ADMIN")

                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf
                        .requireCsrfProtectionMatcher(request -> {
                            if (CSRF_SAFE_METHODS.contains(request.getMethod())) {
                                return false;
                            }
                            String authHeader = request.getHeader("Authorization");
                            if (authHeader != null && authHeader.startsWith("Basic ")) {
                                return false;
                            }
                            return true;
                        })
                )
                .httpBasic(Customizer.withDefaults())
                .cors(cors -> cors.configurationSource(corsSource()))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, authEx) -> {
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            mapper.writeValue(res.getOutputStream(),
                                    Map.of("error", "Необходима аутентификация"));
                        })
                        .accessDeniedHandler((req, res, accessEx) -> {
                            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            mapper.writeValue(res.getOutputStream(),
                                    Map.of("error", "Доступ запрещён"));
                        })
                )
                .userDetailsService(uds);

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain mvcFilterChain(HttpSecurity http,
                                              UserDetailsService uds) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/catalog",
                                "/cars/**",
                                "/css/**",
                                "/images/**",
                                "/js/**",
                                "/uploads/avatars/**",
                                "/uploads/cars/**",
                                "/login",
                                "/register",
                                "/oauth/**",
                                "/error",
                                "/error/**"
                        ).permitAll()

                        .requestMatchers(
                                "/admin/cars",
                                "/admin/cars/new",
                                "/admin/cars/*/edit",
                                "/admin/cars/*/delete",
                                "/admin/bookings",
                                "/admin/bookings/*/confirm",
                                "/admin/bookings/*/complete",
                                "/admin/bookings/*/cancel",
                                "/admin/bookings/*/refund",
                                "/admin/users",
                                "/admin/users/*/documents/*",
                                "/admin/users/*/documents/*/approve",
                                "/admin/users/*/documents/*/reject"
                        ).hasAuthority("ADMIN")

                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml"
                        ).hasAuthority("ADMIN")

                        .requestMatchers(
                                "/profile",
                                "/profile/**",
                                "/favorites",
                                "/bookings",
                                "/bookings/new",
                                "/bookings/*/cancel",
                                "/bookings/wizard/**",
                                "/payments/**",
                                "/chat",
                                "/chat/new",
                                "/chat/*/delete"
                        ).authenticated()

                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,
                                "/bookings",
                                "/cars/*/reviews"
                        ).authenticated()

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .userDetailsService(uds);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:8080",
                "http://127.0.0.1:8080"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "Authorization", "X-CSRF-TOKEN", "X-XSRF-TOKEN"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}