package ru.itis.semestr_work3.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.itis.semestr_work3.exception.ExternalServiceException;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyService {

    private final RestTemplate restTemplate;

    public double convert(int amountRub, String targetCurrency) {
        if (amountRub < 0) {
            throw new IllegalArgumentException("Сумма не может быть отрицательной");
        }

        if (targetCurrency == null || targetCurrency.isBlank()) {
            throw new IllegalArgumentException("Нужно указать код валюты");
        }

        if ("RUB".equalsIgnoreCase(targetCurrency)) {
            return amountRub;
        }

        try {
            String url = "https://api.exchangerate-api.com/v4/latest/RUB";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("rates")) {
                Map<String, Number> rates = (Map<String, Number>) response.get("rates");
                Number rate = rates.get(targetCurrency.toUpperCase());

                if (rate != null) {
                    return Math.round(amountRub * rate.doubleValue() * 100.0) / 100.0;
                }
            }

            throw new IllegalArgumentException("Курс для " + targetCurrency + " не найден");

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Ошибка при получении курса валют: {}", e.getMessage(), e);
            throw new ExternalServiceException("Сервис конвертации валют временно недоступен", e);
        }
    }
}