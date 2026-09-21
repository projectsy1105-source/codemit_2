plugins {
    java
    // Spring Boot 3.5.16: Boot 3.x 최종 OSS 패치로 Java 21 LTS와 안정적으로 호환
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com"
version = "0.0.1-SNAPSHOT"
description = "back"

val querydslVersion = "5.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    // Spring Boot 3.5.16 BOM -> Spring Security 6.5.11: BCrypt 비밀번호 해시와 JWT 인증 필터 제공
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    // JJWT 0.13.0: HS256 JWT 생성과 서명/만료 검증을 간단히 처리
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    // JJWT 0.13.0: 실행 시 JWT 구현체를 제공
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    // JJWT 0.13.0: JWT Claim JSON 처리를 위해 Jackson 연동 제공
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")
    // QueryDSL 5.1.0 jakarta: 엔티티에서 Q 타입을 생성해 타입 안전한 동적 JPA 쿼리를 작성
    implementation("com.querydsl:querydsl-jpa:$querydslVersion:jakarta")
    compileOnly("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    // Spring Boot 3.5.16 BOM -> H2 2.3.232: 로컬 학습용 회원/Refresh Token 저장소와 H2 콘솔 지원
    runtimeOnly("com.h2database:h2")
    annotationProcessor("org.projectlombok:lombok")
    // QueryDSL 5.1.0 jakarta: 컴파일 시 QPost 등 Q 타입을 자동 생성
    annotationProcessor("com.querydsl:querydsl-apt:$querydslVersion:jakarta")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
