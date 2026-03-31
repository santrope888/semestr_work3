package ru.itis.semestr_work3.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.itis.semestr_work3.entity.Role;
import ru.itis.semestr_work3.entity.User;
import ru.itis.semestr_work3.repository.RoleRepository;
import ru.itis.semestr_work3.repository.UserRepository;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public User register(String username, String email, String password, String phoneNumber) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username уже занят");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email уже занят");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setPhoneNumber(phoneNumber);
        user.setCreatedAt(LocalDate.now());

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Role USER not found"));
        user.setRole(userRole);

        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public User updateProfile(Long id, String phoneNumber, String avatarPath) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (phoneNumber != null) user.setPhoneNumber(phoneNumber);
        if (avatarPath != null) user.setAvatarPath(avatarPath);
        return userRepository.save(user);
    }
}