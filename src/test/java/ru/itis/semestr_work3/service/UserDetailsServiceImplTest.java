package ru.itis.semestr_work3.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ru.itis.semestr_work3.entity.Role;
import ru.itis.semestr_work3.entity.User;
import ru.itis.semestr_work3.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void loadUserByUsername_whenUserExists_returnsSpringSecurityUser() {
        Role role = new Role();
        role.setName("USER");

        User user = new User();
        user.setUsername("askar");
        user.setPassword("encoded-password");
        user.setRole(role);

        when(userRepository.findByUsername("askar")).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("askar");

        assertThat(result.getUsername()).isEqualTo("askar");
        assertThat(result.getPassword()).isEqualTo("encoded-password");
        assertThat(result.getAuthorities()).extracting("authority").containsExactly("USER");
    }

    @Test
    void loadUserByUsername_whenUserMissing_throwsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("unknown"));
    }
}
