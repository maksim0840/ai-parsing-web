package io.github.maksim0840.extractionresults.mapper;

import io.github.maksim0840.extractionresults.entity.ExtractionResult;
import io.github.maksim0840.internalapi.extraction_result.v1.dto.ExtractionResultDTO;
import io.github.maksim0840.internalapi.extraction_result.v1.enums.ResultFormat;

import java.util.List;
import java.util.Map;


public class ExtractionResultMapper {

    public static ExtractionResultDTO entityToDto(ExtractionResult entity, ResultFormat format) {
        String resultStr = switch (format) {
            case JSON -> FormatsMapper.jsonToString(entity.getJsonResult());
            case XML -> FormatsMapper.jsonToXmlString(entity.getJsonResult());
            case CSV -> FormatsMapper.jsonToCsvString(entity.getJsonResult());
        };
        return ExtractionResultDTO.builder()
                .id(entity.getId())
                .url(entity.getUrl())
                .userId(entity.getUserId())
                .resultFormat(format)
                .result(resultStr)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static String mergeEntitiesToStr(List<ExtractionResult> entities, ResultFormat format) {
        List<Map<String, Object>> jsonResults = entities.stream().map(ExtractionResult::getJsonResult).toList();
        return switch (format) {
            case JSON -> FormatsMapper.jsonListToString(jsonResults);
            case XML -> FormatsMapper.jsonListToXmlString(jsonResults);
            case CSV -> FormatsMapper.jsonListToCsvString(jsonResults);
        };
    }
}
