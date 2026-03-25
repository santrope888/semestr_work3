package ru.itis.semestr_work3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.itis.semestr_work3.entity.ChatSession;

import java.util.List;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    @Query("SELECT s FROM ChatSession s WHERE s.user.id = :userId ORDER BY s.createdAt DESC")
    List<ChatSession> findByUser(@Param("userId") Long userId);
}