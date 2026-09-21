package com.back.global.exception;

import com.back.global.global.RsData.RsData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<RsData<Void>> handleDomainException(DomainException exception) {
        return ResponseEntity.status(toHttpStatus(exception.getResultCode()))
                .body(new RsData<>(exception.getResultCode(), exception.getMsg()));
    }

    /** DTO 검증 실패를 400으로 바꿔 클라이언트가 필드 오류를 바로 알 수 있게 한다. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RsData<Void>> handleValidation(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = fieldError == null ? "요청 값이 올바르지 않습니다."
                : Objects.requireNonNullElse(fieldError.getDefaultMessage(), "요청 값이 올바르지 않습니다.");
        return badRequest(message);
    }

    @ExceptionHandler({ConstraintViolationException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<RsData<Void>> handleBadRequest(Exception exception) {
        return badRequest("요청 본문 또는 파라미터가 올바르지 않습니다.");
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpRequestMethodNotSupportedException.class,
            HttpMediaTypeNotSupportedException.class
    })
    public ResponseEntity<RsData<Void>> handleInvalidHttpRequest(Exception exception) {
        return badRequest("요청 주소, 메서드 또는 값이 올바르지 않습니다.");
    }

    /** 서비스의 선행 중복 검사와 별도로 DB 유니크 제약 위반도 일관되게 처리한다. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RsData<Void>> handleDuplicate(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new RsData<>("409-1", "이미 존재하는 데이터입니다."));
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<RsData<Void>> handleNotFound(Exception exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new RsData<>("404-0", "요청한 API를 찾을 수 없습니다."));
    }

    /** 예상하지 못한 예외의 내부 정보는 노출하지 않고 서버 로그에만 남긴다. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RsData<Void>> handleUnexpected(Exception exception) {
        log.error("처리하지 못한 예외", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new RsData<>("500-1", "서버 오류가 발생했습니다."));
    }

    private ResponseEntity<RsData<Void>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new RsData<>("400-1", message));
    }

    private HttpStatus toHttpStatus(String resultCode) {
        if (resultCode.startsWith("400")) return HttpStatus.BAD_REQUEST;
        if (resultCode.startsWith("401")) return HttpStatus.UNAUTHORIZED;
        if (resultCode.startsWith("403")) return HttpStatus.FORBIDDEN;
        if (resultCode.startsWith("404")) return HttpStatus.NOT_FOUND;
        if (resultCode.startsWith("409")) return HttpStatus.CONFLICT;
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
