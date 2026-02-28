package com.dong.board.exception;

import com.dong.board.security.LoggingFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * 전역 예외 처리 핸들러
 * - 모든 예외를 일관된 ErrorResponse 형식으로 반환
 * - LoggingFilter가 에러 정보를 DB에 저장할 수 있도록 request attribute에 기록
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 게시글 미존재 → 404 Not Found
     */
    @ExceptionHandler(BoardNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBoardNotFoundException(
            BoardNotFoundException e, HttpServletRequest request) {
        storeErrorInfo(request, e);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("BOARD_NOT_FOUND", e.getMessage(), LocalDateTime.now()));
    }

    /**
     * 입력값 검증 실패 → 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        storeErrorInfo(request, e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", message, LocalDateTime.now()));
    }

    /**
     * 중복 사용자명 → 409 Conflict
     */
    @ExceptionHandler(DuplicateUsernameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateUsernameException(
            DuplicateUsernameException e, HttpServletRequest request) {
        storeErrorInfo(request, e);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("DUPLICATE_USERNAME", e.getMessage(), LocalDateTime.now()));
    }

    /**
     * 인증 실패 (잘못된 자격증명) → 401 Unauthorized
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentialsException(
            InvalidCredentialsException e, HttpServletRequest request) {
        storeErrorInfo(request, e);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("INVALID_CREDENTIALS", e.getMessage(), LocalDateTime.now()));
    }

    /**
     * 게시글 접근 권한 없음 → 403 Forbidden
     */
    @ExceptionHandler(BoardAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleBoardAccessDeniedException(
            BoardAccessDeniedException e, HttpServletRequest request) {
        storeErrorInfo(request, e);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("ACCESS_DENIED", e.getMessage(), LocalDateTime.now()));
    }

    /**
     * 요청 로그 미존재 → 404 Not Found
     */
    @ExceptionHandler(RequestLogNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRequestLogNotFoundException(
            RequestLogNotFoundException e, HttpServletRequest request) {
        storeErrorInfo(request, e);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("LOG_NOT_FOUND", e.getMessage(), LocalDateTime.now()));
    }

    /**
     * 회원 미존재 → 404 Not Found
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(
            UserNotFoundException e, HttpServletRequest request) {
        storeErrorInfo(request, e);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("USER_NOT_FOUND", e.getMessage(), LocalDateTime.now()));
    }

    /**
     * 정지된 계정 로그인 시도 → 403 Forbidden
     */
    @ExceptionHandler(UserSuspendedException.class)
    public ResponseEntity<ErrorResponse> handleUserSuspendedException(
            UserSuspendedException e, HttpServletRequest request) {
        storeErrorInfo(request, e);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("USER_SUSPENDED", e.getMessage(), LocalDateTime.now()));
    }

    /**
     * 탈퇴한 계정 로그인 시도 → 403 Forbidden
     */
    @ExceptionHandler(UserWithdrawnException.class)
    public ResponseEntity<ErrorResponse> handleUserWithdrawnException(
            UserWithdrawnException e, HttpServletRequest request) {
        storeErrorInfo(request, e);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("USER_WITHDRAWN", e.getMessage(), LocalDateTime.now()));
    }

    /**
     * 그 외 예외 → 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        storeErrorInfo(request, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.", LocalDateTime.now()));
    }

    /**
     * 예외 정보를 request attribute에 저장
     * LoggingFilter의 finally 블록에서 이 attribute를 읽어 DB에 함께 저장
     *
     * @param request HTTP 요청 (attribute 저장 대상)
     * @param e       발생한 예외
     */
    private void storeErrorInfo(HttpServletRequest request, Exception e) {
        // 예외 메시지 저장
        request.setAttribute(LoggingFilter.ATTR_ERROR_MESSAGE, e.getMessage());

        // 스택 트레이스를 문자열로 변환 (DB 저장을 위해)
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String stackTrace = sw.toString();

        // 지나치게 긴 스택 트레이스는 잘라서 저장 (TEXT 컬럼 부하 방지)
        if (stackTrace.length() > 10000) {
            stackTrace = stackTrace.substring(0, 10000) + "\n...[truncated]";
        }
        request.setAttribute(LoggingFilter.ATTR_STACK_TRACE, stackTrace);
    }
}
