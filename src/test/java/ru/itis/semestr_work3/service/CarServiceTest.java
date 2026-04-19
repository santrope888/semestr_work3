package ru.itis.semestr_work3.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.itis.semestr_work3.dto.CarFilter;
import ru.itis.semestr_work3.entity.Car;
import ru.itis.semestr_work3.entity.Category;
import ru.itis.semestr_work3.repository.CarRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarServiceTest {

    @Mock
    private CarRepository carRepository;

    @InjectMocks
    private CarService carService;

    private Car car;
    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setId(10L);
        category.setName("SUV");

        car = new Car();
        car.setId(1L);
        car.setBrand("Toyota");
        car.setModel("RAV4");
        car.setYear(2020);
        car.setColor("Black");
        car.setPricePerDay(4500);
        car.setSeats(5);
        car.setTransmission("AT");
        car.setEngine("Hybrid");
        car.setDrive("AWD");
        car.setDescription("Comfortable crossover");
        car.setAvailable(true);
        car.setCategory(category);
        car.setCreatedAt(LocalDate.now());
        car.setImagePath("/old.png");
    }

    @Test
    void findCars_withNullFilterAndNullPageable_usesDefaults() {
        when(carRepository.findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Car>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(car), PageRequest.of(0, 12), 1));

        Page<Car> result = carService.findCars(null, null);

        assertThat(result.getContent()).containsExactly(car);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(carRepository).findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Car>>any(), captor.capture());
        Pageable usedPageable = captor.getValue();
        assertThat(usedPageable.getPageNumber()).isEqualTo(0);
        assertThat(usedPageable.getPageSize()).isEqualTo(12);
        assertThat(usedPageable.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void findCars_withInvalidPageSize_fallsBackToDefaultSize() {
        Pageable pageable = mock(Pageable.class);
        when(pageable.getPageNumber()).thenReturn(2);
        when(pageable.getPageSize()).thenReturn(0);
        when(carRepository.findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Car>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(car), PageRequest.of(2, 12), 1));

        carService.findCars(new CarFilter(), pageable);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(carRepository).findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Car>>any(), captor.capture());
        Pageable usedPageable = captor.getValue();
        assertThat(usedPageable.getPageNumber()).isEqualTo(2);
        assertThat(usedPageable.getPageSize()).isEqualTo(12);
    }

    @Test
    void findCars_withPriceAscSort_appliesPriceAsc() {
        assertSort("priceAsc", "pricePerDay", Sort.Direction.ASC);
    }

    @Test
    void findCars_withPriceDescSort_appliesPriceDesc() {
        assertSort("priceDesc", "pricePerDay", Sort.Direction.DESC);
    }

    @Test
    void findCars_withYearDescSort_appliesYearDesc() {
        assertSort("yearDesc", "year", Sort.Direction.DESC);
    }

    @Test
    void findCars_withYearAscSort_appliesYearAsc() {
        assertSort("yearAsc", "year", Sort.Direction.ASC);
    }

    @Test
    void findCars_withBrandAscSort_appliesBrandAsc() {
        assertSort("brandAsc", "brand", Sort.Direction.ASC);
    }

    @Test
    void findCars_withModelAscSort_appliesModelAsc() {
        assertSort("modelAsc", "model", Sort.Direction.ASC);
    }

    @Test
    void findCars_withUnknownSort_fallsBackToCreatedAtDesc() {
        assertSort("unknown", "createdAt", Sort.Direction.DESC);
    }

    @Test
    void findCars_withBlankSort_fallsBackToCreatedAtDesc() {
        assertSort("   ", "createdAt", Sort.Direction.DESC);
    }

    @Test
    void findDistinctBrands_filtersBlankValuesRemovesDuplicatesAndSorts() {
        Car second = new Car();
        second.setBrand("audi");
        Car third = new Car();
        third.setBrand("Toyota");
        Car fourth = new Car();
        fourth.setBrand("");
        Car fifth = new Car();
        fifth.setBrand(null);

        when(carRepository.findAll()).thenReturn(List.of(car, second, third, fourth, fifth));

        List<String> result = carService.findDistinctBrands();

        assertThat(result).containsExactly("audi", "Toyota");
    }

    @Test
    void findDistinctColors_filtersBlankValuesRemovesDuplicatesAndSorts() {
        Car second = new Car();
        second.setColor("blue");
        Car third = new Car();
        third.setColor("Black");
        Car fourth = new Car();
        fourth.setColor("");
        Car fifth = new Car();
        fifth.setColor(null);

        when(carRepository.findAll()).thenReturn(List.of(car, second, third, fourth, fifth));

        List<String> result = carService.findDistinctColors();

        assertThat(result).containsExactly("Black", "blue");
    }

    @Test
    void findAll_returnsAllCars() {
        when(carRepository.findAll()).thenReturn(List.of(car));

        assertThat(carService.findAll()).containsExactly(car);
    }

    @Test
    void findById_whenExists_returnsCar() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));

        assertThat(carService.findById(1L)).contains(car);
    }

    @Test
    void findById_whenMissing_returnsEmpty() {
        when(carRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(carService.findById(99L)).isEmpty();
    }

    @Test
    void findAvailable_returnsOnlyAvailableCars() {
        when(carRepository.findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Car>>any()))
                .thenReturn(List.of(car));

        List<Car> result = carService.findAvailable();

        assertThat(result).containsExactly(car);
    }

    @Test
    void create_setsCreatedAtAndAvailableAndSaves() {
        car.setAvailable(false);
        car.setCreatedAt(null);
        when(carRepository.save(any(Car.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Car result = carService.create(car);

        assertThat(result.getAvailable()).isTrue();
        assertThat(result.getCreatedAt()).isEqualTo(LocalDate.now());
    }

    @Test
    void update_whenCarExistsAndImageProvided_updatesAllFieldsIncludingImage() {
        Car updateData = new Car();
        updateData.setBrand("BMW");
        updateData.setModel("X6");
        updateData.setYear(2024);
        updateData.setColor("White");
        updateData.setPricePerDay(9000);
        updateData.setSeats(4);
        updateData.setTransmission("MT");
        updateData.setEngine("Petrol");
        updateData.setDrive("RWD");
        updateData.setDescription("Updated description");
        updateData.setAvailable(false);
        updateData.setCategory(category);
        updateData.setImagePath("/new.png");

        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(carRepository.save(any(Car.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Car result = carService.update(1L, updateData);

        assertThat(result.getBrand()).isEqualTo("BMW");
        assertThat(result.getModel()).isEqualTo("X6");
        assertThat(result.getYear()).isEqualTo(2024);
        assertThat(result.getColor()).isEqualTo("White");
        assertThat(result.getPricePerDay()).isEqualTo(9000);
        assertThat(result.getSeats()).isEqualTo(4);
        assertThat(result.getTransmission()).isEqualTo("MT");
        assertThat(result.getEngine()).isEqualTo("Petrol");
        assertThat(result.getDrive()).isEqualTo("RWD");
        assertThat(result.getDescription()).isEqualTo("Updated description");
        assertThat(result.getAvailable()).isFalse();
        assertThat(result.getImagePath()).isEqualTo("/new.png");
    }

    @Test
    void update_whenImageIsNull_keepsOldImagePath() {
        Car updateData = new Car();
        updateData.setBrand("BMW");
        updateData.setModel("X6");
        updateData.setYear(2024);
        updateData.setColor("White");
        updateData.setPricePerDay(9000);
        updateData.setSeats(4);
        updateData.setTransmission("MT");
        updateData.setEngine("Petrol");
        updateData.setDrive("RWD");
        updateData.setDescription("Updated description");
        updateData.setAvailable(false);
        updateData.setCategory(category);
        updateData.setImagePath(null);

        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(carRepository.save(any(Car.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Car result = carService.update(1L, updateData);

        assertThat(result.getImagePath()).isEqualTo("/old.png");
    }

    @Test
    void update_whenCarMissing_throwsException() {
        when(carRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> carService.update(1L, car));

        assertThat(ex.getMessage()).isEqualTo("Автомобиль не найден: 1");
        verify(carRepository, never()).save(any(Car.class));
    }

    @Test
    void delete_whenCarExists_deletesById() {
        when(carRepository.existsById(1L)).thenReturn(true);

        carService.delete(1L);

        verify(carRepository).deleteById(1L);
    }

    @Test
    void delete_whenCarMissing_throwsException() {
        when(carRepository.existsById(1L)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> carService.delete(1L));

        assertThat(ex.getMessage()).isEqualTo("Автомобиль не найден: 1");
        verify(carRepository, never()).deleteById(1L);
    }

    private void assertSort(String sortBy, String expectedProperty, Sort.Direction expectedDirection) {
        CarFilter filter = new CarFilter();
        filter.setSortBy(sortBy);
        PageRequest pageable = PageRequest.of(1, 5);
        when(carRepository.findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Car>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(car), pageable, 1));

        carService.findCars(filter, pageable);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(carRepository).findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Car>>any(), captor.capture());
        Pageable usedPageable = captor.getValue();
        Sort.Order order = usedPageable.getSort().getOrderFor(expectedProperty);
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(expectedDirection);
    }
}
