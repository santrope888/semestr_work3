package ru.itis.semestr_work3.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import ru.itis.semestr_work3.entity.Category;
import ru.itis.semestr_work3.repository.CategoryRepository;

@Component
@RequiredArgsConstructor
public class StringToCategoryConverter implements Converter<String, Category> {

    private final CategoryRepository categoryRepository;

    @Override
    public Category convert(String source) {
        if (source == null || source.isBlank()) return null;
        try {
            Long id = Long.parseLong(source.trim());
            return categoryRepository.findById(id).orElse(null);
        } catch (NumberFormatException e) {
            return categoryRepository.findByName(source.trim()).orElse(null);
        }
    }
}