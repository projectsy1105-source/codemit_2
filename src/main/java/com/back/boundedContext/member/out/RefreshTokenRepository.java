package com.back.boundedContext.member.out;

import com.back.boundedContext.member.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** 회원별 Refresh Token은 하나만 유지해 재발급 시 이전 토큰을 무효화한다. */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {
    Optional<RefreshToken> findByMember_Id(int memberId);

    void deleteByMember_Id(int memberId);
}
