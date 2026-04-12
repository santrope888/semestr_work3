package ru.itis.semestr_work3.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatSessionDto {
    private Long id;
    private String title;
    private LocalDateTime createdAt;
    private Long userId;
}