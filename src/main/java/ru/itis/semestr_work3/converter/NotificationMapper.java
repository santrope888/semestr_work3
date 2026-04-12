package ru.itis.semestr_work3.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.itis.semestr_work3.dto.NotificationDto;
import ru.itis.semestr_work3.entity.Notification;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(source = "user.id", target = "userId")
    NotificationDto toDto(Notification notification);

    List<NotificationDto> toDtoList(List<Notification> notifications);
}