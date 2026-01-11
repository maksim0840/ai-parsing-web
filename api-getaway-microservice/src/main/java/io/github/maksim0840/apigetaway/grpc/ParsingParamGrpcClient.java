package io.github.maksim0840.apigetaway.grpc;

import io.github.maksim0840.apigetaway.dto.ParsingParamDTO;
import io.github.maksim0840.apigetaway.exception.DataUnavailableException;
import io.github.maksim0840.apigetaway.mapper.ProtoDTOParsingParamMapper;
import io.github.maksim0840.extraction_result.v1.GetListExtractionResultRequest;
import io.github.maksim0840.internalapi.common.v1.mapper.ProtoTimeMapper;
import io.github.maksim0840.parsing_param.v1.*;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ParsingParamGrpcClient {

    @GrpcClient("usersInfo")
    ParsingParamServiceGrpc.ParsingParamServiceBlockingStub blockingStub;

    public ParsingParamDTO create(Long userId, String name, String description) {
        CreateParsingParamRequest request = CreateParsingParamRequest.newBuilder()
                .setUserId(userId)
                .setName(name)
                .setDescription(description)
                .build();

        try {
            CreateParsingParamResponse response = blockingStub.create(request);
            ParsingParamProto parsingParamProto = response.getParsingParam();
            return ProtoDTOParsingParamMapper.protoToDto(parsingParamProto);
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
            return ProtoDTOParsingParamMapper.protoToDto(parsingParamProto);
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
                    .map(ProtoDTOParsingParamMapper::protoToDto)
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
}
