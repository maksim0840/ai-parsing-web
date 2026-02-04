package io.github.maksim0840.apigateway.service;

import io.github.maksim0840.apigateway.dto.ParsingParamDTO;
import io.github.maksim0840.apigateway.grpc.ParsingParamGrpcClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ParsingParamRemoteService {

    private final ParsingParamGrpcClient grpcClient;

    public ParsingParamRemoteService(ParsingParamGrpcClient grpcClient) {
        this.grpcClient = grpcClient;
    }

    public ParsingParamDTO createParsingParam(Long userId, String name, String description) {
        return grpcClient.create(userId, name, description);
    }

    public ParsingParamDTO getParsingParamById(Long id) {
        return grpcClient.get(id);
    }

    public List<ParsingParamDTO> getListParsingParamByPageWithFiltering(Long userId, Instant dateFrom, Instant dateTo, int pageNum, int pageSize, Boolean isSortDesc) {
        return grpcClient.getList(userId, dateFrom, dateTo, pageNum, pageSize, isSortDesc);
    }

    public void deleteParsingParamById(Long id) {
        grpcClient.delete(id);
    }
}
