package ru.itis.semestr_work3.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itis.semestr_work3.entity.Notification;
import ru.itis.semestr_work3.entity.User;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.repository.NotificationRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void findByUser_returnsNotifications() {
        Notification notification = new Notification();
        when(notificationRepository.findByUser(1L)).thenReturn(List.of(notification));

        assertThat(notificationService.findByUser(1L)).containsExactly(notification);
    }

    @Test
    void findUnread_returnsUnreadNotifications() {
        Notification notification = new Notification();
        when(notificationRepository.findUnread(1L)).thenReturn(List.of(notification));

        assertThat(notificationService.findUnread(1L)).containsExactly(notification);
    }

    @Test
    void countUnread_returnsCount() {
        when(notificationRepository.countUnread(1L)).thenReturn(5L);

        assertThat(notificationService.countUnread(1L)).isEqualTo(5L);
    }

    @Test
    void markAsRead_whenNotificationExists_marksAndSaves() {
        Notification notification = new Notification();
        notification.setIsRead(false);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(1L);

        assertThat(notification.getIsRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_whenNotificationMissing_throwsException() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.markAsRead(1L));
    }

    @Test
    void send_createsUnreadNotification() {
        User user = new User();
        user.setId(1L);

        notificationService.send(user, "BOOKING", "Бронирование подтверждено");

        verify(notificationRepository).save(any(Notification.class));
    }
}
