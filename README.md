# 게시판 REST API

Spring Boot 3.x, Spring Security, JPA(H2)로 만든 학습용 게시판 API입니다. 비로그인 사용자는 글과 댓글을 읽을 수 있고, 로그인한 사용자는 자신의 글·댓글만 수정하거나 삭제할 수 있습니다.

## 실행 방법

- JDK 21
- 별도 DB 설치는 필요 없습니다. 개발 프로필은 프로젝트 루트에 H2 파일 DB를 자동 생성합니다.

프로젝트 루트의 `.env`에서 JWT 서명 키를 읽습니다. `.env`는 Git에서 무시되므로 절대 커밋하지 않습니다. 최초에 `.env`가 없다면 아래 명령으로 키를 만듭니다.

```bash
openssl rand -base64 48 | sed 's/^/JWT_SECRET=/' > .env
```

그 다음 서버는 아래 한 줄로 실행합니다.

```bash
./gradlew bootRun
```

서버는 `http://localhost:8080`에서 실행됩니다. 개발 중 H2 콘솔은 `http://localhost:8080/h2-console`이며 JDBC URL은 `jdbc:h2:./db_dev;MODE=MySQL`입니다.

테스트는 다음 명령으로 실행합니다. 테스트는 별도의 인메모리 H2 DB와 테스트 전용 키를 사용합니다.

```bash
./gradlew test
```

## 공통 응답 형식

성공과 오류 모두 다음 형식입니다. `data`는 데이터가 없을 때 `null`입니다.

```json
{
  "resultCode": "200-1",
  "msg": "게시글 목록을 조회했습니다.",
  "data": {}
}
```

대표 오류는 다음과 같습니다.

```json
{
  "resultCode": "403-1",
  "msg": "작성자만 수정하거나 삭제할 수 있습니다.",
  "data": null
}
```

## API 명세

### 회원·인증

| 메서드 | 주소 | 인증 | 요청 본문 | 성공 상태 |
|---|---|---|---|---|
| POST | `/api/v1/members/signup` | 불필요 | `email`, `password`(8자 이상), `nickname` | 201 |
| POST | `/api/v1/members/login` | 불필요 | `email`, `password` | 200 |
| POST | `/api/v1/members/reissue` | 불필요 | `refreshToken` | 200 |
| POST | `/api/v1/members/logout` | 필요 | 없음 | 200 |
| GET | `/api/v1/members/me` | 필요 | 없음 | 200 |

가입 응답의 `data`는 `id`, `createDate`, `modifyDate`, `email`, `nickname`, `activityScore`이며 비밀번호는 절대 포함하지 않습니다. 로그인/재발급의 `data`는 `accessToken`, `refreshToken`입니다.

### 게시글

| 메서드 | 주소 | 인증 | 요청/쿼리 | 성공 상태 |
|---|---|---|---|---|
| GET | `/api/v1/posts` | 불필요 | `keyword`, `authorId`, `page`(0부터), `size`(최대 100) | 200 |
| GET | `/api/v1/posts/{postId}` | 불필요 | 없음 | 200 |
| POST | `/api/v1/posts` | 필요 | `title`(최대 200자), `content` | 201 |
| PUT | `/api/v1/posts/{postId}` | 작성자 | `title`, `content` | 200 |
| DELETE | `/api/v1/posts/{postId}` | 작성자 | 없음 | 200 |

목록 `data`는 다음과 같습니다. 각 항목의 `authorName`과 `commentCount`를 함께 반환합니다.

```json
{
  "content": [
    {
      "id": 1,
      "createDate": "2026-09-21T12:00:00",
      "modifyDate": "2026-09-21T12:00:00",
      "authorId": 1,
      "authorName": "코덱스",
      "title": "첫 글",
      "content": "본문",
      "commentCount": 1
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### 댓글

| 메서드 | 주소 | 인증 | 요청 본문 | 성공 상태 |
|---|---|---|---|---|
| GET | `/api/v1/posts/{postId}/comments` | 불필요 | 없음 | 200 |
| POST | `/api/v1/posts/{postId}/comments` | 필요 | `content`(최대 2,000자) | 201 |
| PUT | `/api/v1/comments/{commentId}` | 작성자 | `content` | 200 |
| DELETE | `/api/v1/comments/{commentId}` | 작성자 | 없음 | 200 |

댓글 응답의 `data`는 `id`, 작성·수정 시각, `postId`, `authorId`, `authorName`, `content`입니다.

### 오류 상태

| 상태 | 발생 조건 |
|---|---|
| 400 | 이메일/비밀번호/본문 등 입력값이 잘못된 경우 |
| 401 | 로그인하지 않았거나 유효하지 않은 Access Token인 경우 |
| 403 | 다른 사용자의 글 또는 댓글을 수정·삭제하려는 경우 |
| 404 | 없는 게시글·댓글 또는 API 주소인 경우 |
| 409 | 이미 가입된 이메일인 경우 |

## 설계 설명

### JWT 인증

REST API 서버는 서버 세션을 저장하지 않아도 되는 JWT 방식을 선택했습니다. Access Token은 30분, Refresh Token은 14일입니다. Refresh Token은 DB에 BCrypt 해시로만 보관하고 재발급 때 교체하므로, 이전 Refresh Token은 즉시 사용할 수 없습니다. 비밀번호도 같은 BCrypt 방식으로 저장하며 원문은 응답 또는 DB에 저장하지 않습니다.

### 엔티티 관계와 삭제 정책

논리적으로 회원은 여러 게시글·댓글을 작성하고, 게시글은 여러 댓글을 가집니다. 게시글 컨텍스트는 작성자 정보를 `PostMember` 복제 엔티티로 관리하며 회원 변경 이벤트로 동기화합니다. 게시글을 삭제하면 `cascade = ALL`과 `orphanRemoval = true`로 연결된 댓글도 물리 삭제합니다.

### N+1 방지

게시글 목록은 QueryDSL에서 작성자를 `join`하고 댓글을 `left join`한 뒤 `count(comment.id)`로 집계합니다. 따라서 작성자 닉네임과 댓글 수를 얻기 위해 글 수만큼 추가 쿼리가 실행되지 않습니다. 댓글 목록은 `@EntityGraph(attributePaths = "author")`로 작성자를 한 번에 조회합니다.

### API 모양

리소스를 복수형 `/posts`, `/comments`, `/members`로 표현했습니다. 글에 종속된 댓글의 생성·조회는 `/posts/{postId}/comments`로, 댓글 자체 수정·삭제는 `/comments/{commentId}`로 분리했습니다. 생성은 201, 조회·수정·삭제는 표준 응답 본문과 함께 200을 사용합니다.

## curl 실행 예시

아래는 서버를 실행한 뒤 가입 → 로그인 → 글 → 댓글 → 목록 조회 순서입니다. 응답의 토큰은 매번 달라지므로 `<ACCESS_TOKEN>` 표시는 실제 로그인 응답의 값으로 바꿉니다.

```bash
curl -i -X POST http://localhost:8080/api/v1/members/signup \
  -H 'Content-Type: application/json' \
  -d '{"email":"codex@example.com","password":"password1234","nickname":"코덱스"}'
```

```http
HTTP/1.1 201

{"resultCode":"201-1","msg":"1번 회원이 생성되었습니다.","data":{"id":1,"email":"codex@example.com","nickname":"코덱스"}}
```

```bash
curl -X POST http://localhost:8080/api/v1/members/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"codex@example.com","password":"password1234"}'
```

```json
{"resultCode":"200-1","msg":"로그인되었습니다.","data":{"accessToken":"<ACCESS_TOKEN>","refreshToken":"<REFRESH_TOKEN>"}}
```

```bash
curl -X POST http://localhost:8080/api/v1/posts \
  -H 'Authorization: Bearer <ACCESS_TOKEN>' \
  -H 'Content-Type: application/json' \
  -d '{"title":"첫 글","content":"게시판 API 본문"}'

curl -X POST http://localhost:8080/api/v1/posts/1/comments \
  -H 'Authorization: Bearer <ACCESS_TOKEN>' \
  -H 'Content-Type: application/json' \
  -d '{"content":"첫 댓글"}'

curl 'http://localhost:8080/api/v1/posts?page=0&size=20'
```

목록의 응답은 `data.content[0].authorName`과 `data.content[0].commentCount`를 포함합니다.

로그인 없이 쓰기를 시도하면 401입니다.

```bash
curl -i -X POST http://localhost:8080/api/v1/posts \
  -H 'Content-Type: application/json' \
  -d '{"title":"실패","content":"로그인 필요"}'
```

```http
HTTP/1.1 401

{"resultCode":"401-1","msg":"로그인이 필요합니다.","data":null}
```

다른 사용자의 Access Token으로 글을 수정하면 403입니다.

```bash
curl -i -X PUT http://localhost:8080/api/v1/posts/1 \
  -H 'Authorization: Bearer <OTHER_ACCESS_TOKEN>' \
  -H 'Content-Type: application/json' \
  -d '{"title":"변조","content":"변조 시도"}'
```

```http
HTTP/1.1 403

{"resultCode":"403-1","msg":"작성자만 수정하거나 삭제할 수 있습니다.","data":null}
```
