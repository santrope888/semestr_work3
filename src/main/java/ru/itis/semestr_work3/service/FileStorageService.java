package ru.itis.semestr_work3.service;

import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public String saveAvatar(MultipartFile file) {
        validate(file, ALLOWED_IMAGE_TYPES);
        String filename = UUID.randomUUID() + getExtension(file);
        Path dest = resolveAndCreate("avatars").resolve(filename);
        copy(file, dest);
        return "/uploads/avatars/" + filename;
    }

    public String saveDocument(MultipartFile file, String subFolder) {
        validate(file, ALLOWED_DOC_TYPES);
        String filename = UUID.randomUUID() + getExtension(file);
        Path dest = resolveAndCreate(subFolder).resolve(filename);
        copy(file, dest);
        return "/uploads/" + subFolder + "/" + filename;
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
            return original.substring(original.lastIndexOf('.'));
        }
        String ct = file.getContentType();
        if (ct == null) return ".bin";
        return switch (ct) {
            case "image/jpeg"       -> ".jpg";
            case "image/png"        -> ".png";
            case "image/webp"       -> ".webp";
            case "application/pdf"  -> ".pdf";
            default                 -> ".bin";
        };
    }
}