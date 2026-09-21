package com.back.boundedContext.member.in;

import com.back.boundedContext.member.app.MemberFacade;
import com.back.boundedContext.member.domain.Member;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@Profile("!test")
@Slf4j
public class MemberDataInit {
    private final MemberDataInit self;
    private final MemberFacade memberFacade;

    public MemberDataInit(
            @Lazy MemberDataInit self,
            MemberFacade memberFacade
    ) {
        this.self = self;
        this.memberFacade = memberFacade;
    }

    @Bean
    @Order(1)
    public ApplicationRunner memberBaseInitDataRunner() {
        return args -> {
            self.makeBaseMembers();
        };
    }

    @Transactional
    public void makeBaseMembers() {
        if (memberFacade.count() > 0) return;

        // 초기 데이터도 실제 가입 규칙(이메일/8자 이상 비밀번호)을 그대로 따른다.
        memberFacade.join("system@example.com", "password1234", "시스템");
        memberFacade.join("holding@example.com", "password1234", "홀딩");
        memberFacade.join("admin@example.com", "password1234", "관리자");
        memberFacade.join("user1@example.com", "password1234", "유저1");
        memberFacade.join("user2@example.com", "password1234", "유저2");
        memberFacade.join("user3@example.com", "password1234", "유저3");
    }

}
