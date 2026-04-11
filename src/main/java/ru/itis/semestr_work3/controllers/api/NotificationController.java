package ru.itis.semestr_work3.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.itis.semestr_work3.entity.Notification;
import ru.itis.semestr_work3.service.NotificationService;

import java.util.List;
import java.util.Map;

@Tag(name = "Notifications", description = "Уведомления пользователя")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Все уведомления пользователя")
    @GetMapping("/user/{userId}")
    public List<Notification> findByUser(@PathVariable Long userId) {
        return notificationService.findByUser(userId);
    }

    @Operation(summary = "Непрочитанные уведомления")
    @GetMapping("/user/{userId}/unread")
    public List<Notification> findUnread(@PathVariable Long userId) {
        return notificationService.findUnread(userId);
    }

    @Operation(summary = "Количество непрочитанных уведомлений")
    @GetMapping("/user/{userId}/count")
    public Map<String, Long> countUnread(@PathVariable Long userId) {
        return Map.of("count", notificationService.countUnread(userId));
    }

    @Operation(summary = "Отметить уведомление как прочитанное")
    @PatchMapping("/{id}/read")
    public void markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
    }
}