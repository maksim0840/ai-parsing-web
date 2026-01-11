package io.github.maksim0840.apigetaway.mapper;

import io.github.maksim0840.apigetaway.dto.UserDTO;
import io.github.maksim0840.internalapi.common.v1.mapper.ProtoTimeMapper;
import io.github.maksim0840.internalapi.user.v1.mapper.ProtoUserRoleMapper;
import io.github.maksim0840.user.v1.UserProto;

public class ProtoDTOUserMapper {

    public static UserProto dtoToProto(UserDTO dto) {
        return UserProto.newBuilder()
                .setId(dto.id())
                .setName(dto.name())
                .setPasswordHash(dto.passwordHash())
                .setRole(ProtoUserRoleMapper.domainToProto(dto.role()))
                .setCreatedAt(ProtoTimeMapper.instantToTimestamp(dto.createdAt()))
                .build();
    }

    public static UserDTO protoToDto(UserProto proto) {
        return UserDTO.builder()
                .id(proto.getId())
                .name(proto.getName())
                .passwordHash(proto.getPasswordHash())
                .role(ProtoUserRoleMapper.protoToDomain(proto.getRole()))
                .createdAt(ProtoTimeMapper.timestampToInstant(proto.getCreatedAt()))
                .build();
    }
}
