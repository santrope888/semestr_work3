package ru.itis.semestr_work3.controllers.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itis.semestr_work3.entity.Favorite;
import ru.itis.semestr_work3.repository.FavoriteRepository;

import java.util.List;

@RestController
@RequestMapping("/favorite")
public class FavoriteController {
    private final FavoriteRepository favoriteRepository;

    public FavoriteController(FavoriteRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
    }

    @GetMapping
    public List<Favorite> findAll() {
        return favoriteRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Favorite> findById(@PathVariable Long id) {
        return favoriteRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Favorite create(@Valid @RequestBody Favorite favorite) {
        return favoriteRepository.save(favorite);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Favorite> update(@PathVariable Long id, @Valid @RequestBody Favorite favorite) {
        if (!favoriteRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        Favorite favoriteUpdated = favoriteRepository.save(favorite);
        return ResponseEntity.ok(favoriteUpdated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        if (!favoriteRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        favoriteRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
