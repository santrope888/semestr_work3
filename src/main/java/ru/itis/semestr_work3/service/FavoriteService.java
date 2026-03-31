package ru.itis.semestr_work3.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itis.semestr_work3.entity.Car;
import ru.itis.semestr_work3.entity.Favorite;
import ru.itis.semestr_work3.entity.User;
import ru.itis.semestr_work3.repository.FavoriteRepository;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;

    public List<Favorite> findByUser(Long userId) {
        return favoriteRepository.findByUser(userId);
    }

    public boolean isFavorite(Long userId, Long carId) {
        return favoriteRepository.exists(userId, carId);
    }

    public Favorite add(User user, Car car) {
        if (favoriteRepository.exists(user.getId(), car.getId())) {
            throw new IllegalArgumentException("Уже в избранном");
        }
        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setCar(car);
        favorite.setCreatedAt(LocalDate.now());
        return favoriteRepository.save(favorite);
    }

    @Transactional
    public void remove(Long userId, Long carId) {
        favoriteRepository.remove(userId, carId);
    }
}