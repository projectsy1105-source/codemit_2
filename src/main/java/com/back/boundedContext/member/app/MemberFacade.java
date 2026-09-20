package com.back.boundedContext.member.app;

import com.back.boundedContext.member.domain.Member;
import com.back.boundedContext.member.domain.MemberPolicy;
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
    private final MemberPolicy memberPolicy;

    @Transactional(readOnly = true)
    public long count() {
        return memberRepository.count();
    }

    @Transactional
    public RsData<Member> join(String username, String password, String nickname) {
        return memberJoinUseCase.join(username, password, nickname);
    }

    @Transactional
    public JwtTokenProvider.TokenPair login(String username, String password) {
        return memberAuthUseCase.login(username, password);
    }

    @Transactional
    public JwtTokenProvider.TokenPair reissue(String refreshToken) {
        return memberAuthUseCase.reissue(refreshToken);
    }

    @Transactional
    public void logout(int memberId) {
        memberAuthUseCase.logout(memberId);
    }

    public String randomTip() {
//        public String randomTip(int memberId) {
//        Member member = memberRepository.findById(memberId).get();
//        int dday = memberPolicy.getPasswordChangeDays() - Period.between(member.getModifyDate().toLocalDate(), LocalDate.now()).getDays();
        return "비밀번호의 유효기간은 %d일 입니다.".formatted(memberPolicy.getPasswordChangeDays());
    }

    @Transactional
    public Optional<Member> findById(int id) { return memberRepository.findById(id); }

    @Transactional(readOnly = true)
    public Optional<Member> findByUsername(String username) {
        return memberRepository.findByUsername(username);
    }
}
