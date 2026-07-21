package io.github.maksim0840.extractionresults.grpc;

import io.github.maksim0840.extraction_result.v1.*;
import io.github.maksim0840.extractionresults.entity.ExtractionResult;
import io.github.maksim0840.extractionresults.exception.NotFoundException;
import io.github.maksim0840.extractionresults.service.ExtractionResultService;
import io.github.maksim0840.internalapi.extraction_result.v1.dto.ExtractionResultDTO;
import io.github.maksim0840.internalapi.extraction_result.v1.enums.ResultFormat;
import io.github.maksim0840.internalapi.extraction_result.v1.mapper.ExtractionResultProtoMapper;
import io.github.maksim0840.internalapi.extraction_result.v1.mapper.ProtoJsonMapper;
import io.github.maksim0840.internalapi.common.v1.mapper.ProtoTimeMapper;
import io.github.maksim0840.internalapi.extraction_result.v1.mapper.ResultFormatProtoMapper;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Сервис, который регистрируется на grpc-сервере и реализует методы, вызываемые при получении соответствующего rpc запроса.
 * Отвечает за получение rpc запроса, обработку параметров и отправку ответа обратно grpc-клиенту.
 */
@GrpcService
public class ExtractionResultGrpcEndpoint extends ExtractionResultServiceGrpc.ExtractionResultServiceImplBase {
    private final ExtractionResultService extractionResultService;

    public ExtractionResultGrpcEndpoint(ExtractionResultService extractionResultService) {
        this.extractionResultService = extractionResultService;
    }

    @Override
    public void create(CreateExtractionResultRequest request,
                       StreamObserver<CreateExtractionResultResponse> observerResponse) {
        String url = request.getUrl();
        String userId = request.getUserId();
        Map<String, Object> jsonResult;
        try {
            jsonResult = ProtoJsonMapper.structToMap(request.getJsonResult());
        } catch (RuntimeException e) {
            observerResponse.onError(error(Status.INVALID_ARGUMENT, e.getMessage()));
            return;
        }

        try {
            ExtractionResultDTO extractionResult = extractionResultService.createExtractionResult(url, userId, jsonResult);
            ExtractionResultProto extractionResultProto = ExtractionResultProtoMapper.dtoToProto(extractionResult);
            CreateExtractionResultResponse response = CreateExtractionResultResponse.newBuilder()
                    .setExtractionResult(extractionResultProto).build();

            observerResponse.onNext(response);
            observerResponse.onCompleted();
        } catch (RuntimeException e) {
            observerResponse.onError(error(Status.UNAVAILABLE, e.getMessage()));
        }
    }

    @Override
    public void get(GetExtractionResultRequest request,
                    StreamObserver<GetExtractionResultResponse> observerResponse) {
        String id = request.getId();
        String userId = request.getUserId();
        ResultFormat resultFormat = ResultFormatProtoMapper.protoToEnum(request.getResultFormat());

        if (id.isBlank()) {
            observerResponse.onError(error(Status.INVALID_ARGUMENT, "id must not be blank"));
            return;
        }

        try {
            ExtractionResultDTO extractionResult = extractionResultService.getExtractionResultById(id, userId, resultFormat);
            ExtractionResultProto extractionResultProto = ExtractionResultProtoMapper.dtoToProto(extractionResult);
            GetExtractionResultResponse response = GetExtractionResultResponse.newBuilder()
                    .setExtractionResult(extractionResultProto).build();

            observerResponse.onNext(response);
            observerResponse.onCompleted();
        } catch (NotFoundException e) {
            observerResponse.onError(error(Status.NOT_FOUND, e.getMessage()));
        } catch (RuntimeException e) {
            observerResponse.onError(error(Status.UNAVAILABLE, e.getMessage()));
        }
    }

    @Override
    public void getList(GetListExtractionResultRequest request,
                        StreamObserver<GetListExtractionResultResponse> observerResponse) {
        String userId = request.hasUserId() ? request.getUserId() : null;
        Instant dateFrom = request.hasCreatedFrom() ? ProtoTimeMapper.timestampToInstant(request.getCreatedFrom()) : null;
        Instant dateTo = request.hasCreatedTo() ? ProtoTimeMapper.timestampToInstant(request.getCreatedTo()) : null;
        int pageNum = request.getPageNum();
        int pageSize = request.getPageSize();
        Boolean isSortDesc = request.hasSortCreatedDesc() ? request.getSortCreatedDesc() : null;
        ResultFormat resultFormat = ResultFormatProtoMapper.protoToEnum(request.getResultFormat());

        try {
            List<ExtractionResultDTO> extractionResults = extractionResultService.getListExtractionResultByPageWithFiltering(userId, dateFrom, dateTo, pageNum, pageSize, isSortDesc, resultFormat);
            List<ExtractionResultProto> extractionResultsProto = extractionResults.stream()
                    .map(ExtractionResultProtoMapper::dtoToProto)
                    .toList();
            GetListExtractionResultResponse response = GetListExtractionResultResponse.newBuilder()
                    .addAllExtractionResults(extractionResultsProto).build();

            observerResponse.onNext(response);
            observerResponse.onCompleted();
        } catch (RuntimeException e) {
            observerResponse.onError(error(Status.UNAVAILABLE, e.getMessage()));
        }
    }

    @Override
    public void getMergedList(GetMergedListExtractionResultRequest request,
                              StreamObserver<GetMergedListExtractionResultResponse> observerResponse) {
        String userId = request.hasUserId() ? request.getUserId() : null;
        Instant dateFrom = request.hasCreatedFrom() ? ProtoTimeMapper.timestampToInstant(request.getCreatedFrom()) : null;
        Instant dateTo = request.hasCreatedTo() ? ProtoTimeMapper.timestampToInstant(request.getCreatedTo()) : null;
        int pageNum = request.getPageNum();
        int pageSize = request.getPageSize();
        Boolean isSortDesc = request.hasSortCreatedDesc() ? request.getSortCreatedDesc() : null;
        ResultFormat resultFormat = ResultFormatProtoMapper.protoToEnum(request.getResultFormat());

        try {
            String mergedResults = extractionResultService.getMergedListExtractionResultByPageWithFiltering(userId, dateFrom, dateTo, pageNum, pageSize, isSortDesc, resultFormat);
            GetMergedListExtractionResultResponse response = GetMergedListExtractionResultResponse.newBuilder()
                    .setMergedExtractionResultsStr(mergedResults).build();

            observerResponse.onNext(response);
            observerResponse.onCompleted();
        } catch (RuntimeException e) {
            observerResponse.onError(error(Status.UNAVAILABLE, e.getMessage()));
        }
    }

    @Override
    public void count(CountExtractionResultRequest request,
                      StreamObserver<CountExtractionResultResponse> observerResponse) {
        String userId = request.hasUserId() ? request.getUserId() : null;
        Instant dateFrom = request.hasCreatedFrom() ? ProtoTimeMapper.timestampToInstant(request.getCreatedFrom()) : null;
        Instant dateTo = request.hasCreatedTo() ? ProtoTimeMapper.timestampToInstant(request.getCreatedTo()) : null;

        try {
            long numberOfRecords = extractionResultService.getExtractionResultsNumberByFiltering(userId, dateFrom, dateTo);
            CountExtractionResultResponse response = CountExtractionResultResponse.newBuilder()
                    .setNumberOfRecords(numberOfRecords).build();

            observerResponse.onNext(response);
            observerResponse.onCompleted();
        } catch (RuntimeException e) {
            observerResponse.onError(error(Status.UNAVAILABLE, e.getMessage()));
        }
    }

    @Override
    public void delete(DeleteExtractionResultRequest request,
                       StreamObserver<DeleteExtractionResultResponse> observerResponse) {
        String id = request.getId();
        String userId = request.getUserId();
        if (id.isBlank()) {
            observerResponse.onError(error(Status.INVALID_ARGUMENT, "id must not be blank"));
            return;
        }

        try {
            extractionResultService.deleteExtractionResultById(id, userId);
            DeleteExtractionResultResponse response = DeleteExtractionResultResponse.newBuilder().build();

            observerResponse.onNext(response);
            observerResponse.onCompleted();
        } catch (NotFoundException e) {
            observerResponse.onError(error(Status.NOT_FOUND, e.getMessage()));
        } catch (RuntimeException e) {
            observerResponse.onError(error(Status.UNAVAILABLE, e.getMessage()));
        }
    }

    private StatusRuntimeException error(Status status, String description) {
        return status.withDescription(description).asRuntimeException();
    }

    @Override
    public void update(UpdateExtractionResultRequest request,
                       StreamObserver<UpdateExtractionResultResponse> observerResponse) {
        String id = request.getId();
        String userId = request.getUserId();
        Map<String, Object> jsonResult;
        try {
            jsonResult = ProtoJsonMapper.structToMap(request.getJsonResult());
        } catch (RuntimeException e) {
            observerResponse.onError(error(Status.INVALID_ARGUMENT, e.getMessage()));
            return;
        }

        if (id.isBlank()) {
            observerResponse.onError(error(Status.INVALID_ARGUMENT, "id must not be blank"));
            return;
        }

        try {
            ExtractionResultDTO extractionResult = extractionResultService.updateExtractionResultById(id, userId, jsonResult);
            ExtractionResultProto extractionResultProto = ExtractionResultProtoMapper.dtoToProto(extractionResult);
            UpdateExtractionResultResponse response  = UpdateExtractionResultResponse.newBuilder()
                    .setExtractionResult(extractionResultProto).build();

            observerResponse.onNext(response);
            observerResponse.onCompleted();
        } catch (NotFoundException e) {
            observerResponse.onError(error(Status.NOT_FOUND, e.getMessage()));
        } catch (RuntimeException e) {
            observerResponse.onError(error(Status.UNAVAILABLE, e.getMessage()));
        }
    }
}