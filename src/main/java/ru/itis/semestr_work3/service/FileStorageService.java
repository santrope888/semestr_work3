package ru.itis.semestr_work3.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final long MAX_SIZE = 5 * 1024 * 1024;

    private static final List<String> ALLOWED_IMAGE_TYPES =
            List.of("image/jpeg", "image/png", "image/webp");

    private static final List<String> ALLOWED_DOC_TYPES =
            List.of("image/jpeg", "image/png", "image/webp", "application/pdf");

    private static final int MAX_SLUG_LENGTH = 40;
    private static final int SUFFIX_LENGTH = 8;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public String saveAvatar(MultipartFile file) {
        validate(file, ALLOWED_IMAGE_TYPES);
        String filename = generateFilename(file);
        Path dest = resolveAndCreate("avatars").resolve(filename);
        copy(file, dest);
        return "/uploads/avatars/" + filename;
    }

    public String saveDocument(MultipartFile file, String subFolder) {
        validate(file, ALLOWED_DOC_TYPES);
        String filename = generateFilename(file);
        Path dest = resolveAndCreate(subFolder).resolve(filename);
        copy(file, dest);
        return "/uploads/" + subFolder + "/" + filename;
    }

    public String saveCarImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        validate(file, ALLOWED_IMAGE_TYPES);
        String filename = generateFilename(file);
        Path dest = resolveAndCreate("cars").resolve(filename);
        copy(file, dest);
        return "/uploads/cars/" + filename;
    }

    public Resource loadAsResourceFromStoredPath(String storedPath) {
        if (storedPath == null || storedPath.isBlank() || !storedPath.startsWith("/uploads/")) {
            throw new IllegalArgumentException("Некорректный путь к файлу");
        }

        String relativePath = storedPath.substring("/uploads/".length());

        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path file = root.resolve(relativePath).normalize();

        if (!file.startsWith(root)) {
            throw new IllegalArgumentException("Недопустимый путь к файлу");
        }

        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Файл не найден");
        }

        return new PathResource(file);
    }

    public String getFileNameFromStoredPath(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            throw new IllegalArgumentException("Путь к файлу пустой");
        }
        return Paths.get(storedPath).getFileName().toString();
    }

    private void validate(MultipartFile file, List<String> allowed) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл не выбран");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("Файл слишком большой. Максимум 5 МБ");
        }
        String contentType = file.getContentType();
        if (contentType == null || !allowed.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Недопустимый формат файла. Разрешены: " + allowed);
        }
    }

    private Path resolveAndCreate(String sub) {
        Path dir = Paths.get(uploadDir, sub).toAbsolutePath();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось создать папку: " + dir, e);
        }
        return dir;
    }

    private void copy(MultipartFile file, Path dest) {
        try {
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Ошибка сохранения файла", e);
        }
    }

    private String getExtension(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            return original.substring(original.lastIndexOf('.')).toLowerCase();
        }
        String ct = file.getContentType();
        if (ct == null) {
            return ".bin";
        }
        return switch (ct) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "application/pdf" -> ".pdf";
            default -> ".bin";
        };
    }

    private String generateFilename(MultipartFile file) {
        String extension = getExtension(file);
        String original = file.getOriginalFilename();
        String slug = slugify(original);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, SUFFIX_LENGTH);

        return slug.isBlank()
                ? suffix + extension
                : slug + "-" + suffix + extension;
    }

    private String slugify(String original) {
        if (original == null || original.isBlank()) {
            return "";
        }
        int dotIndex = original.lastIndexOf('.');
        String baseName = dotIndex > 0 ? original.substring(0, dotIndex) : original;

        String slug = baseName.toLowerCase()
                .replaceAll("[^a-z0-9_-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-+|-+$", "");

        if (slug.length() > MAX_SLUG_LENGTH) {
            slug = slug.substring(0, MAX_SLUG_LENGTH);
            slug = slug.replaceAll("-+$", "");
        }

        return slug;
    }
}