package ru.itis.semestr_work3.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itis.semestr_work3.entity.Car;
import ru.itis.semestr_work3.entity.ChatMessage;
import ru.itis.semestr_work3.entity.ChatSession;
import ru.itis.semestr_work3.entity.User;
import ru.itis.semestr_work3.exception.ExternalServiceException;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.repository.CarRepository;
import ru.itis.semestr_work3.repository.ChatMessageRepository;
import ru.itis.semestr_work3.repository.ChatSessionRepository;
import ru.itis.semestr_work3.specifications.CarSpecifications;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final CarRepository carRepository;
    private final OllamaClientService ollamaClientService;

    @Value("${ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${ollama.model:llama3}")
    private String ollamaModel;

    @Transactional
    public ChatSession createSession(User user, String title) {
        ChatSession session = new ChatSession();
        session.setUser(user);
        session.setTitle(title != null ? title : "Новый диалог");
        session.setCreatedAt(LocalDateTime.now());
        return sessionRepository.save(session);
    }

    @Transactional
    public void deleteSession(Long sessionId, Long userId) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Чат-сессия не найдена"));

        if (!session.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Доступ запрещён");
        }

        sessionRepository.delete(session);
    }

    @Transactional(readOnly = true)
    public List<ChatSession> getUserSessions(Long userId) {
        return sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public ChatSession findSessionById(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Чат-сессия не найдена"));
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getSessionMessages(Long sessionId) {
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    @Transactional
    public String sendMessage(Long sessionId, String userMessage) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Чат-сессия не найдена"));

        List<ChatMessage> oldMessages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        boolean isFirstMessage = oldMessages.isEmpty();

        saveMessage(session, "USER", userMessage);

        if (isFirstMessage && shouldAutoRename(session.getTitle())) {
            session.setTitle(generateTitleFromMessage(userMessage));
            sessionRepository.save(session);
        }

        String aiResponse = callOllama(session);

        saveMessage(session, "ASSISTANT", aiResponse);
        return aiResponse;
    }

    private boolean shouldAutoRename(String title) {
        return title == null
                || title.isBlank()
                || title.equals("Новый диалог")
                || title.equals("Подбор автомобиля");
    }

    private String generateTitleFromMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Новый диалог";
        }

        String clean = message
                .replaceAll("\\s+", " ")
                .trim();

        int maxLength = 38;

        if (clean.length() <= maxLength) {
            return clean;
        }

        return clean.substring(0, maxLength).trim() + "...";
    }

    private String callOllama(ChatSession session) {
        try {
            List<Car> cars = carRepository.findAll(CarSpecifications.isAvailable(true));

            String carsInfo = cars.stream()
                    .map(c -> String.format(
                            "%s %s %d — %d мест, %s, %s, %d руб/день, категория: %s",
                            c.getBrand(),
                            c.getModel(),
                            c.getYear(),
                            c.getSeats(),
                            c.getTransmission(),
                            c.getEngine(),
                            c.getPricePerDay(),
                            c.getCategory() != null ? c.getCategory().getName() : "без категории"
                    ))
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("Нет доступных автомобилей");

            String systemPrompt = """
                    Ты — AuraBot, ИИ-помощник сайта Aura Motum по аренде автомобилей.

                    Общайся естественно, дружелюбно и уверенно. Не отвечай сухо.
                    Подбирай машины под запрос пользователя и кратко объясняй, почему они подходят.

                    Ограничения:
                    - не используй markdown-таблицы;
                    - используй только автомобили из каталога ниже;
                    - не выдумывай характеристики, которых нет в каталоге;
                    - не пиши технические параметры, если они не указаны в каталоге;
                    - не перегружай ответ: лучше 2–4 хорошие рекомендации, чем длинный список;
                    - отвечай на русском языке.

                    Каталог автомобилей:
                    %s
                    """.formatted(carsInfo);

            List<ChatMessage> history = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));

            for (ChatMessage msg : history) {
                messages.add(Map.of(
                        "role", msg.getRole().toLowerCase(),
                        "content", msg.getContent()
                ));
            }

            Map<String, Object> body = new HashMap<>();
            body.put("model", ollamaModel);
            body.put("messages", messages);
            body.put("stream", false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = ollamaClientService.chat(
                    ollamaUrl + "/api/chat",
                    request
            );

            if (response.getBody() == null) {
                throw new ExternalServiceException("Ollama вернул пустой ответ");
            }

            Object messageObj = response.getBody().get("message");
            if (!(messageObj instanceof Map<?, ?> message)) {
                throw new ExternalServiceException("Ollama вернул некорректный формат ответа");
            }

            Object content = message.get("content");
            if (content == null || content.toString().isBlank()) {
                throw new ExternalServiceException("Ollama не вернул текст ответа");
            }

            return content.toString();

        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при обращении к Ollama: {}", e.getMessage(), e);
            throw new ExternalServiceException(
                    "ИИ-помощник временно недоступен. Попробуйте позже.",
                    e
            );
        }
    }

    private void saveMessage(ChatSession session, String role, String content) {
        ChatMessage message = new ChatMessage();
        message.setSession(session);
        message.setRole(role);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        messageRepository.save(message);
    }
}
