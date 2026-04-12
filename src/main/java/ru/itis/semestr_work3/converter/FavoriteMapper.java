package ru.itis.semestr_work3.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.itis.semestr_work3.dto.FavoriteDto;
import ru.itis.semestr_work3.entity.Favorite;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FavoriteMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "car.id", target = "carId")
    @Mapping(source = "car.brand", target = "carBrand")
    @Mapping(source = "car.model", target = "carModel")
    @Mapping(source = "car.imagePath", target = "carImagePath")
    @Mapping(source = "car.pricePerDay", target = "carPricePerDay")
    FavoriteDto toDto(Favorite favorite);

    List<FavoriteDto> toDtoList(List<Favorite> favorites);
}