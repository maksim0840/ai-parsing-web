package io.github.maksim0840.apigateway.dto.api;

import java.util.List;

public record ResultsWithFilteringResponse(
        List<ResultResponse> pagedData,
        long totalRecords
) {
}
