package io.github.maksim0840.apigateway.grpc;

import io.github.maksim0840.extraction_result.v1.*;
import io.github.maksim0840.internalapi.extraction_result.v1.dto.ExtractionResultDTO;
import io.github.maksim0840.internalapi.extraction_result.v1.enums.ResultFormat;
import io.github.maksim0840.internalapi.extraction_result.v1.mapper.ExtractionResultProtoMapper;
import io.github.maksim0840.internalapi.extraction_result.v1.mapper.ProtoJsonMapper;
import io.github.maksim0840.internalapi.common.v1.mapper.ProtoTimeMapper;
import io.github.maksim0840.internalapi.extraction_result.v1.mapper.ResultFormatProtoMapper;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class ExtractionResultGrpcClient {

    @GrpcClient("extraction_results")
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
            return ExtractionResultProtoMapper.protoToDto(extractionResultProto);
        } catch (StatusRuntimeException e) {
            throw GrpcExceptionMapper.map(e);
        }
    }

    public ExtractionResultDTO get(String id, ResultFormat resultFormat) {
        GetExtractionResultRequest request = GetExtractionResultRequest.newBuilder()
                .setId(id)
                .setResultFormat(ResultFormatProtoMapper.enumToProto(resultFormat))
                .build();

        try {
            GetExtractionResultResponse response = blockingStub.get(request);
            ExtractionResultProto extractionResultProto = response.getExtractionResult();
            return ExtractionResultProtoMapper.protoToDto(extractionResultProto);
        } catch (StatusRuntimeException e) {
            throw GrpcExceptionMapper.map(e);
        }
    }

    public List<ExtractionResultDTO> getList(String userId, Instant dateFrom, Instant dateTo, int pageNum, int pageSize, Boolean isSortDesc, ResultFormat resultFormat) {
        GetListExtractionResultRequest.Builder requestBuilder = GetListExtractionResultRequest.newBuilder()
                .setPageNum(pageNum)
                .setPageSize(pageSize);
        if (userId != null) requestBuilder.setUserId(userId);
        if (dateFrom != null) requestBuilder.setCreatedFrom(ProtoTimeMapper.instantToTimestamp(dateFrom));
        if (dateTo != null) requestBuilder.setCreatedTo(ProtoTimeMapper.instantToTimestamp(dateTo));
        if (isSortDesc != null) requestBuilder.setSortCreatedDesc(isSortDesc);
        requestBuilder.setResultFormat(ResultFormatProtoMapper.enumToProto(resultFormat));
        GetListExtractionResultRequest request = requestBuilder.build();

        try {
            GetListExtractionResultResponse response = blockingStub.getList(request);
            List<ExtractionResultProto> extractionResultsProto = response.getExtractionResultsList();
            return extractionResultsProto.stream()
                    .map(ExtractionResultProtoMapper::protoToDto)
                    .toList();
        } catch (StatusRuntimeException e) {
            throw GrpcExceptionMapper.map(e);
        }
    }

    public String getMergedList(String userId, Instant dateFrom, Instant dateTo, int pageNum, int pageSize, Boolean isSortDesc, ResultFormat resultFormat) {
        GetMergedListExtractionResultRequest.Builder requestBuilder = GetMergedListExtractionResultRequest.newBuilder()
                .setPageNum(pageNum)
                .setPageSize(pageSize);
        if (userId != null) requestBuilder.setUserId(userId);
        if (dateFrom != null) requestBuilder.setCreatedFrom(ProtoTimeMapper.instantToTimestamp(dateFrom));
        if (dateTo != null) requestBuilder.setCreatedTo(ProtoTimeMapper.instantToTimestamp(dateTo));
        if (isSortDesc != null) requestBuilder.setSortCreatedDesc(isSortDesc);
        requestBuilder.setResultFormat(ResultFormatProtoMapper.enumToProto(resultFormat));
        GetMergedListExtractionResultRequest request = requestBuilder.build();

        try {
            GetMergedListExtractionResultResponse response = blockingStub.getMergedList(request);
            return response.getMergedExtractionResultsStr();
        } catch (StatusRuntimeException e) {
            throw GrpcExceptionMapper.map(e);
        }
    }

    public long count(String userId, Instant dateFrom, Instant dateTo) {
        CountExtractionResultRequest.Builder requestBuilder = CountExtractionResultRequest.newBuilder();
        if (userId != null) requestBuilder.setUserId(userId);
        if (dateFrom != null) requestBuilder.setCreatedFrom(ProtoTimeMapper.instantToTimestamp(dateFrom));
        if (dateTo != null) requestBuilder.setCreatedTo(ProtoTimeMapper.instantToTimestamp(dateTo));
        CountExtractionResultRequest request = requestBuilder.build();

        try {
            CountExtractionResultResponse response = blockingStub.count(request);
            return response.getNumberOfRecords();
        } catch (StatusRuntimeException e) {
            throw GrpcExceptionMapper.map(e);
        }
    }

    public void delete(String id) {
        DeleteExtractionResultRequest request = DeleteExtractionResultRequest.newBuilder()
                .setId(id)
                .build();

        try {
            DeleteExtractionResultResponse response = blockingStub.delete(request);
        } catch (StatusRuntimeException e) {
            throw GrpcExceptionMapper.map(e);
        }
    }
}
