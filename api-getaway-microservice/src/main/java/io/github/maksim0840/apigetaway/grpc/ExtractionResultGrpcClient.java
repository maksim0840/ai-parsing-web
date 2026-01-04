package io.github.maksim0840.apigetaway.grpc;

import io.github.maksim0840.apigetaway.dto.ExtractionResultDTO;
import io.github.maksim0840.apigetaway.exception.DataNotFoundException;
import io.github.maksim0840.apigetaway.exception.DataUnavailableException;
import io.github.maksim0840.apigetaway.mapper.ProtoMapperDTO;
import io.github.maksim0840.extraction_result.v1.*;
import io.github.maksim0840.internalapi.extraction_result.v1.mapper.ProtoJsonMapper;
import io.github.maksim0840.internalapi.extraction_result.v1.mapper.ProtoTimeMapper;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class ExtractionResultGrpcClient {

    @GrpcClient("extractionResults")
    private ExtractionResultServiceGrpc.ExtractionResultServiceBlockingStub blockingStub;

    public ExtractionResultDTO create(String url, String userId, Map<String, Object> jsonResult) {
        CreateExtractionResultRequest request = CreateExtractionResultRequest.newBuilder()
                .setUrl(url)
                .setUserId(userId)
                .setJsonResult(ProtoJsonMapper.mapToStruct(jsonResult))
                .build();

        try {
            CreateExtractionResultResponse response = blockingStub.create(request);
            ExtractionResultProto extractionResultProto = response.getExtractionResult();
            return ProtoMapperDTO.protoToDto(extractionResultProto);
        } catch (StatusRuntimeException e) {
            Status.Code code = e.getStatus().getCode();
            String description = e.getStatus().getDescription();

            switch (code) {
                case INVALID_ARGUMENT -> throw new IllegalArgumentException(description, e);
                case UNAVAILABLE -> throw new DataUnavailableException(description, e);
                default -> throw new RuntimeException();
            }
        }
    }

    public ExtractionResultDTO get(String id) {
        GetExtractionResultRequest request = GetExtractionResultRequest.newBuilder()
                .setId(id)
                .build();

        try {
            GetExtractionResultResponse response = blockingStub.get(request);
            ExtractionResultProto extractionResultProto = response.getExtractionResult();
            return ProtoMapperDTO.protoToDto(extractionResultProto);
        } catch (StatusRuntimeException e) {
            Status.Code code = e.getStatus().getCode();
            String description = e.getStatus().getDescription();

            switch (code) {
                case INVALID_ARGUMENT -> throw new IllegalArgumentException(description, e);
                case NOT_FOUND -> throw new DataNotFoundException(description, e);
                case UNAVAILABLE -> throw new DataUnavailableException(description, e);
                default -> throw new RuntimeException();
            }
        }
    }

    public List<ExtractionResultDTO> getList(String userId, Instant dateFrom, Instant dateTo, int pageNum, int pageSize, Boolean isSortDesc) {
        GetListExtractionResultRequest request = GetListExtractionResultRequest.newBuilder()
                .setUserId(userId)
                .setCreatedFrom(ProtoTimeMapper.instantToTimestamp(dateFrom))
                .setCreatedTo(ProtoTimeMapper.instantToTimestamp(dateTo))
                .setPageNum(pageNum)
                .setPageSize(pageSize)
                .setSortCreatedDesc(isSortDesc)
                .build();

        try {
            GetListExtractionResultResponse response = blockingStub.getList(request);
            List<ExtractionResultProto> extractionResultsProto = response.getExtractionResultsList();
            return extractionResultsProto.stream()
                    .map(ProtoMapperDTO::protoToDto)
                    .toList();
        } catch (StatusRuntimeException e) {
            Status.Code code = e.getStatus().getCode();
            String description = e.getStatus().getDescription();

            switch (code) {
                case UNAVAILABLE -> throw new DataUnavailableException(description, e);
                default -> throw new RuntimeException();
            }
        }
    }

    public void delete(String id) {
        DeleteExtractionResultRequest request = DeleteExtractionResultRequest.newBuilder()
                .setId(id)
                .build();

        try {
            DeleteExtractionResultResponse response = blockingStub.delete(request);
        } catch (StatusRuntimeException e) {
            Status.Code code = e.getStatus().getCode();
            String description = e.getStatus().getDescription();

            switch (code) {
                case INVALID_ARGUMENT -> throw new IllegalArgumentException(description, e);
                case NOT_FOUND -> throw new DataNotFoundException(description, e);
                case UNAVAILABLE -> throw new DataUnavailableException(description, e);
                default -> throw new RuntimeException();
            }
        }
    }
}
