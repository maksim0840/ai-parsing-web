package io.github.maksim0840.apigateway.controller;

import io.github.maksim0840.apigateway.dto.api.ResultsWithFilteringResponse;
import io.github.maksim0840.apigateway.mapper.ApiResultDTOMapper;
import io.github.maksim0840.apigateway.security.JwtPrincipal;
import io.github.maksim0840.apigateway.service.ExtractionResultRemoteService;
import io.github.maksim0840.internalapi.extraction_result.v1.dto.ExtractionResultDTO;
import io.github.maksim0840.internalapi.extraction_result.v1.enums.ResultFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@RequestMapping("/api/admin")
public class AdminController {
    private final ExtractionResultRemoteService extractionResultRemoteService;

    public AdminController(ExtractionResultRemoteService extractionResultRemoteService) {
        this.extractionResultRemoteService = extractionResultRemoteService;
    }

    @GetMapping("/results")
    public ResultsWithFilteringResponse getResultsByFilterAllUsers(
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo,
            @RequestParam(defaultValue = "0") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Boolean isSortDesc,
            @RequestParam(defaultValue = "JSON") ResultFormat format
    ) {
        List<ExtractionResultDTO> resultsDTO = extractionResultRemoteService.getListExtractionResultByPageWithFiltering(null, dateFrom, dateTo, pageNum, pageSize, isSortDesc, format);
        long numberOfRecords = extractionResultRemoteService.getExtractionResultsNumberByFiltering(null, dateFrom, dateTo);

        return new ResultsWithFilteringResponse(ApiResultDTOMapper.dtoToApiList(resultsDTO), numberOfRecords);
    }

    @GetMapping("/results/export")
    public ResponseEntity<byte[]> downloadMergedResultsByFilter(
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo,
            @RequestParam(defaultValue = "0") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Boolean isSortDesc,
            @RequestParam(defaultValue = "JSON") ResultFormat format
    ) {
        String mergedResults = extractionResultRemoteService.getMergedListExtractionResultWithFormatMapping(null, dateFrom, dateTo, pageNum, pageSize, isSortDesc, format);
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
