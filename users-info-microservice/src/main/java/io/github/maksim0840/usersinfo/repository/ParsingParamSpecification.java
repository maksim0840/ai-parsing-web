package io.github.maksim0840.usersinfo.repository;

import io.github.maksim0840.usersinfo.domain.ParsingParam;
import io.github.maksim0840.usersinfo.domain.User;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public class ParsingParamSpecification {
    public static Specification<ParsingParam> hasUser(Long userId) {
        return (root, criteriaQuery, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("user").get("id"), userId);
    }

    public static Specification<ParsingParam> lessOrEqualCreatedAt(Instant targetCreatedAt) {
        return (root, criteriaQuery, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), targetCreatedAt);
    }

    public static Specification<ParsingParam> greaterOrEqualCreatedAt(Instant targetCreatedAt) {
        return (root, criteriaQuery, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), targetCreatedAt);
    }
}
