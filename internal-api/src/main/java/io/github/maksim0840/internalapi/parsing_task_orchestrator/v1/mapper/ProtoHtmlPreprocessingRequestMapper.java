package io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlPreprocessingRequestDTO;
import io.github.maksim0840.parsing_task_orchestrator.v1.HtmlPreprocessingRequestProto;

public class ProtoHtmlPreprocessingRequestMapper {

    public static HtmlPreprocessingRequestProto dtoToProto(HtmlPreprocessingRequestDTO dto) {
        HtmlPreprocessingRequestProto.Builder protoBuilder = HtmlPreprocessingRequestProto.newBuilder();
        protoBuilder.setTaskId(dto.taskId());
        protoBuilder.addAllHtmlPaths(dto.htmlPaths());
        if (dto.noscriptProcessing() != null) protoBuilder.setNoscriptProcessing(dto.noscriptProcessing());
        if (dto.linkProcessing() != null) protoBuilder.setLinkProcessing(dto.linkProcessing());
        if (dto.styleProcessing() != null) protoBuilder.setStyleProcessing(dto.styleProcessing());
        if (dto.metaProcessing() != null) protoBuilder.setMetaProcessing(dto.metaProcessing());
        if (dto.scriptProcessing() != null) protoBuilder.setScriptProcessing(dto.scriptProcessing());
        if (dto.canvasProcessing() != null) protoBuilder.setCanvasProcessing(dto.canvasProcessing());
        if (dto.svgProcessing() != null) protoBuilder.setSvgProcessing(dto.svgProcessing());
        if (dto.areaProcessing() != null) protoBuilder.setAreaProcessing(dto.areaProcessing());
        if (dto.imgProcessing() != null) protoBuilder.setImgProcessing(dto.imgProcessing());
        if (dto.videoProcessing() != null) protoBuilder.setVideoProcessing(dto.videoProcessing());
        if (dto.audioProcessing() != null) protoBuilder.setAudioProcessing(dto.audioProcessing());
        if (dto.iframeProcessing() != null) protoBuilder.setIframeProcessing(dto.iframeProcessing());
        if (dto.portalProcessing() != null) protoBuilder.setPortalProcessing(dto.portalProcessing());
        if (dto.embedProcessing() != null) protoBuilder.setEmbedProcessing(dto.embedProcessing());
        if (dto.objectProcessing() != null) protoBuilder.setObjectProcessing(dto.objectProcessing());
        if (dto.sourceProcessing() != null) protoBuilder.setSourceProcessing(dto.sourceProcessing());
        if (dto.noscriptProcessing() != null) protoBuilder.setNoscriptProcessing(dto.noscriptProcessing());
        return protoBuilder.build();
    }

    public static HtmlPreprocessingRequestDTO protoToDto(HtmlPreprocessingRequestProto proto) {
        return HtmlPreprocessingRequestDTO.builder()
                .taskId(proto.getTaskId())
                .htmlPaths(proto.getHtmlPathsList())
                .noscriptProcessing(proto.hasNoscriptProcessing() ? proto.getNoscriptProcessing() : null)
                .linkProcessing(proto.hasLinkProcessing() ? proto.getLinkProcessing() : null)
                .styleProcessing(proto.hasStyleProcessing() ? proto.getStyleProcessing() : null)
                .metaProcessing(proto.hasMetaProcessing() ? proto.getMetaProcessing() : null)
                .scriptProcessing(proto.hasScriptProcessing() ? proto.getScriptProcessing() : null)
                .canvasProcessing(proto.hasCanvasProcessing() ? proto.getCanvasProcessing() : null)
                .svgProcessing(proto.hasSvgProcessing() ? proto.getSvgProcessing() : null)
                .areaProcessing(proto.hasAreaProcessing() ? proto.getAreaProcessing() : null)
                .imgProcessing(proto.hasImgProcessing() ? proto.getImgProcessing() : null)
                .videoProcessing(proto.hasVideoProcessing() ? proto.getVideoProcessing() : null)
                .audioProcessing(proto.hasAudioProcessing() ? proto.getAudioProcessing() : null)
                .iframeProcessing(proto.hasIframeProcessing() ? proto.getIframeProcessing() : null)
                .portalProcessing(proto.hasPortalProcessing() ? proto.getPortalProcessing() : null)
                .embedProcessing(proto.hasEmbedProcessing() ? proto.getEmbedProcessing() : null)
                .objectProcessing(proto.hasObjectProcessing() ? proto.getObjectProcessing() : null)
                .sourceProcessing(proto.hasSourceProcessing() ? proto.getSourceProcessing() : null)
                .build();
    }
}
