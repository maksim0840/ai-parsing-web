package io.github.maksim0840.apigateway.controller;

import io.github.maksim0840.apigateway.dto.api.ResultResponse;
import io.github.maksim0840.apigateway.dto.api.ResultSaveApiRequest;
import io.github.maksim0840.apigateway.dto.api.ResultsWithFilteringResponse;
import io.github.maksim0840.apigateway.mapper.ApiResultDTOMapper;
import io.github.maksim0840.apigateway.mapper.JsonStringMapper;
import io.github.maksim0840.apigateway.security.JwtPrincipal;
import io.github.maksim0840.apigateway.service.ExtractionResultRemoteService;
import io.github.maksim0840.internalapi.extraction_result.v1.dto.ExtractionResultDTO;
import io.github.maksim0840.internalapi.extraction_result.v1.enums.ResultFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/results")
public class ResultsController {

    private final ExtractionResultRemoteService extractionResultRemoteService;

    public ResultsController(ExtractionResultRemoteService extractionResultRemoteService) {
        this.extractionResultRemoteService = extractionResultRemoteService;
    }

    @PostMapping("/save")
    public ResultResponse saveResult(
            @RequestBody ResultSaveApiRequest request,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        String userId = String.valueOf(principal.userId());
        ExtractionResultDTO resultDTO = extractionResultRemoteService.createExtractionResult(request.url(), userId, request.result());
        return ApiResultDTOMapper.dtoToApi(resultDTO);
    }

    @PostMapping("/update/{id}")
    public ResultResponse updateResult(
            @PathVariable String id,
            @RequestBody String jsonResult,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        String userId = String.valueOf(principal.userId());
        ExtractionResultDTO resultDTO = extractionResultRemoteService.updateExtractionResultById(id, userId, jsonResult);
        return ApiResultDTOMapper.dtoToApi(resultDTO);
    }

    @GetMapping("/{id}")
    public ResultResponse getResultById(
            @PathVariable String id,
            @RequestParam(defaultValue = "JSON") ResultFormat format,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        String userId = String.valueOf(principal.userId());

        ExtractionResultDTO resultDTO = extractionResultRemoteService.getExtractionResultById(id, userId, format);
        return ApiResultDTOMapper.dtoToApi(resultDTO);
    }

    @GetMapping
    public ResultsWithFilteringResponse getResultsByFilter(
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo,
            @RequestParam(defaultValue = "0") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Boolean isSortDesc,
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(defaultValue = "JSON") ResultFormat format
    ) {
        String userId = String.valueOf(principal.userId());

        List<ExtractionResultDTO> resultsDTO = extractionResultRemoteService.getListExtractionResultByPageWithFiltering(userId, dateFrom, dateTo, pageNum, pageSize, isSortDesc, format);
        long numberOfRecords = extractionResultRemoteService.getExtractionResultsNumberByFiltering(userId, dateFrom, dateTo);

        return new ResultsWithFilteringResponse(ApiResultDTOMapper.dtoToApiList(resultsDTO), numberOfRecords);
    }

    @DeleteMapping("/{id}")
    public void deleteResultById(
            @PathVariable String id,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        String userId = String.valueOf(principal.userId());
        extractionResultRemoteService.deleteExtractionResultById(id, userId);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> downloadMergedResultsByFilter(
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo,
            @RequestParam(defaultValue = "0") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Boolean isSortDesc,
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(defaultValue = "JSON") ResultFormat format
    ) {
        String userId = String.valueOf(principal.userId());

        String mergedResults = extractionResultRemoteService.getMergedListExtractionResultWithFormatMapping(userId, dateFrom, dateTo, pageNum, pageSize, isSortDesc, format);
        String extension = resolveFileExtension(format);
        MediaType mediaType = resolveMediaType(format);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"result." + extension + "\"")
                .body(mergedResults.getBytes(StandardCharsets.UTF_8));

    }

    private String resolveFileExtension(ResultFormat format) {
        return switch (format) {
            case JSON -> "json";
            case XML -> "xml";
            case CSV -> "csv";
        };
    }

    private MediaType resolveMediaType(ResultFormat format) {
        return switch (format) {
            case JSON -> MediaType.APPLICATION_JSON;
            case XML -> MediaType.APPLICATION_XML;
            case CSV -> MediaType.parseMediaType("text/csv");
        };
    }
}
