package ru.itis.semestr_work3.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@DisplayName("Каталог автомобилей — интеграционный тест")
class CatalogPaginationIT extends IntegrationTestBase {

    @Test
    @DisplayName("/catalog рендерит шаблон catalog с моделью carsPage")
    void catalogPage_rendersWithPageableModel() throws Exception {
        mockMvc.perform(get("/catalog"))
                .andExpect(status().isOk())
                .andExpect(view().name("catalog"))
                .andExpect(model().attributeExists("carsPage"))
                .andExpect(model().attributeExists("cars"))
                .andExpect(model().attributeExists("filter"))
                .andExpect(model().attributeExists("categories"));
    }

    @Test
    @DisplayName("Pageable size=12: первая страница содержит максимум 12 элементов")
    void catalog_defaultPageSize_isTwelve() throws Exception {
        MvcResult result = mockMvc.perform(get("/catalog"))
                .andExpect(status().isOk())
                .andReturn();

        org.springframework.data.domain.Page<?> page =
                (org.springframework.data.domain.Page<?>) result.getModelAndView()
                        .getModel().get("carsPage");

        assertThat(page).isNotNull();
        assertThat(page.getSize())
                .as("Размер страницы должен быть 12 (CATALOG_PAGE_SIZE)")
                .isEqualTo(12);
        assertThat(page.getNumberOfElements())
                .as("На первой странице не больше 12 элементов")
                .isLessThanOrEqualTo(12);
    }

    @Test
    @DisplayName("Pageable: явно указанная size=5 в URL переопределяет дефолт")
    void catalog_customPageSize_isRespected() throws Exception {
        MvcResult result = mockMvc.perform(get("/catalog").param("size", "5"))
                .andExpect(status().isOk())
                .andReturn();

        org.springframework.data.domain.Page<?> page =
                (org.springframework.data.domain.Page<?>) result.getModelAndView()
                        .getModel().get("carsPage");

        assertThat(page.getSize()).isEqualTo(5);
        assertThat(page.getNumberOfElements()).isLessThanOrEqualTo(5);
    }
}
