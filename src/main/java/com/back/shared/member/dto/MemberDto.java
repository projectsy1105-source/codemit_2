package com.back.shared.member.dto;

import com.back.boundedContext.member.domain.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
public class MemberDto {

    private final int id;
    private final LocalDateTime createDate;
    private final LocalDateTime modifyDate;
    private final String email;
    private final String nickname;
    private final int activityScore;

    public MemberDto(Member member) {
        this(
                member.getId(),
                member.getCreateDate(),
                member.getModifyDate(),
                member.getEmail(),
                member.getNickname(),
                member.getActivityScore()
        );
    }

}
