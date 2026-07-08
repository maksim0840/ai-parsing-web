package io.github.maksim0840.apigateway.service;

import io.github.maksim0840.apigateway.grpc.ExtractionResultGrpcClient;
import io.github.maksim0840.apigateway.mapper.JsonStringMapper;
import io.github.maksim0840.internalapi.extraction_result.v1.dto.ExtractionResultDTO;
import io.github.maksim0840.internalapi.extraction_result.v1.enums.ResultFormat;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class ExtractionResultRemoteService {

    private final ExtractionResultGrpcClient grpcClient;

    public ExtractionResultRemoteService(ExtractionResultGrpcClient grpcClient) {
        this.grpcClient = grpcClient;
    }

    public ExtractionResultDTO createExtractionResult(String url, String userId, String jsonStr) {
        Map<String, Object> jsonResult;
        jsonResult = JsonStringMapper.stringToMap(jsonStr);
        return grpcClient.create(url, userId, jsonResult);
    }

    public ExtractionResultDTO getExtractionResultById(String id, ResultFormat resultFormat) {
        return grpcClient.get(id, resultFormat);
    }

    public List<ExtractionResultDTO> getListExtractionResultByPageWithFiltering(String userId, Instant dateFrom, Instant dateTo, int pageNum, int pageSize, Boolean isSortDesc, ResultFormat resultFormat) {
        return grpcClient.getList(userId, dateFrom, dateTo, pageNum, pageSize, isSortDesc, resultFormat);
    }

    public long getExtractionResultsNumberByFiltering(String userId, Instant dateFrom, Instant dateTo) {
        return grpcClient.count(userId, dateFrom, dateTo);
    }

    public String getMergedListExtractionResultWithFormatMapping(String userId, Instant dateFrom, Instant dateTo, int pageNum, int pageSize, Boolean isSortDesc, ResultFormat resultFormat) {
        return grpcClient.getMergedList(userId, dateFrom, dateTo, pageNum, pageSize, isSortDesc, resultFormat);
    }

    public void deleteExtractionResultById(String id) {
        grpcClient.delete(id);
    }
}
