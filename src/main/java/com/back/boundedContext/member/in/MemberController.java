package com.back.boundedContext.member.in;

import com.back.boundedContext.member.app.MemberFacade;
import com.back.boundedContext.member.domain.Member;
import com.back.boundedContext.member.dto.MemberRequest;
import com.back.boundedContext.member.dto.TokenResponse;
import com.back.global.exception.DomainException;
import com.back.global.global.RsData.RsData;
import com.back.shared.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/member/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberFacade memberFacade;

    @PostMapping("/join")
    public ResponseEntity<RsData<MemberDto>> join(@RequestBody MemberRequest.Join request) {
        requireRequest(request);
        requireText(request.username(), "username");
        requireText(request.password(), "password");
        requireText(request.nickname(), "nickname");

        RsData<Member> result = memberFacade.join(request.username(), request.password(), request.nickname());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RsData<>(result.getResultCode(), result.getMsg(), new MemberDto(result.getData())));
    }

    @PostMapping("/login")
    public RsData<TokenResponse> login(@RequestBody MemberRequest.Login request) {
        requireRequest(request);
        requireText(request.username(), "username");
        requireText(request.password(), "password");

        return new RsData<>(
                "200-1",
                "로그인되었습니다.",
                TokenResponse.from(memberFacade.login(request.username(), request.password()))
        );
    }

    @PostMapping("/reissue")
    public RsData<TokenResponse> reissue(@RequestBody MemberRequest.Reissue request) {
        requireRequest(request);
        requireText(request.refreshToken(), "refreshToken");

        return new RsData<>(
                "200-2",
                "토큰이 재발급되었습니다.",
                TokenResponse.from(memberFacade.reissue(request.refreshToken()))
        );
    }

    @PostMapping("/logout")
    public RsData<Void> logout(Authentication authentication) {
        Member member = memberFacade.findByUsername(authentication.getName())
                .orElseThrow(() -> new DomainException("401-1", "인증된 회원을 찾을 수 없습니다."));
        memberFacade.logout(member.getId());

        return new RsData<>("200-3", "로그아웃되었습니다.");
    }

    @GetMapping("/me")
    public RsData<MemberDto> me(Authentication authentication) {
        Member member = memberFacade.findByUsername(authentication.getName())
                .orElseThrow(() -> new DomainException("401-1", "인증된 회원을 찾을 수 없습니다."));

        return new RsData<>("200-4", "내 회원정보입니다.", new MemberDto(member));
    }

    @GetMapping("/randomSecureTip")
    public String randomTip() {
        return memberFacade.randomTip();
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new DomainException("400-1", "%s은(는) 필수입니다.".formatted(fieldName));
        }
    }

    private void requireRequest(Object request) {
        if (request == null) {
            throw new DomainException("400-1", "요청 본문은 필수입니다.");
        }
    }
}
