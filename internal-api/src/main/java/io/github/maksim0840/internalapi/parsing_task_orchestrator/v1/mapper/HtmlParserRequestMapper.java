package io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlParserRequestDTO;
import io.github.maksim0840.parsing_task_orchestrator.v1.HtmlParserRequestProto;

import java.util.Map;

public class HtmlParserRequestMapper {

    public static HtmlParserRequestProto dtoToProto(HtmlParserRequestDTO dto) {
        HtmlParserRequestProto.Builder protoBuilder = HtmlParserRequestProto.newBuilder();
        protoBuilder.setTaskId(dto.taskId());
        protoBuilder.setUrl(dto.url());
        protoBuilder.setHtmlOutDir(dto.htmlOutDir());
        protoBuilder.setImagesOutDir(dto.imagesOutDir());
        if (dto.downloadImages() != null) protoBuilder.setDownloadImages(dto.downloadImages());
        protoBuilder.putAllHeaders(dto.headers() != null ? dto.headers() : Map.of());
        protoBuilder.putAllCookies(dto.cookies() != null ? dto.cookies() : Map.of());
        protoBuilder.putAllProxy(dto.proxy() != null ? dto.proxy() : Map.of());
        if (dto.pageComplexity() != null) protoBuilder.setPageComplexity(dto.pageComplexity());
        if (dto.additionalPageLoadTimeoutS() != null) protoBuilder.setAdditionalPageLoadTimeoutS(dto.additionalPageLoadTimeoutS());
        return protoBuilder.build();
    }

    public static HtmlParserRequestDTO protoToDto(HtmlParserRequestProto proto) {
        return HtmlParserRequestDTO.builder()
                .taskId(proto.getTaskId())
                .url(proto.getUrl())
                .htmlOutDir(proto.getHtmlOutDir())
                .imagesOutDir(proto.getImagesOutDir())
                .downloadImages(proto.hasDownloadImages() ? proto.getDownloadImages() : null)
                .headers(proto.getHeadersMap())
                .cookies(proto.getCookiesMap())
                .proxy(proto.getProxyMap())
                .pageComplexity(proto.hasPageComplexity() ? proto.getPageComplexity() : null)
                .additionalPageLoadTimeoutS(proto.hasAdditionalPageLoadTimeoutS() ? proto.getAdditionalPageLoadTimeoutS() : null)
                .build();
    }
}
