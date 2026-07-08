package io.github.maksim0840.usersinfo.grpc;

import io.github.maksim0840.internalapi.common.v1.mapper.ProtoTimeMapper;
import io.github.maksim0840.internalapi.user.v1.dto.HtmlParserParamsDTO;
import io.github.maksim0840.internalapi.user.v1.dto.HtmlPreprocessingParamsDTO;
import io.github.maksim0840.internalapi.user.v1.dto.LLMParamsDTO;
import io.github.maksim0840.internalapi.user.v1.dto.ParsingParamDTO;
import io.github.maksim0840.internalapi.user.v1.mapper.HtmlParserParamsProtoMapper;
import io.github.maksim0840.internalapi.user.v1.mapper.HtmlPreprocessingParamsProtoMapper;
import io.github.maksim0840.internalapi.user.v1.mapper.LLMParamsProtoMapper;
import io.github.maksim0840.internalapi.user.v1.mapper.ParsingParamProtoMapper;
import io.github.maksim0840.parsing_param.v1.*;
import io.github.maksim0840.user.v1.DeleteUserResponse;
import io.github.maksim0840.usersinfo.entity.ParsingParam;
import io.github.maksim0840.usersinfo.entity.model.HtmlParserParams;
import io.github.maksim0840.usersinfo.exception.NotFoundException;
import io.github.maksim0840.usersinfo.service.ParsingParamService;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.dao.DataAccessException;

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
        HtmlParserParamsDTO htmlParserParams = HtmlParserParamsProtoMapper.protoToDto(request.getHtmlParserParams());
        HtmlPreprocessingParamsDTO htmlPreprocessingParams = HtmlPreprocessingParamsProtoMapper.protoToDto(request.getHtmlPreprocessingParams());
        LLMParamsDTO llmParams = LLMParamsProtoMapper.protoToDto(request.getLlmParams());

        try {
            ParsingParamDTO parsingParam = parsingParamService.createParsingParam(userId, name, htmlParserParams, htmlPreprocessingParams, llmParams);
            ParsingParamProto parsingParamProto = ParsingParamProtoMapper.dtoToProto(parsingParam);
            CreateParsingParamResponse response = CreateParsingParamResponse.newBuilder()
                    .setParsingParam(parsingParamProto).build();

            observerResponse.onNext(response);
            observerResponse.onCompleted();
        } catch (IllegalArgumentException e) {
            observerResponse.onError(error(Status.INVALID_ARGUMENT, e.getMessage()));
        } catch (RuntimeException e) {
            observerResponse.onError(error(Status.UNAVAILABLE, e.getMessage()));
        }
    }

    @Override
    public void edit(EditParsingParamRequest request, StreamObserver<EditParsingParamResponse> observerResponse) {
        Long id = request.getId();
        Long userId = request.getUserId();
        String name = request.getName();
        HtmlParserParamsDTO htmlParserParams = HtmlParserParamsProtoMapper.protoToDto(request.getHtmlParserParams());
        HtmlPreprocessingParamsDTO htmlPreprocessingParams = HtmlPreprocessingParamsProtoMapper.protoToDto(request.getHtmlPreprocessingParams());
        LLMParamsDTO llmParams = LLMParamsProtoMapper.protoToDto(request.getLlmParams());

        try {
            ParsingParamDTO parsingParam = parsingParamService.editParsingParam(id, userId, name, htmlParserParams, htmlPreprocessingParams, llmParams);
            ParsingParamProto parsingParamProto = ParsingParamProtoMapper.dtoToProto(parsingParam);
            EditParsingParamResponse response = EditParsingParamResponse.newBuilder()
                    .setParsingParam(parsingParamProto).build();

            observerResponse.onNext(response);
            observerResponse.onCompleted();
        } catch (IllegalArgumentException e) {
            observerResponse.onError(error(Status.INVALID_ARGUMENT, e.getMessage()));
        } catch (RuntimeException e) {
            observerResponse.onError(error(Status.UNAVAILABLE, e.getMessage()));
        }
    }

    @Override
    public void get(GetParsingParamRequest request, StreamObserver<GetParsingParamResponse> observerResponse) {
        Long id = request.getId();

        try {
            ParsingParamDTO parsingParam = parsingParamService.getParsingParamById(id);
            ParsingParamProto parsingParamProto = ParsingParamProtoMapper.dtoToProto(parsingParam);
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
            List<ParsingParamDTO> parsingParams = parsingParamService.getListParsingParamByPageWithFiltering(userId, createdFrom, createdTo, pageNum, pageSize, isSortDesc);
            List<ParsingParamProto> parsingParamsProto = parsingParams.stream()
                            .map(ParsingParamProtoMapper::dtoToProto).toList();
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
            DeleteParsingParamResponse response = DeleteParsingParamResponse.newBuilder().build();

            observerResponse.onNext(response);
            observerResponse.onCompleted();
        } catch (NotFoundException e) {
            observerResponse.onError(error(Status.NOT_FOUND, e.getMessage()));
        } catch (RuntimeException e) {
            observerResponse.onError(error(Status.UNAVAILABLE, e.getMessage()));
        }
    }

    @Override
    public void getNamesByUserId(GetNamesParsingParamRequest request, StreamObserver<GetNamesParsingParamResponse> observerResponse) {
        Long userId = request.getUserId();
        try {
            List<String> names = parsingParamService.getListParsingParamNameByUserId(userId);
            GetNamesParsingParamResponse response = GetNamesParsingParamResponse.newBuilder()
                    .addAllNames(names).build();

            observerResponse.onNext(response);
            observerResponse.onCompleted();
        } catch (RuntimeException e) {
            observerResponse.onError(error(Status.UNAVAILABLE, e.getMessage()));
        }
    }

    @Override
    public void getByUserIdAndName(GetParsingParamByUserIdAndNameRequest request, StreamObserver<GetParsingParamByUserIdAndNameResponse> observerResponse) {
        Long userId = request.getUserId();
        String name = request.getName();
        try {
            ParsingParamDTO parsingParam = parsingParamService.getParsingParamByUserIdAndName(userId, name);
            ParsingParamProto parsingParamProto = ParsingParamProtoMapper.dtoToProto(parsingParam);
            GetParsingParamByUserIdAndNameResponse response = GetParsingParamByUserIdAndNameResponse.newBuilder()
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
    public void renameByUserIdAndName(RenameParsingParamByUserIdAndNameRequest request, StreamObserver<RenameParsingParamByUserIdAndNameResponse> observerResponse) {
        Long userId = request.getUserId();
        String oldName = request.getOldName();
        String newName = request.getNewName();
        try {
            parsingParamService.renameParsingParam(userId, oldName, newName);
            RenameParsingParamByUserIdAndNameResponse response = RenameParsingParamByUserIdAndNameResponse.newBuilder().build();

            observerResponse.onNext(response);
            observerResponse.onCompleted();
        } catch (IllegalArgumentException e) {
            observerResponse.onError(error(Status.INVALID_ARGUMENT, e.getMessage()));
        } catch (RuntimeException e) {
            observerResponse.onError(error(Status.UNAVAILABLE, e.getMessage()));
        }
    }

    @Override
    public void deleteByUserIdAndName(DeleteParsingParamByUserIdAndNameRequest request, StreamObserver<DeleteParsingParamByUserIdAndNameResponse> observerResponse) {
        Long userId = request.getUserId();
        String name = request.getName();
        try {
            parsingParamService.deleteParsingParamByUserIdAndName(userId, name);
            DeleteParsingParamByUserIdAndNameResponse response = DeleteParsingParamByUserIdAndNameResponse.newBuilder().build();

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
}
