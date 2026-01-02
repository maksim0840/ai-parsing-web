package io.github.maksim0840.extractionresults.grpc;

import io.github.maksim0840.extraction_result.v1.*;
import io.github.maksim0840.extractionresults.domain.ExtractionResult;
import io.github.maksim0840.extractionresults.exception.NotFoundException;
import io.github.maksim0840.extractionresults.mapper.ProtoMapper;
import io.github.maksim0840.extractionresults.service.ExtractionResultService;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@GrpcService
public class ExtractionResultGrpcEndpoint extends ExtractionResultServiceGrpc.ExtractionResultServiceImplBase {
    private final ExtractionResultService extractionResultService;

    public ExtractionResultGrpcEndpoint(ExtractionResultService extractionResultService) {
        this.extractionResultService = extractionResultService;
    }

    public void create(CreateExtractionResultRequest request,
                       StreamObserver<CreateExtractionResultResponse> observerResponse) {
        String url = request.getUrl();
        String userId = request.getUserId();
        Map<String, Object> jsonResult = new HashMap<>();;
        try {
            jsonResult = ProtoMapper.structToMap(request.getJsonResult());
        } catch (RuntimeException e) {
            observerResponse.onError(error(Status.INVALID_ARGUMENT, e.getMessage()));
            return;
        }

        try {
            ExtractionResult extractionResult = extractionResultService.createExtractionResult(url, userId, jsonResult);
            ExtractionResultProto extractionResultProto = ProtoMapper.docToProto(extractionResult);
            CreateExtractionResultResponse response = CreateExtractionResultResponse.newBuilder()
                    .setExtractionResult(extractionResultProto).build();

            observerResponse.onNext(response);
            observerResponse.onCompleted();
        } catch (RuntimeException e) {
            observerResponse.onError(error(Status.ABORTED, e.getMessage()));
        }
    }

    @Override
    public void get(GetExtractionResultRequest request,
                    StreamObserver<GetExtractionResultResponse> observerResponse) {
        String id = request.getId();
        if (id.isBlank()) {
            observerResponse.onError(error(Status.INVALID_ARGUMENT, "id must not be blank"));
            return;
        }

        try {
            ExtractionResult extractionResult = extractionResultService.getExtractionResultById(id);
            ExtractionResultProto extractionResultProto = ProtoMapper.docToProto(extractionResult);
            GetExtractionResultResponse response = GetExtractionResultResponse.newBuilder()
                            .setExtractionResult(extractionResultProto).build();

            observerResponse.onNext(response);
            observerResponse.onCompleted();
        } catch (NotFoundException e) {
            observerResponse.onError(error(Status.NOT_FOUND, e.getMessage()));
        } catch (RuntimeException e) {
            observerResponse.onError(error(Status.ABORTED, e.getMessage()));
        }
    }

    @Override
    public void getList(GetListExtractionResultRequest request,
                        StreamObserver<GetListExtractionResultResponse> observerResponse) {
        String userId = request.hasUserId() ? request.getUserId() : null;
        Instant dateFrom = request.hasCreatedFrom() ? ProtoMapper.timestampToInstant(request.getCreatedFrom()) : null;
        Instant dateTo = request.hasCreatedTo() ? ProtoMapper.timestampToInstant(request.getCreatedTo()) : null;
        int pageNum = request.getPageNum();
        int pageSize = request.getPageSize();
        Boolean isSortDesc = request.hasSortCreatedDesc() ? request.getSortCreatedDesc() : null;

        try {
            List<ExtractionResult> extractionResults = extractionResultService.getListExtractionResultBySpecPaging(userId, dateFrom, dateTo, pageNum, pageSize, isSortDesc);
            List<ExtractionResultProto> extractionResultsProto = extractionResults.stream()
                            .map(ProtoMapper::docToProto)
                            .toList();
            GetListExtractionResultResponse response = GetListExtractionResultResponse.newBuilder()
                            .addAllExtractionResults(extractionResultsProto).build();

            observerResponse.onNext(response);
            observerResponse.onCompleted();
        } catch (RuntimeException e) {
            observerResponse.onError(error(Status.ABORTED, e.getMessage()));
        }

    }

    @Override
    public void delete(DeleteExtractionResultRequest request,
                       StreamObserver<DeleteExtractionResultResponse> observerResponse) {
        String id = request.getId();
        if (id.isBlank()) {
            observerResponse.onError(error(Status.INVALID_ARGUMENT, "id must not be blank"));
            return;
        }

        try {
            extractionResultService.deleteExtractionResultById(id);
            DeleteExtractionResultResponse response = DeleteExtractionResultResponse.newBuilder().build();

            observerResponse.onNext(response);
            observerResponse.onCompleted();
        } catch (NotFoundException e) {
            observerResponse.onError(error(Status.NOT_FOUND, e.getMessage()));
        } catch (RuntimeException e) {
            observerResponse.onError(error(Status.ABORTED, e.getMessage()));
        }
    }

    private StatusRuntimeException error(Status status, String description) {
        return status.withDescription(description).asRuntimeException();
    }
}
