package com.back.boundedContext.member;

import com.back.boundedContext.member.domain.Member;
import com.back.boundedContext.member.out.MemberRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MemberAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 회원가입은_이메일을_검증하고_BCrypt로_비밀번호를_저장한다() throws Exception {
        String email = uniqueEmail("join");
        MvcResult result = mockMvc.perform(post("/api/v1/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password1234","nickname":"가입유저"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value(email))
                .andReturn();

        Member member = memberRepository.findByEmail(email).orElseThrow();
        assertNotEquals("password1234", member.getPassword());
        assertTrue(passwordEncoder.matches("password1234", member.getPassword()));
        assertFalse(result.getResponse().getContentAsString().contains("password1234"));

        mockMvc.perform(post("/api/v1/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"short\",\"nickname\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    void 인증과_게시글_댓글_권한_오류를_검증한다() throws Exception {
        Login writer = signupAndLogin("writer");
        Login other = signupAndLogin("other");

        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"익명 글\",\"content\":\"내용\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));

        MvcResult postResult = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + writer.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"테스트 글\",\"content\":\"테스트 본문\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.commentCount").value(0))
                .andReturn();
        int postId = body(postResult).path("data").path("id").asInt();

        MvcResult commentResult = mockMvc.perform(post("/api/v1/posts/{postId}/comments", postId)
                        .header("Authorization", "Bearer " + writer.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"첫 댓글\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        int commentId = body(commentResult).path("data").path("id").asInt();

        mockMvc.perform(get("/api/v1/posts").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].commentCount").value(1));

        mockMvc.perform(put("/api/v1/posts/{postId}", postId)
                        .header("Authorization", "Bearer " + other.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"변조\",\"content\":\"변조\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));

        mockMvc.perform(delete("/api/v1/comments/{commentId}", commentId)
                        .header("Authorization", "Bearer " + other.accessToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));

        mockMvc.perform(get("/api/v1/posts/{postId}", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    private Login signupAndLogin(String prefix) throws Exception {
        String email = uniqueEmail(prefix);
        mockMvc.perform(post("/api/v1/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password1234","nickname":"%s"}
                                """.formatted(email, prefix)))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"password1234\"}".formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();
        return new Login(email, body(loginResult).path("data").path("accessToken").asText());
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    private record Login(String email, String accessToken) {
    }
}
