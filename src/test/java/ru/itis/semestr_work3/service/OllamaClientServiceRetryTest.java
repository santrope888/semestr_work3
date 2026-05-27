package ru.itis.semestr_work3.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ru.itis.semestr_work3.exception.ExternalServiceException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(classes = {
        OllamaClientService.class,
        OllamaClientServiceRetryTest.RetryTestConfig.class
})
class OllamaClientServiceRetryTest {

    @Autowired
    private OllamaClientService ollamaClientService;

    @Autowired
    private RestTemplate restTemplate;

    @BeforeEach
    void resetMocks() {
        reset(restTemplate);
    }

    @Test
    void chat_whenOllamaResponds_returnsResponse() {
        String url = "http://localhost:11434/api/chat";
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(Map.of("stream", false));
        ResponseEntity<Map> response = ResponseEntity.ok(Map.of("message", Map.of("content", "OK")));
        when(restTemplate.postForEntity(url, request, Map.class)).thenReturn(response);

        ResponseEntity<Map> result = ollamaClientService.chat(url, request);

        assertThat(result).isSameAs(response);
        verify(restTemplate).postForEntity(url, request, Map.class);
    }

    @Test
    void chat_whenOllamaUnavailable_retriesAndRecovers() {
        String url = "http://localhost:11434/api/chat";
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(Map.of("stream", false));
        when(restTemplate.postForEntity(url, request, Map.class))
                .thenThrow(new RestClientException("connection refused"));

        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> ollamaClientService.chat(url, request)
        );

        assertThat(AopUtils.isAopProxy(ollamaClientService)).isTrue();
        assertThat(exception.getMessage()).isEqualTo("ИИ-помощник временно недоступен. Попробуйте позже.");
        assertThat(exception.getCause()).isInstanceOf(RestClientException.class);
        verify(restTemplate, times(3)).postForEntity(url, request, Map.class);
    }

    @TestConfiguration
    @EnableRetry
    static class RetryTestConfig {

        @Bean
        RestTemplate restTemplate() {
            return mock(RestTemplate.class);
        }
    }
}
