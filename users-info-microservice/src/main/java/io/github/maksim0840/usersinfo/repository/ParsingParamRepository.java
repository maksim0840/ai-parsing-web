package io.github.maksim0840.usersinfo.repository;

import io.github.maksim0840.usersinfo.domain.ParsingParam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ParsingParamRepository extends
        JpaRepository<ParsingParam, Long>,
        JpaSpecificationExecutor<ParsingParam> {
}
