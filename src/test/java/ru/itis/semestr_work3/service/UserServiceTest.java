package ru.itis.semestr_work3.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.itis.semestr_work3.dto.UserDto;
import ru.itis.semestr_work3.entity.Role;
import ru.itis.semestr_work3.entity.User;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.repository.RoleRepository;
import ru.itis.semestr_work3.repository.UserRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private GridFsDocumentService gridFsDocumentService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private UserService userService;

    private UserDto userDto;

    private User user;
    private Role role;

    @BeforeEach
    void setUp() {
        role = new Role();
        role.setName("USER");

        userDto = new UserDto();
        userDto.setUsername("askar");
        userDto.setEmail("askar@test.com");
        userDto.setPassword("password");
        userDto.setPhoneNumber("+79000000000");

        user = new User();
        user.setId(1L);
        user.setUsername("askar");
        user.setEmail("askar@test.com");
        user.setPhoneNumber("+79000000000");
    }

    @Test
    void register_withValidDto_createsEncodedUserWithDefaultStatuses() {
        when(userRepository.existsByUsername("askar")).thenReturn(false);
        when(userRepository.existsByEmail("askar@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encoded-password");
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.register(userDto);

        assertThat(result.getUsername()).isEqualTo("askar");
        assertThat(result.getEmail()).isEqualTo("askar@test.com");
        assertThat(result.getPassword()).isEqualTo("encoded-password");
        assertThat(result.getPhoneNumber()).isEqualTo("+79000000000");
        assertThat(result.getCreatedAt()).isEqualTo(LocalDate.now());
        assertThat(result.getLicenseStatus()).isEqualTo("NOT_UPLOADED");
        assertThat(result.getPassportStatus()).isEqualTo("NOT_UPLOADED");
        assertThat(result.getRole()).isEqualTo(role);
    }

    @Test
    void register_whenUsernameAlreadyExists_throwsException() {
        when(userRepository.existsByUsername("askar")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.register(userDto));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_whenEmailAlreadyExists_throwsException() {
        when(userRepository.existsByUsername("askar")).thenReturn(false);
        when(userRepository.existsByEmail("askar@test.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.register(userDto));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_whenRoleMissing_throwsException() {
        when(userRepository.existsByUsername("askar")).thenReturn(false);
        when(userRepository.existsByEmail("askar@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encoded-password");
        when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.register(userDto));
    }

    @Test
    void findByUsername_whenExists_returnsUser() {
        when(userRepository.findByUsername("askar")).thenReturn(Optional.of(user));

        assertThat(userService.findByUsername("askar")).contains(user);
    }

    @Test
    void findByUsername_whenMissing_returnsEmpty() {
        when(userRepository.findByUsername("askar")).thenReturn(Optional.empty());

        assertThat(userService.findByUsername("askar")).isEmpty();
    }

    @Test
    void findByEmail_whenExists_returnsUser() {
        when(userRepository.findByEmail("askar@test.com")).thenReturn(Optional.of(user));

        assertThat(userService.findByEmail("askar@test.com")).contains(user);
    }

    @Test
    void findByEmail_whenMissing_returnsEmpty() {
        when(userRepository.findByEmail("askar@test.com")).thenReturn(Optional.empty());

        assertThat(userService.findByEmail("askar@test.com")).isEmpty();
    }

    @Test
    void findById_whenExists_returnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThat(userService.findById(1L)).contains(user);
    }

    @Test
    void findById_whenMissing_returnsEmpty() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(userService.findById(99L)).isEmpty();
    }

    @Test
    void findAll_returnsUsers() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<User> result = userService.findAll();

        assertThat(result).containsExactly(user);
    }

    @Test
    void approveDocument_withLicense_setsLicenseConfirmedAndSendsNotification() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.approveDocument(1L, "license");

        assertThat(result.getLicenseStatus()).isEqualTo("CONFIRMED");
        verify(notificationService).send(
                result,
                "DOCUMENT_APPROVED",
                "Ваш документ \"Водительское удостоверение\" подтверждён администратором."
        );
    }

    @Test
    void approveDocument_withPassport_setsPassportConfirmedAndSendsNotification() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.approveDocument(1L, "passport");

        assertThat(result.getPassportStatus()).isEqualTo("CONFIRMED");
        verify(notificationService).send(
                result,
                "DOCUMENT_APPROVED",
                "Ваш документ \"Паспорт\" подтверждён администратором."
        );
    }

    @Test
    void approveDocument_whenUserMissing_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.approveDocument(99L, "license"));

        verify(userRepository, never()).save(any(User.class));
        verify(notificationService, never()).send(any(User.class), any(), any());
    }

    @Test
    void approveDocument_withUnknownDocumentType_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () -> userService.approveDocument(1L, "unknown"));

        verify(userRepository, never()).save(any(User.class));
        verify(notificationService, never()).send(any(User.class), any(), any());
    }

    @Test
    void rejectDocument_withLicense_setsLicenseRejectedAndSendsNotification() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.rejectDocument(1L, "license");

        assertThat(result.getLicenseStatus()).isEqualTo("REJECTED");
        verify(notificationService).send(
                result,
                "DOCUMENT_REJECTED",
                "Ваш документ \"Водительское удостоверение\" отклонён. Загрузите его повторно."
        );
    }

    @Test
    void rejectDocument_withPassport_setsPassportRejectedAndSendsNotification() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.rejectDocument(1L, "passport");

        assertThat(result.getPassportStatus()).isEqualTo("REJECTED");
        verify(notificationService).send(
                result,
                "DOCUMENT_REJECTED",
                "Ваш документ \"Паспорт\" отклонён. Загрузите его повторно."
        );
    }

    @Test
    void rejectDocument_whenUserMissing_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.rejectDocument(99L, "passport"));

        verify(userRepository, never()).save(any(User.class));
        verify(notificationService, never()).send(any(User.class), any(), any());
    }

    @Test
    void humanDocName_withUnknownType_returnsOriginalValue() {
        String result = ReflectionTestUtils.invokeMethod(userService, "humanDocName", "unknown");

        assertThat(result).isEqualTo("unknown");
    }

    @Test
    void updateFullProfile_withAllFields_updatesUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateFullProfile(
                1L,
                "Иван",
                "Иванов",
                "Иванович",
                LocalDate.of(1990, 1, 1),
                "Москва",
                "Россия",
                "+79111111111",
                null
        );

        assertThat(result.getFirstName()).isEqualTo("Иван");
        assertThat(result.getLastName()).isEqualTo("Иванов");
        assertThat(result.getPatronymic()).isEqualTo("Иванович");
        assertThat(result.getBirthDate()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(result.getCity()).isEqualTo("Москва");
        assertThat(result.getCountry()).isEqualTo("Россия");
        assertThat(result.getPhoneNumber()).isEqualTo("+79111111111");
    }

    @Test
    void updateFullProfile_withAvatar_savesAvatarAndUpdatesPath() {
        MockMultipartFile avatar = new MockMultipartFile("avatar", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(fileStorageService.saveAvatar(any())).thenReturn("/uploads/avatars/photo.jpg");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateFullProfile(1L, null, null, null, null, null, null, null, avatar);

        assertThat(result.getAvatarPath()).isEqualTo("/uploads/avatars/photo.jpg");
    }

    @Test
    void updateFullProfile_withEmptyAvatar_doesNotCallStorageService() {
        MockMultipartFile emptyAvatar = new MockMultipartFile("avatar", "photo.jpg", "image/jpeg", new byte[0]);
        user.setAvatarPath("/old-avatar.jpg");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateFullProfile(1L, null, null, null, null, null, null, null, emptyAvatar);

        assertThat(result.getAvatarPath()).isEqualTo("/old-avatar.jpg");
        verify(fileStorageService, never()).saveAvatar(any());
    }

    @Test
    void updateFullProfile_whenUserMissing_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.updateFullProfile(1L, null, null, null, null, null, null, null, null));
    }

    @Test
    void uploadDocument_withLicense_setsLicenseFields() {
        MockMultipartFile file = new MockMultipartFile("file", "license.jpg", "image/jpeg", new byte[]{1, 2, 3});
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(gridFsDocumentService.store(any())).thenReturn("64f0c1a2b3c4d5e6f7a8b9c0");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.uploadDocument(1L, "license", file);

        assertThat(result.getLicensePath()).isEqualTo("64f0c1a2b3c4d5e6f7a8b9c0");
        assertThat(result.getLicenseStatus()).isEqualTo("PENDING");
        assertThat(result.getLicenseUploadedAt()).isEqualTo(LocalDate.now());
    }

    @Test
    void uploadDocument_withPassport_setsPassportFields() {
        MockMultipartFile file = new MockMultipartFile("file", "passport.pdf", "application/pdf", new byte[]{1, 2, 3});
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(gridFsDocumentService.store(any())).thenReturn("64f0c1a2b3c4d5e6f7a8b9c1");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.uploadDocument(1L, "passport", file);

        assertThat(result.getPassportPath()).isEqualTo("64f0c1a2b3c4d5e6f7a8b9c1");
        assertThat(result.getPassportStatus()).isEqualTo("PENDING");
        assertThat(result.getPassportUploadedAt()).isEqualTo(LocalDate.now());
    }

    @Test
    void uploadDocument_whenOldDocumentExists_deletesOldFromGridFs() {
        user.setLicensePath("oldFileId123");
        MockMultipartFile file = new MockMultipartFile("file", "license.jpg", "image/jpeg", new byte[]{1, 2, 3});
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(gridFsDocumentService.store(any())).thenReturn("newFileId456");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.uploadDocument(1L, "license", file);

        verify(gridFsDocumentService).delete("oldFileId123");
        assertThat(result.getLicensePath()).isEqualTo("newFileId456");
    }

    @Test
    void uploadDocument_whenOldDocumentPathIsBlank_doesNotDeleteOldDocument() {
        user.setLicensePath("   ");
        MockMultipartFile file = new MockMultipartFile("file", "license.jpg", "image/jpeg", new byte[]{1, 2, 3});
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(gridFsDocumentService.store(any())).thenReturn("newFileId456");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.uploadDocument(1L, "license", file);

        verify(gridFsDocumentService, never()).delete(any());
        assertThat(result.getLicensePath()).isEqualTo("newFileId456");
    }

    @Test
    void uploadDocument_withUnknownType_throwsExceptionAndDoesNotSaveFile() {
        MockMultipartFile file = new MockMultipartFile("file", "doc.jpg", "image/jpeg", new byte[]{1, 2, 3});
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () -> userService.uploadDocument(1L, "unknown", file));

        verify(gridFsDocumentService, never()).store(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void uploadDocument_whenUserMissing_throwsException() {
        MockMultipartFile file = new MockMultipartFile("file", "doc.jpg", "image/jpeg", new byte[]{1, 2, 3});
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.uploadDocument(99L, "license", file));
        verify(gridFsDocumentService, never()).store(any());
    }

    @Test
    void updateProfile_updatesPhoneAndAvatar() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateProfile(1L, "+79222222222", "/uploads/avatars/new.jpg");

        assertThat(result.getPhoneNumber()).isEqualTo("+79222222222");
        assertThat(result.getAvatarPath()).isEqualTo("/uploads/avatars/new.jpg");
    }

    @Test
    void updateProfile_withNullFields_doesNotOverwriteExistingValues() {
        user.setAvatarPath("/old-avatar.jpg");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateProfile(1L, null, null);

        assertThat(result.getPhoneNumber()).isEqualTo("+79000000000");
        assertThat(result.getAvatarPath()).isEqualTo("/old-avatar.jpg");
    }

    @Test
    void updateProfile_whenUserMissing_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.updateProfile(1L, "+7", null));
    }

    @Test
    void deleteById_whenUserExists_deletesUser() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteById(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteById_whenUserMissing_throwsException() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> userService.deleteById(1L));
        verify(userRepository, never()).deleteById(any());
    }
}
