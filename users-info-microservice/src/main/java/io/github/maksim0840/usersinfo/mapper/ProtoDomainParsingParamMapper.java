package io.github.maksim0840.usersinfo.mapper;

import io.github.maksim0840.internalapi.common.v1.mapper.ProtoTimeMapper;
import io.github.maksim0840.parsing_param.v1.ParsingParamProto;
import io.github.maksim0840.usersinfo.domain.ParsingParam;

public class ProtoDomainParsingParamMapper {

    public static ParsingParamProto domainToProto(ParsingParam domain) {
        return ParsingParamProto.newBuilder()
                .setId(domain.getId())
                .setUserId(domain.getUser().getId())
                .setName(domain.getName())
                .setDescription(domain.getDescription())
                .setCreatedAt(ProtoTimeMapper.instantToTimestamp(domain.getCreatedAt()))
                .build();
    }

//    public static ParsingParam protoToDomain(ParsingParamProto proto) {
//        return ParsingParam.builder()
//                .build();
//    }
}
