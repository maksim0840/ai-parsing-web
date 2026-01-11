package io.github.maksim0840.usersinfo.grpc;

import io.github.maksim0840.internalapi.common.v1.mapper.ProtoTimeMapper;
import io.github.maksim0840.parsing_param.v1.*;
import io.github.maksim0840.usersinfo.domain.ParsingParam;
import io.github.maksim0840.usersinfo.exception.NotFoundException;
import io.github.maksim0840.usersinfo.mapper.ProtoDomainParsingParamMapper;
import io.github.maksim0840.usersinfo.service.ParsingParamService;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.Instant;
import java.util.List;

@GrpcService
public class ParsingParamGrpcEndpoint extends ParsingParamServiceGrpc.ParsingParamServiceImplBase {

    private final ParsingParamService parsingParamService;

    public ParsingParamGrpcEndpoint(ParsingParamService parsingParamService) {
        this.parsingParamService = parsingParamService;
    }

    @Override
    public void create(CreateParsingParamRequest request, StreamObserver<CreateParsingParamResponse> observerResponse) {
        Long userId = request.getUserId();
        String name = request.getName();
        String description = request.getDescription();

        try {
            ParsingParam parsingParam = parsingParamService.createParsingParam(userId, name, description);
            ParsingParamProto parsingParamProto = ProtoDomainParsingParamMapper.domainToProto(parsingParam);
            CreateParsingParamResponse response = CreateParsingParamResponse.newBuilder()
                    .setParsingParam(parsingParamProto).build();

            observerResponse.onNext(response);
            observerResponse.onCompleted();
        } catch (NotFoundException e) {
            observerResponse.onError(error(Status.NOT_FOUND, e.getMessage()));
        } catch (RuntimeException e) {
            observerResponse.onError(error(Status.UNAVAILABLE, e.getMessage()));
        }
    }

    @Override
    public void get(GetParsingParamRequest request, StreamObserver<GetParsingParamResponse> observerResponse) {
        Long id = request.getId();

        try {
            ParsingParam parsingParam = parsingParamService.getParsingParamById(id);
            ParsingParamProto parsingParamProto = ProtoDomainParsingParamMapper.domainToProto(parsingParam);
            GetParsingParamResponse response = GetParsingParamResponse.newBuilder()
                    .setParsingParam(parsingParamProto).build();

            observerResponse.onNext(response);
            observerResponse.onCompleted();
        } catch (NotFoundException e) {
            observerResponse.onError(error(Status.NOT_FOUND, e.getMessage()));
        } catch (RuntimeException e) {
            observerResponse.onError(error(Status.UNAVAILABLE, e.getMessage()));
        }
    }

    @Override
    public void getList(GetListParsingParamRequest request, StreamObserver<GetListParsingParamResponse> observerResponse) {
        Long userId = request.hasUserId() ? request.getUserId() : null;
        Instant createdFrom = request.hasCreatedFrom() ? ProtoTimeMapper.timestampToInstant(request.getCreatedFrom()) : null;
        Instant createdTo = request.hasCreatedTo() ? ProtoTimeMapper.timestampToInstant(request.getCreatedTo()) : null;
        int pageNum = request.getPageNum();
        int pageSize = request.getPageSize();
        Boolean isSortDesc = request.hasSortCreatedDesc() ? request.getSortCreatedDesc() : null;

        try {
            List<ParsingParam> parsingParams = parsingParamService.getListParsingParamByPageWithFiltering(userId, createdFrom, createdTo, pageNum, pageSize, isSortDesc);
            List<ParsingParamProto> parsingParamsProto = parsingParams.stream()
                            .map(ProtoDomainParsingParamMapper::domainToProto).toList();
            GetListParsingParamResponse response = GetListParsingParamResponse.newBuilder()
                    .addAllParsingParams(parsingParamsProto).build();

            observerResponse.onNext(response);
            observerResponse.onCompleted();
        } catch (RuntimeException e) {
            observerResponse.onError(error(Status.UNAVAILABLE, e.getMessage()));
        }
    }

    @Override
    public void delete(DeleteParsingParamRequest request, StreamObserver<DeleteParsingParamResponse> observerResponse) {
        Long id = request.getId();

        try {
            parsingParamService.deleteParsingParamById(id);
        } catch (NotFoundException e) {
            observerResponse.onError(error(Status.NOT_FOUND, e.getMessage()));
        } catch (RuntimeException e) {
            observerResponse.onError(error(Status.UNAVAILABLE, e.getMessage()));
        }
    }

    private StatusRuntimeException error(Status status, String description) {
        return status.withDescription(description).asRuntimeException();
    }
}
