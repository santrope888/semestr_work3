package ru.itis.semestr_work3.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.itis.semestr_work3.entity.User;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.service.GridFsDocumentService;
import ru.itis.semestr_work3.service.UserService;

@Controller
@RequiredArgsConstructor
public class DocumentController {

    private final UserService userService;
    private final GridFsDocumentService gridFsDocumentService;

    @GetMapping("/profile/documents/{docType}")
    public ResponseEntity<Resource> getMyDocument(@PathVariable String docType,
                                                  @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new AccessDeniedException("Необходима аутентификация");
        }

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        return buildResponse(resolveDocumentId(user, docType));
    }

    @GetMapping("/admin/users/{userId}/documents/{docType}")
    public ResponseEntity<Resource> getUserDocumentForAdmin(@PathVariable Long userId,
                                                            @PathVariable String docType,
                                                            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null || userDetails.getAuthorities().stream()
                .noneMatch(a -> "ADMIN".equals(a.getAuthority()))) {
            throw new AccessDeniedException("Доступ запрещён");
        }

        User user = userService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        return buildResponse(resolveDocumentId(user, docType));
    }

    private String resolveDocumentId(User user, String docType) {
        return switch (docType) {
            case "license" -> {
                if (user.getLicensePath() == null || user.getLicensePath().isBlank()) {
                    throw new ResourceNotFoundException("Водительское удостоверение не загружено");
                }
                yield user.getLicensePath();
            }
            case "passport" -> {
                if (user.getPassportPath() == null || user.getPassportPath().isBlank()) {
                    throw new ResourceNotFoundException("Паспорт не загружен");
                }
                yield user.getPassportPath();
            }
            default -> throw new IllegalArgumentException("Неизвестный тип документа: " + docType);
        };
    }

    private ResponseEntity<Resource> buildResponse(String fileId) {
        GridFsResource resource = gridFsDocumentService.load(fileId);

        MediaType mediaType = resolveMediaType(resource);
        String fileName = resource.getFilename() != null ? resource.getFilename() : "document";

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(resource);
    }

    private MediaType resolveMediaType(GridFsResource resource) {
        try {
            String contentType = resource.getContentType();
            return contentType != null
                    ? MediaType.parseMediaType(contentType)
                    : MediaType.APPLICATION_OCTET_STREAM;
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}