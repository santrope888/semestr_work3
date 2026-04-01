package ru.itis.semestr_work3.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.itis.semestr_work3.entity.Car;
import ru.itis.semestr_work3.entity.ChatMessage;
import ru.itis.semestr_work3.entity.ChatSession;
import ru.itis.semestr_work3.entity.User;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.repository.CarRepository;
import ru.itis.semestr_work3.repository.ChatMessageRepository;
import ru.itis.semestr_work3.repository.ChatSessionRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final CarRepository carRepository;
    private final RestTemplate restTemplate;

    @Value("${ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${ollama.model:llama3}")
    private String ollamaModel;

    public ChatSession createSession(User user, String title) {
        ChatSession session = new ChatSession();
        session.setUser(user);
        session.setTitle(title != null ? title : "Новый диалог");
        session.setCreatedAt(LocalDateTime.now());
        return sessionRepository.save(session);
    }

    public List<ChatSession> getUserSessions(Long userId) {
        return sessionRepository.findByUser(userId);
    }

    public Optional<ChatSession> findSessionById(Long sessionId) {
        return sessionRepository.findById(sessionId);
    }

    public List<ChatMessage> getSessionMessages(Long sessionId) {
        return messageRepository.findBySession(sessionId);
    }

    public String sendMessage(Long sessionId, String userMessage) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Чат-сессия не найдена"));

        saveMessage(session, "USER", userMessage);
        String aiResponse = callOllama(session);
        saveMessage(session, "ASSISTANT", aiResponse);

        return aiResponse;
    }

    private String callOllama(ChatSession session) {
        try {
            List<Car> cars = carRepository.findAvailable();

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
                    .collect(Collectors.joining("\n"));

            String systemPrompt = "Ты — ИИ-помощник по подбору автомобилей в аренду. "
                    + "Вот список доступных автомобилей:\n"
                    + carsInfo
                    + "\n\nРекомендуй пользователю подходящие автомобили из этого списка, "
                    + "объясняя свой выбор. Отвечай на русском языке.";

            List<ChatMessage> history = messageRepository.findBySession(session.getId());

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

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    ollamaUrl + "/api/chat",
                    request,
                    Map.class
            );

            if (response.getBody() != null && response.getBody().containsKey("message")) {
                Map<String, Object> message = (Map<String, Object>) response.getBody().get("message");
                return (String) message.get("content");
            }

            return "Извините, не удалось получить ответ. Попробуйте ещё раз.";

        } catch (Exception e) {
            log.error("Ошибка при обращении к Ollama: {}", e.getMessage(), e);
            return "Извините, ИИ-помощник временно недоступен. Попробуйте позже.";
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