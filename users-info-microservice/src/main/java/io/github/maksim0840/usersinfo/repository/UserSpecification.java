package io.github.maksim0840.usersinfo.repository;

import io.github.maksim0840.usersinfo.domain.User;
import io.github.maksim0840.internalapi.user.v1.enums.UserRole;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public class UserSpecification {
    public static Specification<User> hasRole(UserRole targetRole) {
        return (root, criteriaQuery, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("role"), targetRole);
    }

    public static Specification<User> lessOrEqualCreatedAt(Instant targetCreatedAt) {
        return (root, criteriaQuery, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), targetCreatedAt);
    }

    public static Specification<User> greaterOrEqualCreatedAt(Instant targetCreatedAt) {
        return (root, criteriaQuery, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), targetCreatedAt);
    }
}
