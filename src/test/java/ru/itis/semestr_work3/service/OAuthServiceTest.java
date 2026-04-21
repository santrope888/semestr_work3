package ru.itis.semestr_work3.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import ru.itis.semestr_work3.entity.Role;
import ru.itis.semestr_work3.entity.User;
import ru.itis.semestr_work3.repository.RoleRepository;
import ru.itis.semestr_work3.repository.UserRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private OAuthService oAuthService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(oAuthService, "clientId", "test-client-id");
        ReflectionTestUtils.setField(oAuthService, "clientSecret", "test-client-secret");
        ReflectionTestUtils.setField(oAuthService, "redirectUri", "http://localhost:8080/oauth/google/callback");
        ReflectionTestUtils.setField(oAuthService, "authUri", "https://accounts.google.com/o/oauth2/v2/auth");
        ReflectionTestUtils.setField(oAuthService, "tokenUri", "https://oauth2.googleapis.com/token");
        ReflectionTestUtils.setField(oAuthService, "userInfoUri", "https://openidconnect.googleapis.com/v1/userinfo");
    }

    @Test
    void buildAuthorizationUrl_returnsUrlWithAllParameters() {
        String state = "random-state-xyz";

        String url = oAuthService.buildAuthorizationUrl(state);

        assertThat(url)
                .startsWith("https://accounts.google.com/o/oauth2/v2/auth")
                .contains("client_id=test-client-id")
                .contains("redirect_uri=http://localhost:8080/oauth/google/callback")
                .contains("response_type=code")
                .contains("state=random-state-xyz")
                .containsPattern("scope=openid(?:%20| )email(?:%20| )profile");
    }

    @Test
    void exchangeCodeForToken_success_returnsAccessToken() {
        Map<String, Object> responseBody = Map.of("access_token", "token-abc-123");
        ResponseEntity<Map> response = ResponseEntity.ok(responseBody);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

        String token = oAuthService.exchangeCodeForToken("auth-code-456");

        assertThat(token).isEqualTo("token-abc-123");
    }

    @Test
    void exchangeCodeForToken_nullBody_throwsIllegalState() {
        ResponseEntity<Map> response = ResponseEntity.ok(null);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

        assertThrows(IllegalStateException.class,
                () -> oAuthService.exchangeCodeForToken("code"));
    }

    @Test
    void exchangeCodeForToken_missingAccessToken_throwsIllegalState() {
        Map<String, Object> responseBody = Map.of("error", "invalid_grant");
        ResponseEntity<Map> response = ResponseEntity.ok(responseBody);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

        assertThrows(IllegalStateException.class,
                () -> oAuthService.exchangeCodeForToken("code"));
    }

    @Test
    void getUserInfo_success_returnsBody() {
        Map<String, Object> userInfo = Map.of(
                "sub", "google-123",
                "email", "test@example.com",
                "email_verified", true,
                "name", "Test User"
        );
        ResponseEntity<Map> response = ResponseEntity.ok(userInfo);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

        Map<String, Object> result = oAuthService.getUserInfo("access-token");

        assertThat(result).isEqualTo(userInfo);
    }

    @Test
    void getUserInfo_nullBody_throwsIllegalState() {
        ResponseEntity<Map> response = ResponseEntity.ok(null);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

        assertThrows(IllegalStateException.class,
                () -> oAuthService.getUserInfo("access-token"));
    }

    @Test
    void findOrCreateUser_existingByOAuthSubject_returnsSameUser() {
        User existing = new User();
        existing.setId(42L);
        existing.setOauthSubject("sub-100");

        when(userRepository.findByOauthProviderAndOauthSubject("GOOGLE", "sub-100"))
                .thenReturn(Optional.of(existing));

        User result = oAuthService.findOrCreateUser("sub-100", "test@example.com", "Test User");

        assertThat(result).isSameAs(existing);
        verify(userRepository, never()).save(any());
    }

    @Test
    void findOrCreateUser_existingByEmail_linksOAuthAndSaves() {
        User existing = new User();
        existing.setId(7L);
        existing.setEmail("legacy@example.com");

        when(userRepository.findByOauthProviderAndOauthSubject("GOOGLE", "sub-200"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("legacy@example.com"))
                .thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        User result = oAuthService.findOrCreateUser("sub-200", "legacy@example.com", "Legacy");

        assertThat(result.getOauthProvider()).isEqualTo("GOOGLE");
        assertThat(result.getOauthSubject()).isEqualTo("sub-200");
        verify(userRepository).save(existing);
    }

    @Test
    void findOrCreateUser_newUser_createsWithHashedPasswordAndUsernameFromEmail() {
        Role role = new Role();
        role.setName("USER");

        when(userRepository.findByOauthProviderAndOauthSubject("GOOGLE", "sub-300"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("new.user@example.com"))
                .thenReturn(Optional.empty());
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$HASHED");
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        User result = oAuthService.findOrCreateUser("sub-300", "new.user@example.com", "John Doe");

        assertThat(result.getUsername()).isEqualTo("new_user");
        assertThat(result.getEmail()).isEqualTo("new.user@example.com");
        assertThat(result.getPassword()).isEqualTo("$2a$10$HASHED");
        assertThat(result.getOauthProvider()).isEqualTo("GOOGLE");
        assertThat(result.getOauthSubject()).isEqualTo("sub-300");
        assertThat(result.getRole()).isSameAs(role);
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    void findOrCreateUser_newUserWithSingleNameToken_doesNotSetLastName() {
        Role role = new Role();
        role.setName("USER");

        when(userRepository.findByOauthProviderAndOauthSubject("GOOGLE", "sub-400"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("single@example.com"))
                .thenReturn(Optional.empty());
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        User result = oAuthService.findOrCreateUser("sub-400", "single@example.com", "Cher");

        assertThat(result.getFirstName()).isEqualTo("Cher");
        assertThat(result.getLastName()).isNull();
    }

    @Test
    void findOrCreateUser_newUserWithBlankFullName_skipsNameParsing() {
        Role role = new Role();
        role.setName("USER");

        when(userRepository.findByOauthProviderAndOauthSubject("GOOGLE", "sub-500"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("noname@example.com"))
                .thenReturn(Optional.empty());
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        User result = oAuthService.findOrCreateUser("sub-500", "noname@example.com", "");

        assertThat(result.getFirstName()).isNull();
        assertThat(result.getLastName()).isNull();
    }

    @Test
    void findOrCreateUser_newUserWithNullFullName_skipsNameParsing() {
        Role role = new Role();
        role.setName("USER");

        when(userRepository.findByOauthProviderAndOauthSubject("GOOGLE", "sub-550"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("nullname@example.com"))
                .thenReturn(Optional.empty());
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        User result = oAuthService.findOrCreateUser("sub-550", "nullname@example.com", null);

        assertThat(result.getFirstName()).isNull();
        assertThat(result.getLastName()).isNull();
    }

    @Test
    void findOrCreateUser_missingUserRole_throwsIllegalState() {
        when(userRepository.findByOauthProviderAndOauthSubject("GOOGLE", "sub-600"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("any@example.com"))
                .thenReturn(Optional.empty());
        when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> oAuthService.findOrCreateUser("sub-600", "any@example.com", "Any"));
    }

    @Test
    void generateUsername_baseFree_returnsSanitizedBase() {
        when(userRepository.findByUsername("clean_user"))
                .thenReturn(Optional.empty());

        String username = oAuthService.generateUsername("clean.user@example.com");

        assertThat(username).isEqualTo("clean_user");
    }

    @Test
    void generateUsername_takenOnce_returnsWithSuffix1() {
        when(userRepository.findByUsername("taken"))
                .thenReturn(Optional.of(new User()));
        when(userRepository.findByUsername("taken_1"))
                .thenReturn(Optional.empty());

        String username = oAuthService.generateUsername("taken@example.com");

        assertThat(username).isEqualTo("taken_1");
    }

    @Test
    void generateUsername_sanitizesSpecialChars() {
        when(userRepository.findByUsername("a_b_c"))
                .thenReturn(Optional.empty());

        String username = oAuthService.generateUsername("a+b-c@example.com");

        assertThat(username).isEqualTo("a_b_c");
    }
}