package io.github.maksim0840.usersinfo.mapper;

import io.github.maksim0840.internalapi.common.v1.mapper.ProtoTimeMapper;
import io.github.maksim0840.internalapi.user.v1.mapper.ProtoUserRoleMapper;
import io.github.maksim0840.user.v1.UserProto;
import io.github.maksim0840.usersinfo.domain.User;

public class ProtoDomainUserMapper {

    public static UserProto domainToProto(User domain) {
        return UserProto.newBuilder()
                .setId(domain.getId())
                .setName(domain.getName())
                .setPasswordHash(domain.getPasswordHash())
                .setRole(ProtoUserRoleMapper.domainToProto(domain.getRole()))
                .setCreatedAt(ProtoTimeMapper.instantToTimestamp(domain.getCreatedAt()))
                .build();
    }

    public static User protoToDomain(UserProto proto) {
        return User.builder()
                .id(proto.getId())
                .name(proto.getName())
                .passwordHash(proto.getPasswordHash())
                .role(ProtoUserRoleMapper.protoToDomain(proto.getRole()))
                .createdAt(ProtoTimeMapper.timestampToInstant(proto.getCreatedAt()))
                .build();
    }
}
