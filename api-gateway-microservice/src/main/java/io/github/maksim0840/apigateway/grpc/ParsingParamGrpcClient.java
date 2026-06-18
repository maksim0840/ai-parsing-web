package io.github.maksim0840.apigateway.grpc;

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
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ParsingParamGrpcClient {

    @GrpcClient("users_info")
    ParsingParamServiceGrpc.ParsingParamServiceBlockingStub blockingStub;

    public ParsingParamDTO create(Long userId, String name, HtmlParserParamsDTO htmlParserParams, HtmlPreprocessingParamsDTO htmlPreprocessingParams, LLMParamsDTO llmParams) {
        CreateParsingParamRequest request = CreateParsingParamRequest.newBuilder()
                .setUserId(userId)
                .setName(name)
                .setHtmlParserParams(HtmlParserParamsProtoMapper.dtoToProto(htmlParserParams))
                .setHtmlPreprocessingParams(HtmlPreprocessingParamsProtoMapper.dtoToProto(htmlPreprocessingParams))
                .setLlmParams(LLMParamsProtoMapper.dtoToProto(llmParams))
                .build();

        try {
            CreateParsingParamResponse response = blockingStub.create(request);
            ParsingParamProto parsingParamProto = response.getParsingParam();
            return ParsingParamProtoMapper.protoToDto(parsingParamProto);
        } catch (StatusRuntimeException e) {
            throw GrpcExceptionMapper.map(e);
        }
    }

    public ParsingParamDTO get(Long id) {
        GetParsingParamRequest request = GetParsingParamRequest.newBuilder()
                .setId(id)
                .build();

        try {
            GetParsingParamResponse response = blockingStub.get(request);
            ParsingParamProto parsingParamProto = response.getParsingParam();
            return ParsingParamProtoMapper.protoToDto(parsingParamProto);
        } catch (StatusRuntimeException e) {
            throw GrpcExceptionMapper.map(e);
        }
    }

    public List<ParsingParamDTO> getList(Long userId, Instant dateFrom, Instant dateTo, int pageNum, int pageSize, Boolean isSortDesc) {
        GetListParsingParamRequest.Builder requestBuilder = GetListParsingParamRequest.newBuilder()
                .setPageNum(pageNum)
                .setPageSize(pageSize);
        if (userId != null) requestBuilder.setUserId(userId);
        if (dateFrom != null) requestBuilder.setCreatedFrom(ProtoTimeMapper.instantToTimestamp(dateFrom));
        if (dateTo != null) requestBuilder.setCreatedTo(ProtoTimeMapper.instantToTimestamp(dateTo));
        if (isSortDesc != null) requestBuilder.setSortCreatedDesc(isSortDesc);
        GetListParsingParamRequest request = requestBuilder.build();

        try {
            GetListParsingParamResponse response = blockingStub.getList(request);
            List<ParsingParamProto> parsingParamProto = response.getParsingParamsList();
            return parsingParamProto.stream()
                    .map(ParsingParamProtoMapper::protoToDto)
                    .toList();
        } catch (StatusRuntimeException e) {
            throw GrpcExceptionMapper.map(e);
        }
    }

    public void delete(Long id) {
        DeleteParsingParamRequest request = DeleteParsingParamRequest.newBuilder()
                .setId(id)
                .build();

        try {
            DeleteParsingParamResponse response = blockingStub.delete(request);
        } catch (StatusRuntimeException e) {
            throw GrpcExceptionMapper.map(e);
        }
    }

    public List<String> getNamesByUserId(Long userId) {
        GetNamesParsingParamRequest request = GetNamesParsingParamRequest.newBuilder()
                .setUserId(userId)
                .build();
        try {
            GetNamesParsingParamResponse response = blockingStub.getNamesByUserId(request);
            return response.getNamesList();
        } catch (StatusRuntimeException e) {
            throw GrpcExceptionMapper.map(e);
        }

    }

    public ParsingParamDTO getByUserIdAndName(Long userId, String name) {
        GetParsingParamByUserIdAndNameRequest request = GetParsingParamByUserIdAndNameRequest.newBuilder()
                .setUserId(userId)
                .setName(name)
                .build();
        try {
            GetParsingParamByUserIdAndNameResponse response = blockingStub.getByUserIdAndName(request);
            ParsingParamProto parsingParamProto = response.getParsingParam();
            return ParsingParamProtoMapper.protoToDto(parsingParamProto);
        } catch (StatusRuntimeException e) {
            throw GrpcExceptionMapper.map(e);
        }
    }

    public void renameByUserIdAndName(Long userId, String oldName, String newName) {
        RenameParsingParamByUserIdAndNameRequest request = RenameParsingParamByUserIdAndNameRequest.newBuilder()
                .setUserId(userId)
                .setOldName(oldName)
                .setNewName(newName)
                .build();
        try {
            RenameParsingParamByUserIdAndNameResponse response = blockingStub.renameByUserIdAndName(request);
        } catch (StatusRuntimeException e) {
            throw GrpcExceptionMapper.map(e);
        }
    }

    public void deleteByUserIdAndName(Long userId, String name) {
        DeleteParsingParamByUserIdAndNameRequest request = DeleteParsingParamByUserIdAndNameRequest.newBuilder()
                .setUserId(userId)
                .setName(name)
                .build();
        try {
            DeleteParsingParamByUserIdAndNameResponse response = blockingStub.deleteByUserIdAndName(request);
        } catch (StatusRuntimeException e) {
            throw GrpcExceptionMapper.map(e);
        }
    }
}
