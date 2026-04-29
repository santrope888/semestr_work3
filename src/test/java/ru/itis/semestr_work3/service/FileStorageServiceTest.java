package ru.itis.semestr_work3.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class FileStorageServiceTest {

    private FileStorageService fileStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService();
        ReflectionTestUtils.setField(fileStorageService, "uploadDir", tempDir.toString());
    }

    @Test
    void saveAvatar_validJpeg_returnsPathAndCreatesFile() {
        MockMultipartFile file = new MockMultipartFile(
                "avatar",
                "photo.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );

        String path = fileStorageService.saveAvatar(file);

        assertThat(path).startsWith("/uploads/avatars/").endsWith(".jpg");

        String fileName = path.substring("/uploads/avatars/".length());
        Path savedFile = tempDir.resolve("avatars").resolve(fileName);
        assertThat(Files.exists(savedFile)).isTrue();
    }

    @Test
    void saveAvatar_validPng_returnsPath() {
        MockMultipartFile file = new MockMultipartFile(
                "avatar",
                "photo.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        String path = fileStorageService.saveAvatar(file);

        assertThat(path).startsWith("/uploads/avatars/").endsWith(".png");
    }

    @Test
    void saveAvatar_validWebp_returnsPath() {
        MockMultipartFile file = new MockMultipartFile(
                "avatar",
                "photo.webp",
                "image/webp",
                new byte[]{1, 2, 3}
        );

        String path = fileStorageService.saveAvatar(file);

        assertThat(path).startsWith("/uploads/avatars/").endsWith(".webp");
    }

    @Test
    void saveAvatar_withoutExtension_usesContentTypeFallback() {
        MockMultipartFile file = new MockMultipartFile(
                "avatar",
                "photo",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );

        String path = fileStorageService.saveAvatar(file);

        assertThat(path).endsWith(".jpg");
    }

    @Test
    void saveAvatar_emptyFile_throwsException() {
        MockMultipartFile file = new MockMultipartFile(
                "avatar",
                "photo.jpg",
                "image/jpeg",
                new byte[0]
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> fileStorageService.saveAvatar(file)
        );

        assertThat(ex.getMessage()).isEqualTo("Файл не выбран");
    }

    @Test
    void saveAvatar_tooLargeFile_throwsException() {
        MockMultipartFile file = new MockMultipartFile(
                "avatar",
                "photo.jpg",
                "image/jpeg",
                new byte[6 * 1024 * 1024]
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> fileStorageService.saveAvatar(file)
        );

        assertThat(ex.getMessage()).isEqualTo("Файл слишком большой. Максимум 5 МБ");
    }

    @Test
    void saveAvatar_invalidType_throwsException() {
        MockMultipartFile file = new MockMultipartFile(
                "avatar",
                "photo.gif",
                "image/gif",
                new byte[]{1, 2, 3}
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> fileStorageService.saveAvatar(file)
        );

        assertThat(ex.getMessage()).contains("Недопустимый формат файла");
    }

    @Test
    void saveAvatar_nullContentType_throwsException() {
        MockMultipartFile file = new MockMultipartFile(
                "avatar",
                "photo.jpg",
                null,
                new byte[]{1, 2, 3}
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> fileStorageService.saveAvatar(file)
        );

        assertThat(ex.getMessage()).contains("Недопустимый формат файла");
    }

    @Test
    void saveDocument_validPdf_returnsPathAndCreatesFile() {
        MockMultipartFile file = new MockMultipartFile(
                "doc",
                "passport.pdf",
                "application/pdf",
                new byte[]{1, 2, 3}
        );

        String path = fileStorageService.saveDocument(file, "documents");

        assertThat(path).startsWith("/uploads/documents/").endsWith(".pdf");

        String fileName = path.substring("/uploads/documents/".length());
        Path savedFile = tempDir.resolve("documents").resolve(fileName);
        assertThat(Files.exists(savedFile)).isTrue();
    }

    @Test
    void saveDocument_validPng_returnsPath() {
        MockMultipartFile file = new MockMultipartFile(
                "doc",
                "scan.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        String path = fileStorageService.saveDocument(file, "documents");

        assertThat(path).startsWith("/uploads/documents/").endsWith(".png");
    }

    @Test
    void saveDocument_invalidType_throwsException() {
        MockMultipartFile file = new MockMultipartFile(
                "doc",
                "doc.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new byte[]{1, 2, 3}
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> fileStorageService.saveDocument(file, "documents")
        );

        assertThat(ex.getMessage()).contains("Недопустимый формат файла");
    }

    @Test
    void saveCarImage_whenFileIsNull_returnsNull() {
        String path = fileStorageService.saveCarImage(null);

        assertThat(path).isNull();
    }

    @Test
    void saveCarImage_whenFileIsEmpty_returnsNull() {
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "car.png",
                "image/png",
                new byte[0]
        );

        String path = fileStorageService.saveCarImage(file);

        assertThat(path).isNull();
    }

    @Test
    void saveCarImage_withValidImage_returnsPathAndCreatesFile() {
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "car.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        String path = fileStorageService.saveCarImage(file);

        assertThat(path).startsWith("/uploads/cars/").endsWith(".png");

        String fileName = path.substring("/uploads/cars/".length());
        Path savedFile = tempDir.resolve("cars").resolve(fileName);
        assertThat(Files.exists(savedFile)).isTrue();
    }

    @Test
    void saveCarImage_withInvalidType_throwsException() {
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "car.gif",
                "image/gif",
                new byte[]{1, 2, 3}
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> fileStorageService.saveCarImage(file)
        );

        assertThat(ex.getMessage()).contains("Недопустимый формат файла");
    }

    @Test
    void loadAsResourceFromStoredPath_whenValidPath_returnsResource() throws Exception {
        Path documentsDir = tempDir.resolve("documents");
        Files.createDirectories(documentsDir);

        Path file = documentsDir.resolve("test.pdf");
        Files.write(file, new byte[]{1, 2, 3});

        var resource = fileStorageService.loadAsResourceFromStoredPath("/uploads/documents/test.pdf");

        assertThat(resource).isNotNull();
        assertThat(resource.exists()).isTrue();
        assertThat(resource.getFilename()).isEqualTo("test.pdf");
    }

    @Test
    void loadAsResourceFromStoredPath_whenStoredPathIsNull_throwsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> fileStorageService.loadAsResourceFromStoredPath(null)
        );

        assertThat(ex.getMessage()).isEqualTo("Некорректный путь к файлу");
    }

    @Test
    void loadAsResourceFromStoredPath_whenStoredPathIsBlank_throwsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> fileStorageService.loadAsResourceFromStoredPath(" ")
        );

        assertThat(ex.getMessage()).isEqualTo("Некорректный путь к файлу");
    }

    @Test
    void loadAsResourceFromStoredPath_whenPathDoesNotStartWithUploads_throwsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> fileStorageService.loadAsResourceFromStoredPath("/documents/test.pdf")
        );

        assertThat(ex.getMessage()).isEqualTo("Некорректный путь к файлу");
    }

    @Test
    void loadAsResourceFromStoredPath_whenPathEscapesUploadRoot_throwsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> fileStorageService.loadAsResourceFromStoredPath("/uploads/../../secret.txt")
        );

        assertThat(ex.getMessage()).isEqualTo("Недопустимый путь к файлу");
    }

    @Test
    void loadAsResourceFromStoredPath_whenFileMissing_throwsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> fileStorageService.loadAsResourceFromStoredPath("/uploads/documents/missing.pdf")
        );

        assertThat(ex.getMessage()).isEqualTo("Файл не найден");
    }

    @Test
    void getFileNameFromStoredPath_returnsFileName() {
        String fileName = fileStorageService.getFileNameFromStoredPath("/uploads/documents/test.pdf");

        assertThat(fileName).isEqualTo("test.pdf");
    }

    @Test
    void getFileNameFromStoredPath_whenPathIsNull_throwsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> fileStorageService.getFileNameFromStoredPath(null)
        );

        assertThat(ex.getMessage()).isEqualTo("Путь к файлу пустой");
    }

    @Test
    void getFileNameFromStoredPath_whenPathIsBlank_throwsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> fileStorageService.getFileNameFromStoredPath(" ")
        );

        assertThat(ex.getMessage()).isEqualTo("Путь к файлу пустой");
    }

    @Test
    void getExtension_returnsOriginalExtension_whenFilenameContainsDot() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("image.jpeg");

        String extension = ReflectionTestUtils.invokeMethod(fileStorageService, "getExtension", file);

        assertThat(extension).isEqualTo(".jpeg");
    }

    @Test
    void getExtension_returnsJpg_whenNoExtensionAndJpegContentType() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("image");
        when(file.getContentType()).thenReturn("image/jpeg");

        String extension = ReflectionTestUtils.invokeMethod(fileStorageService, "getExtension", file);

        assertThat(extension).isEqualTo(".jpg");
    }

    @Test
    void getExtension_returnsPng_whenNoExtensionAndPngContentType() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("image");
        when(file.getContentType()).thenReturn("image/png");

        String extension = ReflectionTestUtils.invokeMethod(fileStorageService, "getExtension", file);

        assertThat(extension).isEqualTo(".png");
    }

    @Test
    void getExtension_returnsWebp_whenNoExtensionAndWebpContentType() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("image");
        when(file.getContentType()).thenReturn("image/webp");

        String extension = ReflectionTestUtils.invokeMethod(fileStorageService, "getExtension", file);

        assertThat(extension).isEqualTo(".webp");
    }

    @Test
    void getExtension_returnsPdf_whenNoExtensionAndPdfContentType() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("document");
        when(file.getContentType()).thenReturn("application/pdf");

        String extension = ReflectionTestUtils.invokeMethod(fileStorageService, "getExtension", file);

        assertThat(extension).isEqualTo(".pdf");
    }

    @Test
    void getExtension_returnsBin_whenContentTypeIsNull() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn(null);
        when(file.getContentType()).thenReturn(null);

        String extension = ReflectionTestUtils.invokeMethod(fileStorageService, "getExtension", file);

        assertThat(extension).isEqualTo(".bin");
    }

    @Test
    void getExtension_returnsBin_whenContentTypeUnknown() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("file");
        when(file.getContentType()).thenReturn("application/octet-stream");

        String extension = ReflectionTestUtils.invokeMethod(fileStorageService, "getExtension", file);

        assertThat(extension).isEqualTo(".bin");
    }

    @Test
    void copy_whenInputStreamThrows_throwsRuntimeException() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenThrow(new IOException("boom"));

        Path dest = tempDir.resolve("test.bin");

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> ReflectionTestUtils.invokeMethod(fileStorageService, "copy", file, dest)
        );

        assertThat(ex.getMessage()).isEqualTo("Ошибка сохранения файла");
    }

    @Test
    void resolveAndCreate_whenCreateDirectoriesFails_throwsRuntimeException() {
        Path dir = Paths.get(tempDir.toString(), "avatars").toAbsolutePath();

        try (MockedStatic<Files> filesMock = mockStatic(Files.class, CALLS_REAL_METHODS)) {
            filesMock.when(() -> Files.createDirectories(dir))
                    .thenThrow(new IOException("cannot create dir"));

            RuntimeException ex = assertThrows(
                    RuntimeException.class,
                    () -> ReflectionTestUtils.invokeMethod(fileStorageService, "resolveAndCreate", "avatars")
            );

            assertThat(ex.getMessage()).contains("Не удалось создать папку");
        }
    }

    @Test
    void saveAvatar_whenFileIsNull_throwsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> fileStorageService.saveAvatar(null)
        );

        assertThat(ex.getMessage()).isEqualTo("Файл не выбран");
    }

    @Test
    void saveAvatar_whenOriginalFilenameProducesBlankSlug_returnsOnlySuffixWithExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "avatar",
                "!!!.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        String path = fileStorageService.saveAvatar(file);

        assertThat(path).startsWith("/uploads/avatars/");
        assertThat(path).matches("/uploads/avatars/[a-f0-9]{8}\\.png");

        String fileName = path.substring("/uploads/avatars/".length());
        Path savedFile = tempDir.resolve("avatars").resolve(fileName);
        assertThat(Files.exists(savedFile)).isTrue();
    }

    @Test
    void loadAsResourceFromStoredPath_whenPathPointsToDirectory_throwsException() throws Exception {
        Path documentsDir = tempDir.resolve("documents");
        Files.createDirectories(documentsDir);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> fileStorageService.loadAsResourceFromStoredPath("/uploads/documents")
        );

        assertThat(ex.getMessage()).isEqualTo("Файл не найден");
    }

    @Test
    void slugify_whenOriginalIsNull_returnsEmptyString() {
        String result = ReflectionTestUtils.invokeMethod(fileStorageService, "slugify", (String) null);

        assertThat(result).isEmpty();
    }

    @Test
    void slugify_whenOriginalIsBlank_returnsEmptyString() {
        String result = ReflectionTestUtils.invokeMethod(fileStorageService, "slugify", "   ");

        assertThat(result).isEmpty();
    }

    @Test
    void slugify_whenSlugIsLong_truncatesToMaxLength() {
        String original = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.png";

        String result = ReflectionTestUtils.invokeMethod(fileStorageService, "slugify", original);

        assertThat(result).hasSize(40);
        assertThat(result).isEqualTo("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    }

    @Test
    void slugify_whenSlugIsLongAndEndsWithDashAfterTruncate_removesTrailingDash() {
        String original = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa-.png";

        String result = ReflectionTestUtils.invokeMethod(fileStorageService, "slugify", original);

        assertThat(result).isEqualTo("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    }
}