package io.github.maksim0840.internalapi.user.v1.mapper;

import io.github.maksim0840.internalapi.user.v1.enums.UserRole;
import io.github.maksim0840.user.v1.UserRoleProto;

public class UserRoleProtoMapper {

    public static UserRoleProto domainToProto(UserRole domain) {
        if (domain == null) {
            throw new IllegalArgumentException("null UserRole mapping");
        }
        return switch (domain) {
            case ROLE_VISITOR -> UserRoleProto.VISITOR;
            case ROLE_USER -> UserRoleProto.USER;
            case ROLE_ADMIN -> UserRoleProto.ADMIN;
            default -> throw new IllegalArgumentException("Unknown UserRole argument mapping: " + domain);
        };
    }

    public static UserRole protoToDomain(UserRoleProto proto) {
        if (proto == null) {
            throw new IllegalArgumentException("null UserRoleProto mapping");
        }
        return switch (proto) {
            case USER_ROLE_UNSPECIFIED -> throw new IllegalArgumentException("UserRoleProto mapping not specified");
            case VISITOR -> UserRole.ROLE_VISITOR;
            case USER -> UserRole.ROLE_USER;
            case ADMIN -> UserRole.ROLE_ADMIN;
            default -> throw new IllegalArgumentException("Unknown UserRoleProto argument mapping: " + proto);
        };
    }
}
