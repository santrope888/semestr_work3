package ru.itis.semestr_work3.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itis.semestr_work3.entity.Car;
import ru.itis.semestr_work3.entity.Favorite;
import ru.itis.semestr_work3.entity.User;
import ru.itis.semestr_work3.repository.FavoriteRepository;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @InjectMocks
    private FavoriteService favoriteService;

    private User user;
    private Car car;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        car = new Car();
        car.setId(2L);
    }

    @Test
    void findByUser_returnsFavorites() {
        Favorite favorite = new Favorite();
        when(favoriteRepository.findByUserId(1L)).thenReturn(List.of(favorite));

        assertThat(favoriteService.findByUser(1L)).containsExactly(favorite);
    }

    @Test
    void isFavorite_whenExists_returnsTrue() {
        when(favoriteRepository.exists(1L, 2L)).thenReturn(true);

        assertThat(favoriteService.isFavorite(1L, 2L)).isTrue();
    }

    @Test
    void isFavorite_whenMissing_returnsFalse() {
        when(favoriteRepository.exists(1L, 2L)).thenReturn(false);

        assertThat(favoriteService.isFavorite(1L, 2L)).isFalse();
    }

    @Test
    void add_whenFavoriteMissing_createsFavorite() {
        when(favoriteRepository.exists(1L, 2L)).thenReturn(false);
        when(favoriteRepository.save(any(Favorite.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Favorite result = favoriteService.add(user, car);

        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getCar()).isEqualTo(car);
        assertThat(result.getCreatedAt()).isEqualTo(LocalDate.now());
    }

    @Test
    void add_whenFavoriteAlreadyExists_throwsException() {
        when(favoriteRepository.exists(1L, 2L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> favoriteService.add(user, car));
        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    void remove_callsRepositoryDeleteQuery() {
        favoriteService.remove(1L, 2L);

        verify(favoriteRepository).remove(1L, 2L);
    }
}
