package ru.itis.semestr_work3.converter;

import org.mapstruct.Mapper;
import ru.itis.semestr_work3.dto.InsuranceDto;
import ru.itis.semestr_work3.entity.Insurance;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface InsuranceMapper {

    InsuranceDto toDto(Insurance insurance);

    List<InsuranceDto> toDtoList(Set<Insurance> insurances);
}