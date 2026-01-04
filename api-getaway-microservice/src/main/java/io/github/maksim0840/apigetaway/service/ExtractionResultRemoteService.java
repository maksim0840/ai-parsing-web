package io.github.maksim0840.apigetaway.service;

import io.github.maksim0840.apigetaway.dto.ExtractionResultDTO;
import io.github.maksim0840.apigetaway.grpc.ExtractionResultGrpcClient;
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

    public ExtractionResultDTO createExtractionResult(String url, String userId, Map<String, Object> jsonResult) {
        return grpcClient.create(url, userId, jsonResult);
    }

    public ExtractionResultDTO getExtractionResultById(String id) {
        return grpcClient.get(id);
    }

    public List<ExtractionResultDTO> getListExtractionResultByPageWithFiltering(String userId, Instant dateFrom, Instant dateTo, int pageNum, int pageSize, Boolean isSortDesc) {
        return grpcClient.getList(userId, dateFrom, dateTo, pageNum, pageSize, isSortDesc);
    }

    public void deleteExtractionResultById(String id) {
        grpcClient.delete(id);
    }
}
