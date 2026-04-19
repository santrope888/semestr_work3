package ru.itis.semestr_work3.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itis.semestr_work3.entity.Category;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.repository.CategoryRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void findAll_returnsCategories() {
        Category category = new Category();
        category.setId(1L);
        category.setName("SUV");
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        List<Category> result = categoryService.findAll();

        assertThat(result).containsExactly(category);
        verify(categoryRepository).findAll();
    }

    @Test
    void findAll_whenNoCategories_returnsEmptyList() {
        when(categoryRepository.findAll()).thenReturn(List.of());

        assertThat(categoryService.findAll()).isEmpty();
    }

    @Test
    void findById_whenExists_returnsCategory() {
        Category category = new Category();
        category.setId(1L);
        category.setName("SUV");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        Category result = categoryService.findById(1L);

        assertThat(result).isEqualTo(category);
        verify(categoryRepository).findById(1L);
    }

    @Test
    void findById_whenMissing_throwsException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoryService.findById(99L));
    }
}