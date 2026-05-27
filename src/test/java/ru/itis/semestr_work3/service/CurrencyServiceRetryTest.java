package ru.itis.semestr_work3.service;

import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(classes = {
        CurrencyService.class,
        CurrencyServiceRetryTest.RetryTestConfig.class
})
class CurrencyServiceRetryTest {

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private RestTemplate restTemplate;

    @Test
    void convert_whenRestClientFails_retriesAndReturnsOriginalAmount() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new RestClientException("exchange unavailable"));

        double result = currencyService.convert(1500, "USD");

        assertThat(AopUtils.isAopProxy(currencyService)).isTrue();
        assertThat(result).isEqualTo(1500.0);
        verify(restTemplate, times(3))
                .getForObject("https://api.exchangerate-api.com/v4/latest/RUB", Map.class);
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
