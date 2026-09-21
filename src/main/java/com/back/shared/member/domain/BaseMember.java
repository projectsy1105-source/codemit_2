package com.back.shared.member.domain;

import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.IDENTITY;

@MappedSuperclass
@Getter
@Setter(value = AccessLevel.PROTECTED)
@NoArgsConstructor
public abstract class BaseMember extends BaseEntity {
    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String nickname;
    private int activityScore;

    public BaseMember(String email, String password, String nickname, int activityScore) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.activityScore = activityScore;
    }

}
