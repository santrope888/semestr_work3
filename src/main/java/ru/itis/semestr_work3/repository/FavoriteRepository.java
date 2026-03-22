package ru.itis.semestr_work3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itis.semestr_work3.entity.Favorite;

public interface FavoriteRepository extends JpaRepository<Favorite,Long> {
}
