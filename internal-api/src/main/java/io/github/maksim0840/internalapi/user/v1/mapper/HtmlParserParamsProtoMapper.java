package io.github.maksim0840.internalapi.user.v1.mapper;

import io.github.maksim0840.internalapi.common.v1.mapper.ProtoTimeMapper;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlParserRequestDTO;
import io.github.maksim0840.internalapi.user.v1.dto.HtmlParserParamsDTO;
import io.github.maksim0840.internalapi.user.v1.dto.UserDTO;
import io.github.maksim0840.parsing_param.v1.HtmlParserParamsProto;
import io.github.maksim0840.parsing_task_orchestrator.v1.HtmlParserRequestProto;
import io.github.maksim0840.user.v1.UserProto;

import java.util.Map;


public class HtmlParserParamsProtoMapper {
    public static HtmlParserParamsProto dtoToProto(HtmlParserParamsDTO dto) {
        HtmlParserParamsProto.Builder protoBuilder = HtmlParserParamsProto.newBuilder();
        if (dto.downloadImages() != null) protoBuilder.setDownloadImages(dto.downloadImages());
        protoBuilder.putAllHeaders(dto.headers() != null ? dto.headers() : Map.of());
        protoBuilder.putAllCookies(dto.cookies() != null ? dto.cookies() : Map.of());
        protoBuilder.putAllProxy(dto.proxy() != null ? dto.proxy() : Map.of());
        if (dto.pageComplexity() != null) protoBuilder.setPageComplexity(dto.pageComplexity());
        if (dto.additionalPageLoadTimeoutS() != null) protoBuilder.setAdditionalPageLoadTimeoutS(dto.additionalPageLoadTimeoutS());
        return protoBuilder.build();
    }

    public static HtmlParserParamsDTO protoToDto(HtmlParserParamsProto proto) {
        return HtmlParserParamsDTO.builder()
                .downloadImages(proto.hasDownloadImages() ? proto.getDownloadImages() : null)
                .headers(proto.getHeadersMap())
                .cookies(proto.getCookiesMap())
                .proxy(proto.getProxyMap())
                .pageComplexity(proto.hasPageComplexity() ? proto.getPageComplexity() : null)
                .additionalPageLoadTimeoutS(proto.hasAdditionalPageLoadTimeoutS() ? proto.getAdditionalPageLoadTimeoutS() : null)
                .build();
    }
}