package com.back.boundedContext.post.app;

import com.back.boundedContext.post.domain.Post;
import com.back.boundedContext.post.domain.PostMember;
import com.back.boundedContext.post.out.PostRepository;
import com.back.global.eventPublisher.EventPublisher;
import com.back.global.global.RsData.RsData;
import com.back.shared.member.out.MemberApiClient;
import com.back.shared.post.dto.PostDto;
import com.back.shared.post.event.PostCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostWriteUseCase {

    private final PostRepository postRepository;
    private final EventPublisher eventPublisher;
    private final MemberApiClient  memberApiClient;

    public RsData<PostDto> write(PostMember author, String title, String content) {
        Post post = new Post(author, title, content);
        postRepository.save(post);

        PostDto postDto = new PostDto(post);

        eventPublisher.publisher(new PostCreatedEvent(postDto));
        String tip = memberApiClient.getRandomSecureTip();

        return new RsData<>("201-1", "%d번 글이 생성되었습니다. 보안 팁 : %s".formatted(post.getId(), tip), postDto);
    }
}
