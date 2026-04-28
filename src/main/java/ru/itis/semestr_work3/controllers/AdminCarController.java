package ru.itis.semestr_work3.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.itis.semestr_work3.dto.CarForm;
import ru.itis.semestr_work3.entity.Car;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.service.CarService;
import ru.itis.semestr_work3.service.CategoryService;
import ru.itis.semestr_work3.service.FileStorageService;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/cars")
public class AdminCarController {

    private final CarService carService;
    private final CategoryService categoryService;
    private final FileStorageService fileStorageService;

    @GetMapping
    public String listCars(Model model) {
        model.addAttribute("cars", carService.findAll());
        return "admin/cars";
    }

    @GetMapping("/new")
    public String newCarForm(Model model) {
        CarForm form = new CarForm();
        form.setAvailable(true);
        model.addAttribute("carForm", form);
        model.addAttribute("categories", categoryService.findAll());
        return "admin/car-form";
    }

    @PostMapping
    public String createCar(@Valid @ModelAttribute("carForm") CarForm form,
                            BindingResult bindingResult,
                            @RequestParam(required = false) MultipartFile image,
                            Model model,
                            RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            return "admin/car-form";
        }

        Car car = buildCar(form, new Car());
        String imagePath = fileStorageService.saveCarImage(image);
        if (imagePath != null) {
            car.setImagePath(imagePath);
        }

        carService.create(car);
        ra.addFlashAttribute("success", "Автомобиль добавлен");
        return "redirect:/admin/cars";
    }

    @GetMapping("/{id}/edit")
    public String editCarForm(@PathVariable Long id, Model model) {
        Car car = carService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Автомобиль не найден: " + id));

        CarForm form = new CarForm();
        form.setBrand(car.getBrand());
        form.setModel(car.getModel());
        form.setYear(car.getYear());
        form.setColor(car.getColor());
        form.setPricePerDay(car.getPricePerDay());
        form.setSeats(car.getSeats());
        form.setTransmission(car.getTransmission());
        form.setEngine(car.getEngine());
        form.setDrive(car.getDrive());
        form.setDescription(car.getDescription());
        form.setAvailable(car.getAvailable());
        form.setCategoryId(car.getCategory() != null ? car.getCategory().getId() : null);

        model.addAttribute("carForm", form);
        model.addAttribute("carId", id);
        model.addAttribute("currentImagePath", car.getImagePath());
        model.addAttribute("categories", categoryService.findAll());
        return "admin/car-form";
    }

    @PostMapping("/{id}/edit")
    public String updateCar(@PathVariable Long id,
                            @Valid @ModelAttribute("carForm") CarForm form,
                            BindingResult bindingResult,
                            @RequestParam(required = false) MultipartFile image,
                            Model model,
                            RedirectAttributes ra) {
        Car existing = carService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Автомобиль не найден: " + id));

        if (bindingResult.hasErrors()) {
            model.addAttribute("carId", id);
            model.addAttribute("currentImagePath", existing.getImagePath());
            model.addAttribute("categories", categoryService.findAll());
            return "admin/car-form";
        }

        Car carData = buildCar(form, new Car());
        carData.setImagePath(existing.getImagePath());

        String newImagePath = fileStorageService.saveCarImage(image);
        if (newImagePath != null) {
            carData.setImagePath(newImagePath);
        }

        carService.update(id, carData);
        ra.addFlashAttribute("success", "Автомобиль обновлён");
        return "redirect:/admin/cars";
    }

    @PostMapping("/{id}/delete")
    public String deleteCar(@PathVariable Long id, RedirectAttributes ra) {
        carService.delete(id);
        ra.addFlashAttribute("success", "Автомобиль удалён");
        return "redirect:/admin/cars";
    }

    private Car buildCar(CarForm form, Car car) {
        car.setBrand(form.getBrand());
        car.setModel(form.getModel());
        car.setYear(form.getYear());
        car.setColor(form.getColor());
        car.setPricePerDay(form.getPricePerDay());
        car.setSeats(form.getSeats());
        car.setTransmission(form.getTransmission());
        car.setEngine(form.getEngine());
        car.setDrive(form.getDrive());
        car.setDescription(form.getDescription());
        car.setAvailable(form.getAvailable() != null ? form.getAvailable() : true);
        car.setCategory(categoryService.findById(form.getCategoryId()));
        return car;
    }
}