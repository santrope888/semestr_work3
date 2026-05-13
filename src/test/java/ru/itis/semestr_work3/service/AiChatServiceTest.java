package ru.itis.semestr_work3.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import ru.itis.semestr_work3.entity.Car;
import ru.itis.semestr_work3.entity.Category;
import ru.itis.semestr_work3.entity.ChatMessage;
import ru.itis.semestr_work3.entity.ChatSession;
import ru.itis.semestr_work3.entity.User;
import ru.itis.semestr_work3.exception.ExternalServiceException;
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
import static org.mockito.Mockito.never;
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
    void sendMessage_whenFirstMessageAndUserMessageIsNull_renamesSessionToDefaultTitle() {
        session.setTitle("Новый диалог");

        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(messageRepository.findBySessionIdOrderByCreatedAtAsc(10L))
                .thenReturn(new ArrayList<>())
                .thenReturn(new ArrayList<>());
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(carRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Car>>any())).thenReturn(List.of());
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("message", Map.of("content", "OK"))));

        String result = aiChatService.sendMessage(10L, null);

        assertThat(result).isEqualTo("OK");
        assertThat(session.getTitle()).isEqualTo("Новый диалог");
        verify(sessionRepository).save(session);
    }

    @Test
    void createSession_withNullTitle_usesDefaultTitle() {
        when(sessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatSession result = aiChatService.createSession(user, null);

        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getTitle()).isEqualTo("Новый диалог");
        assertNotNull(result.getCreatedAt());
        verify(sessionRepository).save(any(ChatSession.class));
    }

    @Test
    void deleteSession_whenOwner_deletesSession() {
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));

        aiChatService.deleteSession(10L, 1L);

        verify(sessionRepository).delete(session);
    }

    @Test
    void deleteSession_whenSessionMissing_throwsException() {
        when(sessionRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> aiChatService.deleteSession(10L, 1L));

        verify(sessionRepository, never()).delete(any(ChatSession.class));
    }

    @Test
    void deleteSession_whenUserIsNotOwner_throwsAccessDeniedException() {
        User anotherUser = new User();
        anotherUser.setId(2L);
        session.setUser(anotherUser);

        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));

        assertThrows(AccessDeniedException.class, () -> aiChatService.deleteSession(10L, 1L));

        verify(sessionRepository, never()).delete(any(ChatSession.class));
    }

    @Test
    void getUserSessions_returnsSessions() {
        when(sessionRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(session));

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
        message.setCreatedAt(LocalDateTime.now());

        when(messageRepository.findBySessionIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(message));

        List<ChatMessage> result = aiChatService.getSessionMessages(10L);

        assertThat(result).containsExactly(message);
    }

    @Test
    void sendMessage_whenSessionMissing_throwsException() {
        when(sessionRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> aiChatService.sendMessage(10L, "Подбери авто"));
    }

    @Test
    void sendMessage_success_savesUserAndAssistantMessagesAndCallsOllama() {
        Category category = new Category();
        category.setName("SUV");

        Car firstCar = car("Audi", "Q8", 2024, 5, "AT", "Diesel", 12000, category);
        Car secondCar = car("BMW", "X6", 2023, 5, "AT", "Hybrid", 15000, null);

        ChatMessage historyMessage = message(session, "USER", "Хочу премиальный кроссовер");

        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(messageRepository.findBySessionIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(historyMessage));
        when(carRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Car>>any()))
                .thenReturn(List.of(firstCar, secondCar));
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("message", Map.of("content", "Подойдут Audi Q8 и BMW X6"))));
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String result = aiChatService.sendMessage(10L, "Что посоветуешь?");

        assertThat(result).isEqualTo("Подойдут Audi Q8 и BMW X6");

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messageRepository, times(2)).save(messageCaptor.capture());

        List<ChatMessage> savedMessages = messageCaptor.getAllValues();
        assertThat(savedMessages).hasSize(2);
        assertThat(savedMessages.get(0).getSession()).isEqualTo(session);
        assertThat(savedMessages.get(0).getRole()).isEqualTo("USER");
        assertThat(savedMessages.get(0).getContent()).isEqualTo("Что посоветуешь?");
        assertNotNull(savedMessages.get(0).getCreatedAt());

        assertThat(savedMessages.get(1).getSession()).isEqualTo(session);
        assertThat(savedMessages.get(1).getRole()).isEqualTo("ASSISTANT");
        assertThat(savedMessages.get(1).getContent()).isEqualTo("Подойдут Audi Q8 и BMW X6");
        assertNotNull(savedMessages.get(1).getCreatedAt());

        ArgumentCaptor<HttpEntity> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq("http://localhost:11434/api/chat"), requestCaptor.capture(), eq(Map.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) requestCaptor.getValue().getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("model")).isEqualTo("llama3");
        assertThat(body.get("stream")).isEqualTo(false);
        assertThat(body.get("messages").toString())
                .contains("Audi Q8 2024")
                .contains("категория: SUV")
                .contains("BMW X6 2023")
                .contains("категория: без категории")
                .contains("Хочу премиальный кроссовер");
    }

    @Test
    void sendMessage_whenFirstMessageAndTitleIsNewDialog_renamesSessionToShortMessage() {
        session.setTitle("Новый диалог");
        stubSuccessfulFirstMessage("OK");

        String result = aiChatService.sendMessage(10L, "Хочу кабриолет");

        assertThat(result).isEqualTo("OK");
        assertThat(session.getTitle()).isEqualTo("Хочу кабриолет");
        verify(sessionRepository).save(session);
    }

    @Test
    void sendMessage_whenFirstMessageAndTitleIsNullAndMessageBlank_renamesSessionToDefaultTitle() {
        session.setTitle(null);
        stubSuccessfulFirstMessage("OK");

        String result = aiChatService.sendMessage(10L, "   ");

        assertThat(result).isEqualTo("OK");
        assertThat(session.getTitle()).isEqualTo("Новый диалог");
        verify(sessionRepository).save(session);
    }

    @Test
    void sendMessage_whenFirstMessageAndTitleIsBlank_renamesSessionToTrimmedLongTitle() {
        session.setTitle("   ");
        stubSuccessfulFirstMessage("OK");

        String longMessage = "   Хочу   большой   семейный автомобиль с большим багажником и автоматом   ";
        String result = aiChatService.sendMessage(10L, longMessage);

        assertThat(result).isEqualTo("OK");
        assertThat(session.getTitle()).isEqualTo("Хочу большой семейный автомобиль с бол...");
        verify(sessionRepository).save(session);
    }

    @Test
    void sendMessage_whenFirstMessageAndTitleIsCarSelection_renamesSession() {
        session.setTitle("Подбор автомобиля");
        stubSuccessfulFirstMessage("OK");

        String result = aiChatService.sendMessage(10L, "SUV");

        assertThat(result).isEqualTo("OK");
        assertThat(session.getTitle()).isEqualTo("SUV");
        verify(sessionRepository).save(session);
    }

    @Test
    void sendMessage_whenFirstMessageButTitleIsCustom_doesNotRenameSession() {
        session.setTitle("Мой чат");
        stubSuccessfulFirstMessage("OK");

        String result = aiChatService.sendMessage(10L, "Хочу седан");

        assertThat(result).isEqualTo("OK");
        assertThat(session.getTitle()).isEqualTo("Мой чат");
        verify(sessionRepository, never()).save(session);
    }

    @Test
    void sendMessage_whenNoCarsAvailable_addsFallbackCatalogTextToPrompt() {
        session.setTitle("Мой чат");
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(messageRepository.findBySessionIdOrderByCreatedAtAsc(10L)).thenReturn(new ArrayList<>());
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(carRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Car>>any())).thenReturn(List.of());
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("message", Map.of("content", "Пока нет доступных автомобилей"))));

        String result = aiChatService.sendMessage(10L, "Что есть в наличии?");

        assertThat(result).isEqualTo("Пока нет доступных автомобилей");

        ArgumentCaptor<HttpEntity> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq("http://localhost:11434/api/chat"), requestCaptor.capture(), eq(Map.class));
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) requestCaptor.getValue().getBody();
        assertThat(body.get("messages").toString())
                .contains("Нет доступных автомобилей");
    }

    @Test
    void sendMessage_whenResponseBodyIsNull_throwsExternalServiceException() {
        stubBeforeOllamaCall();
        ResponseEntity<Map> emptyResponse = ResponseEntity.ok(null);
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(emptyResponse);

        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> aiChatService.sendMessage(10L, "Привет")
        );

        assertThat(exception.getMessage()).isEqualTo("Ollama вернул пустой ответ");
        verify(messageRepository, times(1)).save(any(ChatMessage.class));
    }

    @Test
    void sendMessage_whenMessageHasWrongFormat_throwsExternalServiceException() {
        stubBeforeOllamaCall();
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("message", "wrong-format")));

        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> aiChatService.sendMessage(10L, "Привет")
        );

        assertThat(exception.getMessage()).isEqualTo("Ollama вернул некорректный формат ответа");
        verify(messageRepository, times(1)).save(any(ChatMessage.class));
    }

    @Test
    void sendMessage_whenMessageHasNoContent_throwsExternalServiceException() {
        Map<String, Object> message = new HashMap<>();
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);

        stubBeforeOllamaCall();
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(body));

        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> aiChatService.sendMessage(10L, "Привет")
        );

        assertThat(exception.getMessage()).isEqualTo("Ollama не вернул текст ответа");
        verify(messageRepository, times(1)).save(any(ChatMessage.class));
    }

    @Test
    void sendMessage_whenContentIsBlank_throwsExternalServiceException() {
        stubBeforeOllamaCall();
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("message", Map.of("content", "   "))));

        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> aiChatService.sendMessage(10L, "Привет")
        );

        assertThat(exception.getMessage()).isEqualTo("Ollama не вернул текст ответа");
        verify(messageRepository, times(1)).save(any(ChatMessage.class));
    }

    @Test
    void sendMessage_whenOllamaFails_throwsExternalServiceException() {
        stubBeforeOllamaCall();
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> aiChatService.sendMessage(10L, "Привет")
        );

        assertThat(exception.getMessage()).isEqualTo("ИИ-помощник временно недоступен. Попробуйте позже.");
        assertThat(exception.getCause()).isInstanceOf(RuntimeException.class);
        verify(messageRepository, times(1)).save(any(ChatMessage.class));
    }

    private void stubSuccessfulFirstMessage(String aiResponse) {
        List<ChatMessage> storedMessages = new ArrayList<>();

        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(messageRepository.findBySessionIdOrderByCreatedAtAsc(10L)).thenAnswer(invocation -> new ArrayList<>(storedMessages));
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage saved = invocation.getArgument(0);
            storedMessages.add(saved);
            return saved;
        });
        when(carRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Car>>any())).thenReturn(List.of());
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("message", Map.of("content", aiResponse))));
    }

    private void stubBeforeOllamaCall() {
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(messageRepository.findBySessionIdOrderByCreatedAtAsc(10L)).thenReturn(new ArrayList<>());
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(carRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Car>>any())).thenReturn(List.of());
    }

    private Car car(String brand,
                    String model,
                    Integer year,
                    Integer seats,
                    String transmission,
                    String engine,
                    Integer pricePerDay,
                    Category category) {
        Car car = new Car();
        car.setBrand(brand);
        car.setModel(model);
        car.setYear(year);
        car.setSeats(seats);
        car.setTransmission(transmission);
        car.setEngine(engine);
        car.setPricePerDay(pricePerDay);
        car.setCategory(category);
        return car;
    }

    private ChatMessage message(ChatSession session, String role, String content) {
        ChatMessage message = new ChatMessage();
        message.setSession(session);
        message.setRole(role);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        return message;
    }
}
