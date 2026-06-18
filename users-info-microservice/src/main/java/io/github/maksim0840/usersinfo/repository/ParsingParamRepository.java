package io.github.maksim0840.usersinfo.repository;

import io.github.maksim0840.usersinfo.entity.ParsingParam;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParsingParamRepository extends
        JpaRepository<ParsingParam, Long>,
        JpaSpecificationExecutor<ParsingParam> {

    // использует уникальный индекс uq_parsing_params_user_name
    Optional<ParsingParam> findByUserIdAndName(Long userId, String name);

    // использует leftmost prefix из индекса uq_parsing_params_user_name
    @Query("SELECT p.name FROM ParsingParam p WHERE p.user.id = :userId")
    List<String> findNamesByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ParsingParam p WHERE p.user.id = :userId AND p.name = :name")
    void deleteByUserIdAndName(@Param("userId") Long userId, @Param("name") String name);

    @Modifying
    @Transactional
    @Query("UPDATE ParsingParam p SET p.name = :newName WHERE p.user.id = :userId AND p.name = :oldName")
    void renameByUserIdAndName(
            @Param("userId") Long userId,
            @Param("oldName") String oldName,
            @Param("newName") String newName
    );
}
