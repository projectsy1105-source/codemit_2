package com.back.boundedContext.member.app;

import com.back.boundedContext.member.domain.Member;
import com.back.boundedContext.member.out.MemberRepository;
import com.back.global.global.RsData.RsData;
import com.back.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberFacade {

    private final MemberRepository memberRepository;
    private final MemberJoinUseCase memberJoinUseCase;
    private final MemberAuthUseCase memberAuthUseCase;

    @Transactional(readOnly = true)
    public long count() {
        return memberRepository.count();
    }

    @Transactional
    public RsData<Member> join(String email, String password, String nickname) {
        // 가입 규칙·해싱은 Join 유스케이스에 위임하고, 컨트롤러는 엔티티를 직접 다루지 않는다.
        return memberJoinUseCase.join(email, password, nickname);
    }

    @Transactional
    public JwtTokenProvider.TokenPair login(String email, String password) {
        // 로그인 성공 시 Access/Refresh Token 발급과 Refresh Token 저장이 하나의 트랜잭션으로 처리된다.
        return memberAuthUseCase.login(email, password);
    }

    @Transactional
    public JwtTokenProvider.TokenPair reissue(String refreshToken) {
        return memberAuthUseCase.reissue(refreshToken);
    }

    @Transactional
    public void logout(int memberId) {
        memberAuthUseCase.logout(memberId);
    }

    @Transactional
    public Optional<Member> findById(int id) { return memberRepository.findById(id); }

    /** JWT subject와 회원 조회 기준을 이메일 하나로 통일해 인증 주체가 흔들리지 않게 한다. */
    @Transactional(readOnly = true)
    public Optional<Member> findByEmail(String email) {
        return memberRepository.findByEmail(email);
    }
}
