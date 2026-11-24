package org.yasha.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.yasha.auth.entity.RefreshToken;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    Optional<RefreshToken> findByTokenHashAndUserIdAndRevokedAtIsNull(String tokenHash, UUID userId);

    @Modifying
    @Query("""
        UPDATE RefreshToken rt
        SET rt.revokedAt = :now
        WHERE rt.userId = :userId AND rt.revokedAt IS NULL
    """)
    int revokeAllByUser(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);
}