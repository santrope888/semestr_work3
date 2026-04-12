package ru.itis.semestr_work3.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.itis.semestr_work3.dto.ChatMessageDto;
import ru.itis.semestr_work3.dto.ChatSessionDto;
import ru.itis.semestr_work3.entity.ChatMessage;
import ru.itis.semestr_work3.entity.ChatSession;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ChatMapper {

    @Mapping(source = "user.id", target = "userId")
    ChatSessionDto toDto(ChatSession session);

    List<ChatSessionDto> toSessionDtoList(List<ChatSession> sessions);

    @Mapping(source = "session.id", target = "sessionId")
    ChatMessageDto toDto(ChatMessage message);

    List<ChatMessageDto> toMessageDtoList(List<ChatMessage> messages);
}