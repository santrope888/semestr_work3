package ru.itis.semestr_work3.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

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
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/payments/convert").permitAll()

                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,   "/api/cars"
                        ).hasAuthority("ADMIN")
                        .requestMatchers(
                                org.springframework.http.HttpMethod.PUT,    "/api/cars/{id}"
                        ).hasAuthority("ADMIN")
                        .requestMatchers(
                                org.springframework.http.HttpMethod.DELETE, "/api/cars/{id}"
                        ).hasAuthority("ADMIN")
                        .requestMatchers("/api/bookings/*/confirm").hasAuthority("ADMIN")
                        .requestMatchers("/api/bookings/*/complete").hasAuthority("ADMIN")
                        .requestMatchers("/api/payments/*/refund").hasAuthority("ADMIN")



                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.disable())
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
                                "/", "/catalog", "/cars/**",
                                "/css/**", "/images/**", "/js/**", "/uploads/**",
                                "/login", "/register"
                        ).permitAll()
                        .requestMatchers("/admin/**").hasAuthority("ADMIN")
                        .requestMatchers(
                                "/profile/**", "/favorites/**", "/bookings/**",
                                "/payments/**", "/notifications/**", "/chat/**"
                        ).authenticated()
                        .requestMatchers("/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml").hasAuthority("ADMIN")
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
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}