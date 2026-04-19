package ru.itis.semestr_work3.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import ru.itis.semestr_work3.converter.NotificationMapper;
import ru.itis.semestr_work3.dto.NotificationDto;
import ru.itis.semestr_work3.entity.Notification;
import ru.itis.semestr_work3.entity.User;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.service.NotificationService;
import ru.itis.semestr_work3.service.UserService;

import java.util.List;
import java.util.Map;

@Tag(name = "Notifications", description = "Уведомления пользователя")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;
    private final UserService userService;

    @Operation(summary = "Все уведомления текущего пользователя")
    @GetMapping("/my")
    public List<NotificationDto> findMy(@AuthenticationPrincipal UserDetails principal) {
        User user = resolveUser(principal);
        return notificationMapper.toDtoList(notificationService.findByUser(user.getId()));
    }

    @Operation(summary = "Количество непрочитанных уведомлений")
    @GetMapping("/count")
    public Map<String, Long> countUnread(@AuthenticationPrincipal UserDetails principal) {
        User user = resolveUser(principal);
        return Map.of("count", notificationService.countUnread(user.getId()));
    }

    @Operation(summary = "Отметить уведомление как прочитанное")
    @PatchMapping("/{id}/read")
    public void markAsRead(@PathVariable Long id,
                           @AuthenticationPrincipal UserDetails principal) {
        Notification notification = notificationService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Уведомление не найдено"));
        if (!notification.getUser().getUsername().equals(principal.getUsername())) {
            throw new AccessDeniedException("Доступ запрещён");
        }
        notificationService.markAsRead(id);
    }

    @Operation(summary = "Прочитать все уведомления")
    @PatchMapping("/read-all")
    public void markAllRead(@AuthenticationPrincipal UserDetails principal) {
        User user = resolveUser(principal);
        notificationService.markAllRead(user.getId());
    }

    private User resolveUser(UserDetails principal) {
        return userService.findByUsername(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }
}