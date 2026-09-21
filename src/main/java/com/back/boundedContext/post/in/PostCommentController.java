package com.back.boundedContext.post.in;

import com.back.boundedContext.post.app.PostFacade;
import com.back.boundedContext.post.domain.PostMember;
import com.back.boundedContext.post.dto.PostRequest;
import com.back.global.global.RsData.RsData;
import com.back.shared.post.dto.PostCommentDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PostCommentController {

    private final PostFacade postFacade;

    @GetMapping("/posts/{postId}/comments")
    public RsData<List<PostCommentDto>> list(@PathVariable int postId) {
        return postFacade.listComments(postId);
    }

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<RsData<PostCommentDto>> write(
            @PathVariable int postId,
            Authentication authentication,
            @Valid @RequestBody PostRequest.Comment request
    ) {
        PostMember author = postFacade.getRequiredPostMember(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postFacade.writeComment(postId, author, request.content()));
    }

    @PutMapping("/comments/{commentId}")
    public RsData<PostCommentDto> update(
            @PathVariable int commentId,
            Authentication authentication,
            @Valid @RequestBody PostRequest.Comment request
    ) {
        PostMember author = postFacade.getRequiredPostMember(authentication.getName());
        return postFacade.updateComment(commentId, author, request.content());
    }

    @DeleteMapping("/comments/{commentId}")
    public RsData<Void> delete(@PathVariable int commentId, Authentication authentication) {
        PostMember author = postFacade.getRequiredPostMember(authentication.getName());
        return postFacade.deleteComment(commentId, author);
    }
}
