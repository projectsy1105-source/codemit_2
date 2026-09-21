package com.back.boundedContext.post.domain;


import com.back.global.entity.BaseIdAndTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "POST_POST")
@NoArgsConstructor
@Getter
public class Post extends BaseIdAndTime {

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(nullable = false)
    private PostMember author;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    // 글을 물리 삭제하면 연관 댓글도 함께 삭제한다는 정책을 JPA 관계에 명시한다.
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostComment> comments = new ArrayList<>();

    public Post(PostMember author, String title, String content) {
        this.author = author;
        this.title = title;
        this.content = content;
    }

    public PostComment addComment(PostMember author, String content) {
        PostComment postComment = new PostComment(this, author, content);

        comments.add(postComment);

        return postComment;
    }

    public void changeTitle(String title) {
        this.title = title;
    }

    public void changeContent(String content) {
        this.content = content;
    }

    public boolean hasComments() {
        return !comments.isEmpty();
    }
}
