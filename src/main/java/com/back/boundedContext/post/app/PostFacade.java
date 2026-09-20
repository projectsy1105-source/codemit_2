package com.back.boundedContext.post.app;

import com.back.boundedContext.post.domain.Post;
import com.back.boundedContext.post.domain.PostMember;
import com.back.boundedContext.post.out.PostMemberRepository;
import com.back.boundedContext.post.out.PostRepository;
import com.back.global.exception.DomainException;
import com.back.global.global.RsData.RsData;
import com.back.shared.member.dto.MemberDto;
import com.back.shared.post.dto.PageResponse;
import com.back.shared.post.dto.PostDetailDto;
import com.back.shared.post.dto.PostDto;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.back.boundedContext.post.domain.QPost.post;
import static com.back.boundedContext.post.domain.QPostComment.postComment;

@Service
@RequiredArgsConstructor
public class PostFacade {
    private final PostRepository postRepository;
    private final PostMemberRepository postMemberRepository;
    private final PostWriteUseCase postWriteUseCase;
    private final JPAQueryFactory jpaQueryFactory;

    @Transactional(readOnly = true)
    public long count() {
        return postRepository.count();
    }

    @Transactional(readOnly = true)
    public Optional<Post> findById(int id) {
        return postRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<PostMember> findPostMember(String username) {
        return postMemberRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public RsData<PageResponse<PostDto>> list(String keyword, int authorId, int page, int size) {
        BooleanBuilder conditions = new BooleanBuilder();

        if (keyword != null && !keyword.isBlank()) {
            conditions.and(post.title.containsIgnoreCase(keyword)
                    .or(post.content.containsIgnoreCase(keyword)));
        }

        if (authorId > 0) {
            conditions.and(post.author.id.eq(authorId));
        }

        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : 20;

        // QPost의 필드를 사용하므로 엔티티 필드명이 바뀌면 컴파일 단계에서 바로 알 수 있다.
        List<PostDto> list = jpaQueryFactory
                .select(Projections.constructor(PostDto.class, post.id, post.createDate, post.modifyDate,
                        post.author.id, post.author.nickname, post.title, post.content))
                .from(post)
                .join(post.author)
                .where(conditions)
                .orderBy(post.id.desc())
                .offset((long) safePage * safeSize)
                .limit(safeSize)
                .fetch();

        Long totalCount = jpaQueryFactory.select(post.count())
                .from(post)
                .where(conditions)
                .fetchOne();

        int total = totalCount == null ? 0 : Integer.parseInt(totalCount.toString());
        int totalPages = (int) Math.ceil((double) total / safeSize);

        return new RsData<>("200", "조회성공", new PageResponse<>(list, safePage, safeSize, total, totalPages));
    }

    public RsData<PostDetailDto> find(int postId) {
        PostDetailDto detailDto = jpaQueryFactory.select(Projections.constructor(PostDetailDto.class, post.id, post.createDate, post.modifyDate,
                post.author.id, post.author.nickname, post.title, post.content, post.comments))
                .from(post)
                .join(post.author).fetchJoin()
                .leftJoin(post.comments, postComment).fetchJoin()
                .leftJoin(postComment.author).fetchJoin()
                .where(post.id.eq(postId))
                .fetchOne();
        return new RsData<>("200", "조회성공", detailDto);
    }

    @Transactional
    public RsData<PostDto> write(PostMember author, String title, String content) {
        return postWriteUseCase.write(author, title, content);
    }

    @Transactional
    public RsData<PostDto> update(int postId, PostMember author, String title, String content) {
        Post post = postRepository.findById(postId).orElseThrow(()
                -> new DomainException("404-1", "유효하지 않은 글 번호입니다."));
        if (post.getAuthor().getId() != author.getId()) {
            throw new DomainException("403-1", "수정 권한이 없습니다.");
        }
        if (post.isDeleted()) {
            throw new DomainException("404-2", "삭제된 글 입니다.");
        }

        if (title != null && !title.isBlank()) {
            post.changeTitle(title);
        }

        if (content != null && !content.isBlank()) {
            post.changeContent(content);
        }

        return new RsData<>("201-2", "%d 번 글이 수정되었습니다.".formatted(postId), new PostDto(post));
    }

    @Transactional
    public RsData delete(int postId, PostMember author) {
        Post post = postRepository.findById(postId).orElseThrow(()
                -> new DomainException("404-1", "유효하지 않은 글 번호입니다."));
        if (post.getAuthor().getId() != author.getId()) {
            throw new DomainException("403-1", "수정 권한이 없습니다.");
        }
        if (post.isDeleted()) {
            throw new DomainException("404-2", "삭제된 글 입니다.");
        }
        post.delete();

        return new RsData<>("201-3","%d 번 글이 삭제되었습니다.");
    }



    @Transactional
    public PostMember syncMember(MemberDto memberDto) {
        PostMember m = new PostMember(memberDto.getId(), memberDto.getCreateDate(), memberDto.getModifyDate(), memberDto.getUsername(), "", memberDto.getNickname(), memberDto.getActivityScore());

        return postMemberRepository.save(m);
    }
}
