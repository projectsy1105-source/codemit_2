package com.back.boundedContext.post.in;

import com.back.boundedContext.post.app.PostFacade;
import com.back.boundedContext.post.domain.Post;
import com.back.boundedContext.post.domain.PostMember;
import com.back.global.global.RsData.RsData;
import com.back.shared.post.dto.PostDto;
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
public class PostDataInit {
    private final PostDataInit self;
    private final PostFacade postFacade;

    public PostDataInit(
            @Lazy PostDataInit self,
            PostFacade postFacade
    ) {
        this.self = self;
        this.postFacade = postFacade;
    }

    @Bean
    @Order(2)
    public ApplicationRunner PostBaseInitDataRunner() {
        return args -> {
            self.makeBasePosts();
            self.makeBasePostComments();
        };
    }

    @Transactional
    public void makeBasePosts() {
        if (postFacade.count() > 0) return;

        PostMember user1 = postFacade.findPostMember("user1@example.com").orElseThrow();
        PostMember user2 = postFacade.findPostMember("user2@example.com").orElseThrow();
        PostMember user3 = postFacade.findPostMember("user3@example.com").orElseThrow();

        RsData<PostDto> post1 = postFacade.write(user1, "제목1", "내용1"); log.debug(post1.getMsg());
        RsData<PostDto> post2 = postFacade.write(user1, "제목2", "내용2"); log.debug(post2.getMsg());
        RsData<PostDto> post3 = postFacade.write(user1, "제목3", "내용3"); log.debug(post3.getMsg());
        RsData<PostDto> post4 = postFacade.write(user2, "제목4", "내용4"); log.debug(post4.getMsg());
        RsData<PostDto> post5 = postFacade.write(user2, "제목5", "내용5"); log.debug(post5.getMsg());
        RsData<PostDto> post6 = postFacade.write(user3, "제목6", "내용6"); log.debug(post6.getMsg());
    }

    @Transactional
    public void makeBasePostComments() {
        Post post1 = postFacade.findById(1).get();
        Post post2 = postFacade.findById(2).get();
        Post post3 = postFacade.findById(3).get();
        Post post4 = postFacade.findById(4).get();
        Post post5 = postFacade.findById(5).get();
        Post post6 = postFacade.findById(6).get();

        PostMember user1Member = postFacade.findPostMember("user1@example.com").orElseThrow();
        PostMember user2Member = postFacade.findPostMember("user2@example.com").orElseThrow();
        PostMember user3Member = postFacade.findPostMember("user3@example.com").orElseThrow();

        if (post1.hasComments()) return;

        postFacade.writeComment(post1.getId(), user1Member, "댓글1");
        postFacade.writeComment(post1.getId(), user2Member, "댓글2");
        postFacade.writeComment(post1.getId(), user3Member, "댓글3");

        postFacade.writeComment(post2.getId(), user2Member, "댓글4");
        postFacade.writeComment(post2.getId(), user2Member, "댓글5");

        postFacade.writeComment(post3.getId(), user3Member, "댓글6");
        postFacade.writeComment(post3.getId(), user3Member, "댓글7");

        postFacade.writeComment(post4.getId(), user1Member, "댓글8");
        postFacade.writeComment(post5.getId(), user1Member, "댓글9");
    }
}
