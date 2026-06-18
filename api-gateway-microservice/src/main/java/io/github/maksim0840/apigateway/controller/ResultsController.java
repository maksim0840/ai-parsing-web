package io.github.maksim0840.apigateway.controller;

import io.github.maksim0840.apigateway.dto.api.ResultResponse;
import io.github.maksim0840.apigateway.dto.api.ResultSaveApiRequest;
import io.github.maksim0840.apigateway.dto.api.ResultsWithFilteringResponse;
import io.github.maksim0840.apigateway.mapper.ApiResultDTOMapper;
import io.github.maksim0840.apigateway.security.JwtPrincipal;
import io.github.maksim0840.apigateway.service.ExtractionResultRemoteService;
import io.github.maksim0840.internalapi.extraction_result.v1.dto.ExtractionResultDTO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/results")
public class ResultsController {

    private final ExtractionResultRemoteService extractionResultRemoteService;

    public ResultsController(ExtractionResultRemoteService extractionResultRemoteService) {
        this.extractionResultRemoteService = extractionResultRemoteService;
    }

    @PostMapping("/save")
    public ResultResponse saveResult(@RequestBody ResultSaveApiRequest request, @AuthenticationPrincipal JwtPrincipal principal) {
        String userId = String.valueOf(principal.userId());
        ExtractionResultDTO resultDTO = extractionResultRemoteService.createExtractionResult(request.url(), userId, request.result());
        return ApiResultDTOMapper.dtoToApi(resultDTO);
    }

    @GetMapping("/{id}")
    public ResultResponse getResultById(@PathVariable String id) {
        ExtractionResultDTO resultDTO = extractionResultRemoteService.getExtractionResultById(id);
        return ApiResultDTOMapper.dtoToApi(resultDTO);
    }

    @GetMapping
    public ResultsWithFilteringResponse getResultsByFilter(
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo,
            @RequestParam(defaultValue = "0") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Boolean isSortDesc,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        String userId = String.valueOf(principal.userId());

        List<ExtractionResultDTO> resultsDTO = extractionResultRemoteService.getListExtractionResultByPageWithFiltering(userId, dateFrom, dateTo, pageNum, pageSize, isSortDesc);
        long numberOfRecords = extractionResultRemoteService.getExtractionResultsNumberByFiltering(userId, dateFrom, dateTo);

        return new ResultsWithFilteringResponse(ApiResultDTOMapper.dtoToApiList(resultsDTO), numberOfRecords);
    }

    @DeleteMapping("/{id}")
    public void deleteResultById(@PathVariable String id) {
        extractionResultRemoteService.deleteExtractionResultById(id);
    }
}
