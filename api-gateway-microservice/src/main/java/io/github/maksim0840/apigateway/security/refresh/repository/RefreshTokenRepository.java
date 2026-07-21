package io.github.maksim0840.apigateway.security.refresh.repository;

import io.github.maksim0840.apigateway.security.refresh.entity.RefreshToken;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {

    // для "выйти со всех устройств"
    List<RefreshToken> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}