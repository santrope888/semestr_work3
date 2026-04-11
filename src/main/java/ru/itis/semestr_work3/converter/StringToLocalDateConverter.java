package ru.itis.semestr_work3.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class StringToLocalDateConverter implements Converter<String, LocalDate> {

    private static final DateTimeFormatter ISO   = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter RU    = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter SLASH = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public LocalDate convert(String source) {
        if (source == null || source.isBlank()) return null;
        String s = source.trim();
        for (DateTimeFormatter fmt : new DateTimeFormatter[]{ISO, RU, SLASH}) {
            try {
                return LocalDate.parse(s, fmt);
            } catch (DateTimeParseException ignored) {}
        }
        throw new IllegalArgumentException("Не удалось распознать дату: " + source
                + ". Допустимые форматы: yyyy-MM-dd, dd.MM.yyyy, dd/MM/yyyy");
    }
}