package ru.itis.semestr_work3.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import ru.itis.semestr_work3.entity.Car;
import ru.itis.semestr_work3.entity.Category;
import ru.itis.semestr_work3.entity.ChatMessage;
import ru.itis.semestr_work3.entity.ChatSession;
import ru.itis.semestr_work3.entity.User;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.repository.CarRepository;
import ru.itis.semestr_work3.repository.ChatMessageRepository;
import ru.itis.semestr_work3.repository.ChatSessionRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatServiceTest {

    @Mock
    private ChatSessionRepository sessionRepository;
    @Mock
    private ChatMessageRepository messageRepository;
    @Mock
    private CarRepository carRepository;
    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AiChatService aiChatService;

    private User user;
    private ChatSession session;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aiChatService, "ollamaUrl", "http://localhost:11434");
        ReflectionTestUtils.setField(aiChatService, "ollamaModel", "llama3");

        user = new User();
        user.setId(1L);
        user.setUsername("askar");

        session = new ChatSession();
        session.setId(10L);
        session.setUser(user);
        session.setTitle("Мой чат");
        session.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void createSession_withExplicitTitle_savesSession() {
        when(sessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatSession result = aiChatService.createSession(user, "Подбор авто");

        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getTitle()).isEqualTo("Подбор авто");
        assertNotNull(result.getCreatedAt());
        verify(sessionRepository).save(any(ChatSession.class));
    }

    @Test
    void createSession_withNullTitle_usesDefaultTitle() {
        when(sessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatSession result = aiChatService.createSession(user, null);

        assertThat(result.getTitle()).isEqualTo("Новый диалог");
    }

    @Test
    void getUserSessions_returnsSessions() {
        when(sessionRepository.findByUser(1L)).thenReturn(List.of(session));

        List<ChatSession> result = aiChatService.getUserSessions(1L);

        assertThat(result).containsExactly(session);
    }

    @Test
    void findSessionById_whenExists_returnsSession() {
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));

        ChatSession result = aiChatService.findSessionById(10L);

        assertThat(result).isEqualTo(session);
    }

    @Test
    void findSessionById_whenMissing_throwsException() {
        when(sessionRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> aiChatService.findSessionById(10L));
    }

    @Test
    void getSessionMessages_returnsMessages() {
        ChatMessage message = new ChatMessage();
        message.setSession(session);
        message.setRole("USER");
        message.setContent("Привет");

        when(messageRepository.findBySession(10L)).thenReturn(List.of(message));

        List<ChatMessage> result = aiChatService.getSessionMessages(10L);

        assertThat(result).containsExactly(message);
    }

    @Test
    void sendMessage_whenSessionMissing_throwsException() {
        when(sessionRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> aiChatService.sendMessage(10L, "Подбери авто"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendMessage_success_savesUserAndAssistantMessages() {
        Category category = new Category();
        category.setName("SUV");

        Car firstCar = new Car();
        firstCar.setBrand("Audi");
        firstCar.setModel("Q8");
        firstCar.setYear(2024);
        firstCar.setSeats(5);
        firstCar.setTransmission("AT");
        firstCar.setEngine("Diesel");
        firstCar.setPricePerDay(12000);
        firstCar.setCategory(category);

        Car secondCar = new Car();
        secondCar.setBrand("BMW");
        secondCar.setModel("X6");
        secondCar.setYear(2023);
        secondCar.setSeats(5);
        secondCar.setTransmission("AT");
        secondCar.setEngine("Hybrid");
        secondCar.setPricePerDay(15000);
        secondCar.setCategory(null);

        ChatMessage historyMessage = new ChatMessage();
        historyMessage.setRole("USER");
        historyMessage.setContent("Хочу премиальный кроссовер");

        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(carRepository.findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Car>>any()))
                .thenReturn(List.of(firstCar, secondCar));
        when(messageRepository.findBySession(10L)).thenReturn(List.of(historyMessage));
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("message", Map.of("content", "Подойдут Audi Q8 и BMW X6"))));
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String result = aiChatService.sendMessage(10L, "Что посоветуешь?");

        assertThat(result).isEqualTo("Подойдут Audi Q8 и BMW X6");

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messageRepository, times(2)).save(captor.capture());
        List<ChatMessage> savedMessages = captor.getAllValues();

        assertThat(savedMessages).hasSize(2);
        assertThat(savedMessages.get(0).getRole()).isEqualTo("USER");
        assertThat(savedMessages.get(0).getContent()).isEqualTo("Что посоветуешь?");
        assertNotNull(savedMessages.get(0).getCreatedAt());

        assertThat(savedMessages.get(1).getRole()).isEqualTo("ASSISTANT");
        assertThat(savedMessages.get(1).getContent()).isEqualTo("Подойдут Audi Q8 и BMW X6");
        assertNotNull(savedMessages.get(1).getCreatedAt());

        verify(restTemplate).postForEntity(eq("http://localhost:11434/api/chat"), any(), eq(Map.class));
    }

    @Test
    void sendMessage_whenResponseBodyIsNull_returnsFallbackMessage() {
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(carRepository.findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Car>>any()))
                .thenReturn(List.<Car>of());
        when(messageRepository.findBySession(10L)).thenReturn(List.<ChatMessage>of());
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenReturn(ResponseEntity.ok(null));
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String result = aiChatService.sendMessage(10L, "Привет");

        assertThat(result).isEqualTo("Извините, не удалось получить ответ. Попробуйте ещё раз.");
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendMessage_whenMessageHasNoContent_returnsFallbackMessage() {
        Map<String, Object> message = new HashMap<>();
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);

        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(carRepository.findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Car>>any()))
                .thenReturn(List.<Car>of());
        when(messageRepository.findBySession(10L)).thenReturn(new ArrayList<>());
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenReturn(ResponseEntity.ok(body));
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String result = aiChatService.sendMessage(10L, "Привет");

        assertThat(result).isEqualTo("Извините, не удалось получить ответ. Попробуйте ещё раз.");
    }

    @Test
    void sendMessage_whenOllamaFails_returnsServiceUnavailableMessage() {
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(carRepository.findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Car>>any()))
                .thenReturn(List.<Car>of());
        when(messageRepository.findBySession(10L)).thenReturn(List.<ChatMessage>of());
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("Connection refused"));
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String result = aiChatService.sendMessage(10L, "Привет");

        assertThat(result).isEqualTo("Извините, ИИ-помощник временно недоступен. Попробуйте позже.");
    }
}
