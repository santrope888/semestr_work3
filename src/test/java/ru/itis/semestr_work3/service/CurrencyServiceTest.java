package ru.itis.semestr_work3.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import ru.itis.semestr_work3.exception.ExternalServiceException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private CurrencyService currencyService;

    @Test
    void convert_whenTargetCurrencyIsRub_returnsSameAmountWithoutApiCall() {
        double result = currencyService.convert(1000, "RUB");

        assertThat(result).isEqualTo(1000.0);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void convert_whenTargetCurrencyIsLowercaseRub_returnsSameAmount() {
        double result = currencyService.convert(1000, "rub");

        assertThat(result).isEqualTo(1000.0);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void convert_whenRateExists_returnsRoundedConvertedAmount() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("rates", Map.of("USD", 0.01556)));

        double result = currencyService.convert(1000, "USD");

        assertThat(result).isEqualTo(15.56);
        verify(restTemplate).getForObject("https://api.exchangerate-api.com/v4/latest/RUB", Map.class);
    }

    @Test
    void convert_whenCurrencyIsBlank_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> currencyService.convert(100, "   "));
    }

    @Test
    void convert_whenCurrencyIsNull_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> currencyService.convert(100, null));
    }

    @Test
    void convert_whenAmountIsNegative_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> currencyService.convert(-1, "USD"));
    }

    @Test
    void convert_whenRateMissing_throwsException() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("rates", Map.of("EUR", 0.01)));

        assertThrows(IllegalArgumentException.class, () -> currencyService.convert(100, "USD"));
    }

    @Test
    void convert_whenResponseHasNoRates_throwsException() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("base", "RUB"));

        assertThrows(IllegalArgumentException.class, () -> currencyService.convert(100, "USD"));
    }

    @Test
    void convert_whenResponseIsNull_throwsException() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> currencyService.convert(100, "USD"));
    }

    @Test
    void convert_whenApiThrowsRuntimeException_wrapsIt() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenThrow(new RuntimeException("boom"));

        assertThrows(ExternalServiceException.class, () -> currencyService.convert(100, "USD"));
    }
}
