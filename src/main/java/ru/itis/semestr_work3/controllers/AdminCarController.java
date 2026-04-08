package ru.itis.semestr_work3.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.itis.semestr_work3.entity.Car;
import ru.itis.semestr_work3.entity.Category;
import ru.itis.semestr_work3.service.CarService;
import ru.itis.semestr_work3.service.CategoryService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/cars")
public class AdminCarController {

    private final CarService carService;
    private final CategoryService categoryService;

    @GetMapping
    public String listCars(Model model) {
        model.addAttribute("cars", carService.findAll());
        return "admin/cars";
    }

    @GetMapping("/new")
    public String newCarForm(Model model) {
        model.addAttribute("car", new Car());
        model.addAttribute("categories", categoryService.findAll());
        return "admin/car-form";
    }

    @PostMapping
    public String createCar(@RequestParam String brand,
                            @RequestParam String model,
                            @RequestParam Integer year,
                            @RequestParam String color,
                            @RequestParam Integer pricePerDay,
                            @RequestParam Integer seats,
                            @RequestParam String transmission,
                            @RequestParam String engine,
                            @RequestParam String drive,
                            @RequestParam(required = false) String description,
                            @RequestParam Long categoryId,
                            @RequestParam(required = false) MultipartFile image) throws IOException {

        Car car = new Car();
        fillCarFields(car, brand, model, year, color, pricePerDay, seats,
                transmission, engine, drive, description, categoryId);

        String imagePath = saveImage(image);
        if (imagePath != null) {
            car.setImagePath(imagePath);
        }

        carService.create(car);
        return "redirect:/admin/cars";
    }

    @GetMapping("/{id}/edit")
    public String editCarForm(@PathVariable Long id, Model model) {
        Car car = carService.findById(id)
                .orElseThrow(() -> new RuntimeException("Автомобиль не найден: " + id));

        model.addAttribute("car", car);
        model.addAttribute("categories", categoryService.findAll());
        return "admin/car-form";
    }

    @PostMapping("/{id}/edit")
    public String updateCar(@PathVariable Long id,
                            @RequestParam String brand,
                            @RequestParam String model,
                            @RequestParam Integer year,
                            @RequestParam String color,
                            @RequestParam Integer pricePerDay,
                            @RequestParam Integer seats,
                            @RequestParam String transmission,
                            @RequestParam String engine,
                            @RequestParam String drive,
                            @RequestParam(required = false) String description,
                            @RequestParam Long categoryId,
                            @RequestParam(required = false) MultipartFile image) throws IOException {

        Car existingCar = carService.findById(id)
                .orElseThrow(() -> new RuntimeException("Автомобиль не найден: " + id));

        Car carData = new Car();
        fillCarFields(carData, brand, model, year, color, pricePerDay, seats,
                transmission, engine, drive, description, categoryId);

        carData.setAvailable(existingCar.getAvailable());
        carData.setImagePath(existingCar.getImagePath());

        String newImagePath = saveImage(image);
        if (newImagePath != null) {
            carData.setImagePath(newImagePath);
        }

        carService.update(id, carData);
        return "redirect:/admin/cars";
    }

    @PostMapping("/{id}/delete")
    public String deleteCar(@PathVariable Long id) {
        carService.delete(id);
        return "redirect:/admin/cars";
    }

    private void fillCarFields(Car car,
                               String brand,
                               String model,
                               Integer year,
                               String color,
                               Integer pricePerDay,
                               Integer seats,
                               String transmission,
                               String engine,
                               String drive,
                               String description,
                               Long categoryId) {

        Category category = categoryService.findAll().stream()
                .filter(cat -> cat.getId().equals(categoryId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Категория не найдена: " + categoryId));

        car.setBrand(brand);
        car.setModel(model);
        car.setYear(year);
        car.setColor(color);
        car.setPricePerDay(pricePerDay);
        car.setSeats(seats);
        car.setTransmission(transmission);
        car.setEngine(engine);
        car.setDrive(drive);
        car.setDescription(description);
        car.setCategory(category);
    }

    private String saveImage(MultipartFile image) throws IOException {
        if (image == null || image.isEmpty()) {
            return null;
        }

        String originalFilename = StringUtils.cleanPath(image.getOriginalFilename());
        String extension = "";

        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalFilename.substring(dotIndex);
        }

        String newFilename = UUID.randomUUID() + extension;

        Path uploadDir = Path.of("images", "cars");
        Files.createDirectories(uploadDir);

        Path filePath = uploadDir.resolve(newFilename);
        Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return "/images/cars/" + newFilename;
    }
}