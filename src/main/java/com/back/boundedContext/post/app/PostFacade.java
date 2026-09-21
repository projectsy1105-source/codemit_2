package com.back.boundedContext.post.app;

import com.back.boundedContext.post.domain.Post;
import com.back.boundedContext.post.domain.PostComment;
import com.back.boundedContext.post.domain.PostMember;
import com.back.boundedContext.post.out.PostCommentRepository;
import com.back.boundedContext.post.out.PostMemberRepository;
import com.back.boundedContext.post.out.PostRepository;
import com.back.global.eventPublisher.EventPublisher;
import com.back.global.exception.DomainException;
import com.back.global.global.RsData.RsData;
import com.back.shared.member.dto.MemberDto;
import com.back.shared.post.dto.PageResponse;
import com.back.shared.post.dto.PostCommentDto;
import com.back.shared.post.dto.PostDetailDto;
import com.back.shared.post.dto.PostDto;
import com.back.shared.post.event.PostCommentCreatedEvent;
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
    private final PostCommentRepository postCommentRepository;
    private final PostWriteUseCase postWriteUseCase;
    private final EventPublisher eventPublisher;
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
    public Optional<PostMember> findPostMember(String email) {
        return postMemberRepository.findByEmail(email);
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
        int safeSize = size > 0 ? Math.min(size, 100) : 20;

        // 작성자와 댓글 수를 한 번의 집계 쿼리로 조회하므로 목록 크기만큼 쿼리가 늘지 않는다.
        List<PostDto> posts = jpaQueryFactory
                .select(Projections.constructor(PostDto.class,
                        post.id, post.createDate, post.modifyDate,
                        post.author.id, post.author.nickname, post.title, post.content,
                        postComment.id.count()))
                .from(post)
                .join(post.author)
                .leftJoin(post.comments, postComment)
                .where(conditions)
                .groupBy(post.id, post.createDate, post.modifyDate,
                        post.author.id, post.author.nickname, post.title, post.content)
                .orderBy(post.createDate.desc(), post.id.desc())
                .offset((long) safePage * safeSize)
                .limit(safeSize)
                .fetch();

        Long totalElements = jpaQueryFactory.select(post.count())
                .from(post)
                .where(conditions)
                .fetchOne();
        long total = totalElements == null ? 0 : totalElements;
        int totalPages = (int) Math.ceil((double) total / safeSize);

        return new RsData<>("200-1", "게시글 목록을 조회했습니다.",
                new PageResponse<>(posts, safePage, safeSize, total, totalPages));
    }

    @Transactional(readOnly = true)
    public RsData<PostDetailDto> find(int postId) {
        Post foundPost = jpaQueryFactory.selectFrom(post)
                .join(post.author).fetchJoin()
                .leftJoin(post.comments, postComment).fetchJoin()
                .leftJoin(postComment.author).fetchJoin()
                .where(post.id.eq(postId))
                .distinct()
                .fetchOne();

        if (foundPost == null) {
            throw new DomainException("404-1", "존재하지 않는 게시글입니다.");
        }
        return new RsData<>("200-2", "게시글을 조회했습니다.", new PostDetailDto(foundPost));
    }

    @Transactional
    public RsData<PostDto> write(PostMember author, String title, String content) {
        return postWriteUseCase.write(author, title, content);
    }

    @Transactional
    public RsData<PostDto> update(int postId, PostMember author, String title, String content) {
        Post foundPost = getPost(postId);
        assertAuthor(foundPost.getAuthor(), author);
        foundPost.changeTitle(title);
        foundPost.changeContent(content);
        return new RsData<>("200-3", "%d번 글이 수정되었습니다.".formatted(postId), new PostDto(foundPost));
    }

    @Transactional
    public RsData<Void> delete(int postId, PostMember author) {
        Post foundPost = getPost(postId);
        assertAuthor(foundPost.getAuthor(), author);
        // cascade = ALL/orphanRemoval 설정으로 댓글도 같은 트랜잭션에서 물리 삭제된다.
        postRepository.delete(foundPost);
        return new RsData<>("200-4", "%d번 글과 댓글이 삭제되었습니다.".formatted(postId));
    }

    @Transactional(readOnly = true)
    public RsData<List<PostCommentDto>> listComments(int postId) {
        getPost(postId);
        List<PostCommentDto> comments = postCommentRepository.findByPost_IdOrderByCreateDateAsc(postId)
                .stream()
                .map(PostCommentDto::new)
                .toList();
        return new RsData<>("200-5", "댓글 목록을 조회했습니다.", comments);
    }

    @Transactional
    public RsData<PostCommentDto> writeComment(int postId, PostMember author, String content) {
        Post foundPost = getPost(postId);
        PostComment savedComment = postCommentRepository.save(foundPost.addComment(author, content));
        // 저장 후 이벤트를 발행해야 댓글 ID가 0인 DTO가 전달되지 않는다.
        eventPublisher.publisher(new PostCommentCreatedEvent(new PostCommentDto(savedComment)));
        return new RsData<>("201-2", "댓글이 생성되었습니다.", new PostCommentDto(savedComment));
    }

    @Transactional
    public RsData<PostCommentDto> updateComment(int commentId, PostMember author, String content) {
        PostComment comment = getComment(commentId);
        assertAuthor(comment.getAuthor(), author);
        comment.changeContent(content);
        return new RsData<>("200-6", "댓글이 수정되었습니다.", new PostCommentDto(comment));
    }

    @Transactional
    public RsData<Void> deleteComment(int commentId, PostMember author) {
        PostComment comment = getComment(commentId);
        assertAuthor(comment.getAuthor(), author);
        postCommentRepository.delete(comment);
        return new RsData<>("200-7", "댓글이 삭제되었습니다.");
    }

    @Transactional(readOnly = true)
    public PostMember getRequiredPostMember(String email) {
        return findPostMember(email)
                .orElseThrow(() -> new DomainException("401-1", "인증된 회원을 찾을 수 없습니다."));
    }

    @Transactional
    public PostMember syncMember(MemberDto memberDto) {
        PostMember member = new PostMember(
                memberDto.getId(), memberDto.getCreateDate(), memberDto.getModifyDate(),
                memberDto.getEmail(), "", memberDto.getNickname(), memberDto.getActivityScore());
        return postMemberRepository.save(member);
    }

    private Post getPost(int postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new DomainException("404-1", "존재하지 않는 게시글입니다."));
    }

    private PostComment getComment(int commentId) {
        return postCommentRepository.findById(commentId)
                .orElseThrow(() -> new DomainException("404-2", "존재하지 않는 댓글입니다."));
    }

    /** ID 비교는 객체 인스턴스가 달라도 작성자 권한을 정확히 판별한다. */
    private void assertAuthor(PostMember resourceAuthor, PostMember requestMember) {
        if (resourceAuthor.getId() != requestMember.getId()) {
            throw new DomainException("403-1", "작성자만 수정하거나 삭제할 수 있습니다.");
        }
    }
}
