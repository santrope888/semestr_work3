package ru.itis.semestr_work3.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ru.itis.semestr_work3.exception.ExternalServiceException;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OllamaClientService {

    private final RestTemplate restTemplate;

    @Retryable(
            retryFor = RestClientException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public ResponseEntity<Map> chat(String url, HttpEntity<Map<String, Object>> request) {
        return restTemplate.postForEntity(url, request, Map.class);
    }

    @Recover
    public ResponseEntity<Map> recover(RestClientException e, String url, HttpEntity<Map<String, Object>> request) {
        log.error("Ollama is unavailable after 3 attempts: {}", e.getMessage(), e);
        throw new ExternalServiceException(
                "ИИ-помощник временно недоступен. Попробуйте позже.",
                e
        );
    }
}
