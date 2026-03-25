package ru.itis.semestr_work3.controllers.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itis.semestr_work3.entity.Favorite;
import ru.itis.semestr_work3.service.FavoriteService;

import java.util.List;

@RestController
@RequestMapping("/favorite")
public class FavoriteController {
    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public List<Favorite> findAll() {
        return favoriteService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Favorite> findById(@PathVariable Long id) {
        return favoriteService.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Favorite create(@Valid @RequestBody Favorite favorite) {
        return favoriteService.save(favorite);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Favorite> update(@PathVariable Long id, @Valid @RequestBody Favorite favorite) {
        return favoriteService.update(id, favorite).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        return favoriteService.deleteById(id) ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}
