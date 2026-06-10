package io.github.maksim0840.extractionresults.mapper;

import io.github.maksim0840.extractionresults.entity.ExtractionResult;
import io.github.maksim0840.internalapi.extraction_result.v1.dto.ExtractionResultDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.*;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface ExtractionResultMapper {

    ExtractionResult toEntity(ExtractionResultDTO dto);
    ExtractionResultDTO toDto(ExtractionResult entity);
}
