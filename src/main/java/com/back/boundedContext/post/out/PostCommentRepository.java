package com.back.boundedContext.post.out;

import com.back.boundedContext.post.domain.PostComment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostCommentRepository extends JpaRepository<PostComment, Integer> {

    /** 댓글 목록을 만들 때 작성자를 함께 가져와 댓글마다 작성자 조회가 추가되는 N+1을 막는다. */
    @EntityGraph(attributePaths = "author")
    List<PostComment> findByPost_IdOrderByCreateDateAsc(int postId);
}
