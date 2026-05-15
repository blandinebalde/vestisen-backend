package com.vendit.repository;

import com.vendit.model.RevokedJwtToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface RevokedJwtTokenRepository extends JpaRepository<RevokedJwtToken, Long> {

    boolean existsByJti(String jti);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM RevokedJwtToken r WHERE r.expiresAt < :now")
    int deleteByExpiresAtBefore(@Param("now") Instant now);
}
