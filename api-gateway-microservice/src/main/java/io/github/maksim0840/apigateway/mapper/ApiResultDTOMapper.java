package io.github.maksim0840.apigateway.mapper;

import io.github.maksim0840.apigateway.dto.api.ResultResponse;
import io.github.maksim0840.internalapi.extraction_result.v1.dto.ExtractionResultDTO;

import java.util.List;

public class ApiResultDTOMapper {

    public static ResultResponse dtoToApi(ExtractionResultDTO dto) {
        return new ResultResponse(
                dto.id(),
                dto.url(),
                dto.resultFormat(),
                dto.result(),
                dto.createdAt()
        );
    }

    public static List<ResultResponse> dtoToApiList(List<ExtractionResultDTO> dtoList) {
        return dtoList.stream().map(ApiResultDTOMapper::dtoToApi).toList();
    }
}
