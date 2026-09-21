package com.back.shared.member.domain;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class ReplicaMember extends BaseMember {

    @Id
    private int id;

    private LocalDateTime createDate;

    private LocalDateTime modifyDate;

    public ReplicaMember(int id, LocalDateTime createDate, LocalDateTime modifyDate, String email, String password, String nickname, int activityScore) {
        super(email, password, nickname, activityScore);
        this.id = id;
        this.createDate = createDate;
        this.modifyDate = modifyDate;
    }
}
