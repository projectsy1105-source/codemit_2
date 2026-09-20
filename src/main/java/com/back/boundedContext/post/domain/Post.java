package com.back.boundedContext.post.domain;


import com.back.global.entity.BaseIdAndTime;
import com.back.shared.post.dto.PostCommentDto;
import com.back.shared.post.event.PostCommentCreatedEvent;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.CascadeType.REMOVE;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "POST_POST")
@NoArgsConstructor
@Getter
public class Post extends BaseIdAndTime {

    @ManyToOne(fetch = LAZY)
    private PostMember author;

    private State state;

    private String title;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @OneToMany(mappedBy = "post", cascade = {PERSIST, REMOVE}, orphanRemoval = true)
    private List<PostComment> comments = new ArrayList<>();

    public Post(PostMember author, String title, String content) {
        this.author = author;
        this.state = State.ACTIVE;
        this.title = title;
        this.content = content;
    }

    public PostComment addComment(PostMember author, String content) {
        PostComment postComment = new PostComment(this, author, content);

        comments.add(postComment);

        publishEvent(new PostCommentCreatedEvent(new PostCommentDto(postComment)));
        return postComment;
    }

    public void changeTitle(String title) {
        this.title = title;
    }

    public void changeContent(String content) {
        this.content = content;
    }

    public void delete() {
        this.state = State.DELETED;
    }

    public boolean isDeleted() {
        return this.state == State.DELETED;
    }

    public boolean hasComments() {
        return !comments.isEmpty();
    }

    public enum State {
        ACTIVE, DELETED
    }
}
