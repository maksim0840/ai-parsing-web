package io.github.maksim0840.usersinfo.mapper;

import io.github.maksim0840.internalapi.user.v1.dto.HtmlParserParamsDTO;
import io.github.maksim0840.internalapi.user.v1.dto.HtmlPreprocessingParamsDTO;
import io.github.maksim0840.internalapi.user.v1.dto.LLMParamsDTO;
import io.github.maksim0840.internalapi.user.v1.dto.ParsingParamDTO;
import io.github.maksim0840.usersinfo.entity.ParsingParam;
import io.github.maksim0840.usersinfo.entity.model.HtmlParserParams;
import io.github.maksim0840.usersinfo.entity.model.HtmlPreprocessingParams;
import io.github.maksim0840.usersinfo.entity.model.LLMParams;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ParsingParamMapper {
    ParsingParamDTO toDto(ParsingParam entity);
    @Mapping(target = "user", ignore = true)
    ParsingParam toEntity(ParsingParamDTO dto);

    HtmlParserParamsDTO toDto(HtmlParserParams entity);
    HtmlParserParams toEntity(HtmlParserParamsDTO dto);

    HtmlPreprocessingParamsDTO toDto(HtmlPreprocessingParams entity);
    HtmlPreprocessingParams toEntity(HtmlPreprocessingParamsDTO dto);

    LLMParamsDTO toDto(LLMParams entity);
    LLMParams toEntity(LLMParamsDTO dto);
}
