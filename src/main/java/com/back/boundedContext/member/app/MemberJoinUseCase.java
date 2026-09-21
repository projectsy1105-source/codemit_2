package com.back.boundedContext.member.app;

import com.back.boundedContext.member.domain.Member;
import com.back.boundedContext.member.out.MemberRepository;
import com.back.global.eventPublisher.EventPublisher;
import com.back.global.exception.DomainException;
import com.back.global.global.RsData.RsData;
import com.back.shared.member.dto.MemberDto;
import com.back.shared.member.event.MemberJoinedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberJoinUseCase {

    private final MemberRepository memberRepository;
    private final EventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;

    public RsData<Member> join(String email, String password, String nickname) {
        validate(email, password, nickname);
        memberRepository.findByEmail(email).ifPresent(m -> {
            throw new DomainException("409-1", "이미 가입된 이메일입니다.");
        });
        // 원문 비밀번호는 저장하지 않고 BCrypt 해시값만 DB에 보관한다.
        Member member = memberRepository.save(new Member(email, passwordEncoder.encode(password), nickname));
        eventPublisher.publisher(new MemberJoinedEvent(new MemberDto(member)));

        return new RsData<>("201-1", "%d번 회원이 생성되었습니다.".formatted(member.getId()), member);
    }

    /** 컨트롤러 밖에서 가입 유스케이스를 호출해도 핵심 회원 규칙이 깨지지 않게 한다. */
    private void validate(String email, String password, String nickname) {
        if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new DomainException("400-1", "email 형식이 올바르지 않습니다.");
        }
        if (password == null || password.length() < 8) {
            throw new DomainException("400-1", "password는 8자 이상이어야 합니다.");
        }
        if (nickname == null || nickname.isBlank()) {
            throw new DomainException("400-1", "nickname은 필수입니다.");
        }
    }
}
