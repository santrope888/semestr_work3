package ru.itis.semestr_work3.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.itis.semestr_work3.dto.UserDto;
import ru.itis.semestr_work3.entity.Role;
import ru.itis.semestr_work3.entity.User;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.repository.RoleRepository;
import ru.itis.semestr_work3.repository.UserRepository;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    public User register(UserDto userDto) {
        if (userRepository.existsByUsername(userDto.getUsername())) {
            throw new IllegalArgumentException("Username уже занят");
        }
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("Email уже занят");
        }
        User user = new User();
        user.setUsername(userDto.getUsername());
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setPhoneNumber(userDto.getPhoneNumber());
        user.setCreatedAt(LocalDate.now());
        user.setLicenseStatus("NOT_UPLOADED");
        user.setPassportStatus("NOT_UPLOADED");

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new ResourceNotFoundException("Роль USER не найдена"));
        user.setRole(userRole);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public User updateFullProfile(Long id,
                                  String firstName, String lastName, String patronymic,
                                  LocalDate birthDate, String city, String country,
                                  String phoneNumber, MultipartFile avatar) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        if (firstName  != null) user.setFirstName(firstName);
        if (lastName   != null) user.setLastName(lastName);
        if (patronymic != null) user.setPatronymic(patronymic);
        if (birthDate  != null) user.setBirthDate(birthDate);
        if (city       != null) user.setCity(city);
        if (country    != null) user.setCountry(country);
        if (phoneNumber != null) user.setPhoneNumber(phoneNumber);

        if (avatar != null && !avatar.isEmpty()) {
            String path = fileStorageService.saveAvatar(avatar);
            user.setAvatarPath(path);
        }

        return userRepository.save(user);
    }

    public User uploadDocument(Long id, String docType, MultipartFile file) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        if (!"license".equals(docType) && !"passport".equals(docType)) {
            throw new IllegalArgumentException("Неизвестный тип документа: " + docType);
        }

        String path = fileStorageService.saveDocument(file, "documents");

        if ("license".equals(docType)) {
            user.setLicensePath(path);
            user.setLicenseStatus("PENDING");
            user.setLicenseUploadedAt(LocalDate.now());
        } else {
            user.setPassportPath(path);
            user.setPassportStatus("PENDING");
            user.setPassportUploadedAt(LocalDate.now());
        }

        return userRepository.save(user);
    }

    public User updateProfile(Long id, String phoneNumber, String avatarPath) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (phoneNumber != null) user.setPhoneNumber(phoneNumber);
        if (avatarPath  != null) user.setAvatarPath(avatarPath);
        return userRepository.save(user);
    }

    public void deleteById(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Пользователь не найден");
        }
        userRepository.deleteById(id);
    }
}