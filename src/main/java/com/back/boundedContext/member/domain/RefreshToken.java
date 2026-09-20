package com.back.boundedContext.member.domain;

import com.back.global.entity.BaseIdAndTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "MEMBER_REFRESH_TOKEN")
@NoArgsConstructor
@Getter
public class RefreshToken extends BaseIdAndTime {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Column(nullable = false, length = 100)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public RefreshToken(Member member, String tokenHash, LocalDateTime expiresAt) {
        this.member = member;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public void renew(String tokenHash, LocalDateTime expiresAt) {
        // 재발급 시 기존 토큰을 교체해 이전 Refresh Token을 즉시 무효화한다.
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now(ZoneOffset.UTC));
    }
}
