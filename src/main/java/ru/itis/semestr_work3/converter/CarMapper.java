package ru.itis.semestr_work3.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.itis.semestr_work3.dto.CarDto;
import ru.itis.semestr_work3.entity.Car;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CarMapper {

    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    CarDto toDto(Car car);

    List<CarDto> toDtoList(List<Car> cars);
}