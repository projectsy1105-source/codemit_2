package com.back.boundedContext.post.in;

import com.back.boundedContext.post.app.PostFacade;
import com.back.boundedContext.post.domain.PostMember;
import com.back.boundedContext.post.dto.PostRequest;
import com.back.global.exception.DomainException;
import com.back.global.global.RsData.RsData;
import com.back.shared.post.dto.PageResponse;
import com.back.shared.post.dto.PostDetailDto;
import com.back.shared.post.dto.PostDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/post")
@RequiredArgsConstructor
public class PostController {

    private final PostFacade postFacade;

    public RsData<PageResponse<PostDto>> list(PostRequest.List list) {

        return postFacade.list(list.keyword(), list.authorId(), list.page(), list.size());
    }

    public RsData<PostDetailDto> find(int id) {
        return postFacade.find(id);
    }

    public RsData<PostDto> write(Authentication authentication, @RequestBody PostRequest.PostContent content) {
        PostMember author = postFacade.findPostMember(authentication.getName())
                .orElseThrow(() -> new DomainException("401-1", "인증된 회원을 찾을 수 없습니다."));

        return postFacade.write(author, content.title(), content.content());
    }

    public RsData<PostDto> update(Authentication authentication, @RequestBody PostRequest.PostContent content) {
        PostMember author = postFacade.findPostMember(authentication.getName())
                .orElseThrow(() -> new DomainException("401-1", "인증된 회원을 찾을 수 없습니다."));
        return postFacade.update(content.id(), author, content.title(), content.content());
    }

    public RsData delete(Authentication authentication, @RequestBody PostRequest.PostContent content) {
        PostMember author = postFacade.findPostMember(authentication.getName())
                .orElseThrow(() -> new DomainException("401-1", "인증된 회원을 찾을 수 없습니다."));
        return postFacade.delete(content.id(), author);
    }

}
