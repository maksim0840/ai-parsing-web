package io.github.maksim0840.internalapi.user.v1.mapper;

import io.github.maksim0840.internalapi.user.v1.enums.UserRole;
import io.github.maksim0840.user.v1.UserRoleProto;

public class ProtoUserRoleMapper {

    public static UserRoleProto domainToProto(UserRole domain) {
        if (domain == null) {
            throw new IllegalArgumentException("null UserRole mapping");
        }
        return switch (domain) {
            case VISITOR -> UserRoleProto.VISITOR;
            case USER -> UserRoleProto.USER;
            case ADMIN -> UserRoleProto.ADMIN;
            default -> throw new IllegalArgumentException("Unknown UserRole argument mapping: " + domain);
        };
    }

    public static UserRole protoToDomain(UserRoleProto proto) {
        if (proto == null) {
            throw new IllegalArgumentException("null UserRoleProto mapping");
        }
        return switch (proto) {
            case USER_ROLE_UNSPECIFIED -> throw new IllegalArgumentException("UserRoleProto mapping not specified");
            case VISITOR -> UserRole.VISITOR;
            case USER -> UserRole.USER;
            case ADMIN -> UserRole.ADMIN;
            default -> throw new IllegalArgumentException("Unknown UserRoleProto argument mapping: " + proto);
        };
    }
}
