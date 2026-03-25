package ru.itis.semestr_work3.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import ru.itis.semestr_work3.entity.Favorite;
import ru.itis.semestr_work3.repository.FavoriteRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FavoriteService {
    private FavoriteRepository favoriteRepository;

    public FavoriteService(FavoriteRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
    }

    public List<Favorite> findAll() {
        return favoriteRepository.findAll();
    }

    public Optional<Favorite> findById(Long id) {
        return favoriteRepository.findById(id);
    }

    public Favorite save(Favorite favorite) {
        return favoriteRepository.save(favorite);
    }

    public Optional<Favorite> update(Long id, Favorite favorite) {
        if (!favoriteRepository.existsById(id)) {
            return Optional.empty();
        }
        favorite.setId(id);
        Favorite favoriteUpdated = favoriteRepository.save(favorite);
        return Optional.of(favoriteUpdated);
    }

    public boolean deleteById(Long id) {
        if (!favoriteRepository.existsById(id)) {
            return false;
        }

        favoriteRepository.deleteById(id);

        return true;
    }
}
