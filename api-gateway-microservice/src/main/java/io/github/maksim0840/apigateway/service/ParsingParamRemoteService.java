package io.github.maksim0840.apigateway.service;

import io.github.maksim0840.apigateway.grpc.ParsingParamGrpcClient;
import io.github.maksim0840.internalapi.parsing_param.v1.dto.HtmlParserParamsDTO;
import io.github.maksim0840.internalapi.parsing_param.v1.dto.HtmlPreprocessingParamsDTO;
import io.github.maksim0840.internalapi.parsing_param.v1.dto.LLMParamsDTO;
import io.github.maksim0840.internalapi.parsing_param.v1.dto.ParsingParamDTO;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ParsingParamRemoteService {

    private final ParsingParamGrpcClient grpcClient;

    public ParsingParamRemoteService(ParsingParamGrpcClient grpcClient) {
        this.grpcClient = grpcClient;
    }

    public ParsingParamDTO createParsingParam(Long userId, String name, HtmlParserParamsDTO htmlParserParams, HtmlPreprocessingParamsDTO htmlPreprocessingParams, LLMParamsDTO llmParams) {
        return grpcClient.create(userId, name, htmlParserParams, htmlPreprocessingParams, llmParams);
    }

    public ParsingParamDTO editParsingParam(Long id, Long userId, String name, HtmlParserParamsDTO htmlParserParams, HtmlPreprocessingParamsDTO htmlPreprocessingParams, LLMParamsDTO llmParams) {
        return grpcClient.edit(id, userId, name, htmlParserParams, htmlPreprocessingParams, llmParams);
    }

    public ParsingParamDTO getParsingParamById(Long id, Long userId) {
        return grpcClient.get(id, userId);
    }

    public List<ParsingParamDTO> getListParsingParamByPageWithFiltering(Long userId, Instant dateFrom, Instant dateTo, int pageNum, int pageSize, Boolean isSortDesc) {
        return grpcClient.getList(userId, dateFrom, dateTo, pageNum, pageSize, isSortDesc);
    }

    public void deleteParsingParamById(Long id, Long userId) {
        grpcClient.delete(id, userId);
    }

    public List<String> getNamesByUserId(Long userId) {
        return grpcClient.getNamesByUserId(userId);
    }

    public ParsingParamDTO getByUserIdAndName(Long userId, String name) {
        return grpcClient.getByUserIdAndName(userId, name);
    }

    public void renameByUserIdAndName(Long userId, String oldName, String newName) {
        grpcClient.renameByUserIdAndName(userId, oldName, newName);
    }

    public void deleteByUserIdAndName(Long userId, String name) {
        grpcClient.deleteByUserIdAndName(userId, name);
    }
}
