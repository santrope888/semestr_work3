package ru.itis.semestr_work3.controllers.api;

import org.springframework.web.bind.annotation.*;
import ru.itis.semestr_work3.entity.Notification;
import ru.itis.semestr_work3.service.NotificationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/user/{userId}")
    public List<Notification> findByUser(@PathVariable Long userId) {
        return notificationService.findByUser(userId);
    }

    @GetMapping("/user/{userId}/unread")
    public List<Notification> findUnread(@PathVariable Long userId) {
        return notificationService.findUnread(userId);
    }

    @GetMapping("/user/{userId}/count")
    public Map<String, Long> countUnread(@PathVariable Long userId) {
        return Map.of("count", notificationService.countUnread(userId));
    }

    @PatchMapping("/{id}/read")
    public void markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
    }
}