package com.dong.board.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 요청 흐름 추적 AOP
 * Controller → Service → Repository 레이어를 자동으로 추적하여 로그에 출력합니다.
 *
 * IntelliJ IDEA 콘솔에서 클래스명(예: BoardController)을 클릭하면 해당 파일로 이동합니다.
 * 각 메서드 호출마다 Micrometer Tracing의 Span을 생성하여 Zipkin에 전송합니다.
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class RequestFlowLoggingAspect {

    private final Tracer tracer;

    // ────────────────────────────────────────────
    // Controller 레이어
    // ────────────────────────────────────────────
    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object traceController(ProceedingJoinPoint pjp) throws Throwable {
        return traceLayer(pjp, "CONTROLLER");
    }

    // ────────────────────────────────────────────
    // Service 레이어
    // ────────────────────────────────────────────
    @Around("within(@org.springframework.stereotype.Service *)")
    public Object traceService(ProceedingJoinPoint pjp) throws Throwable {
        return traceLayer(pjp, "SERVICE");
    }

    // ────────────────────────────────────────────
    // Repository 레이어 (인터페이스 구현체 포함)
    // ────────────────────────────────────────────
    @Around(
        "within(@org.springframework.stereotype.Repository *) || " +
        "within(org.springframework.data.repository.Repository+)"
    )
    public Object traceRepository(ProceedingJoinPoint pjp) throws Throwable {
        return traceLayer(pjp, "REPOSITORY");
    }

    // ────────────────────────────────────────────
    // 공통 추적 로직
    // ────────────────────────────────────────────
    private Object traceLayer(ProceedingJoinPoint pjp, String layer) throws Throwable {
        String className  = pjp.getTarget().getClass().getSimpleName();
        String methodName = pjp.getSignature().getName();
        String spanName   = layer.toLowerCase() + "." + className + "." + methodName;

        // 현재 활성 Span에 새 child Span 시작
        Span span = tracer.nextSpan().name(spanName).start();

        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            // ★ IntelliJ에서 클릭하면 해당 클래스로 이동하는 FQCN 링크 포함 ★
            String fqcn = pjp.getTarget().getClass().getName();
            log.debug("[{}] ▶ {}.{}()  (at {})",
                layer, className, methodName, fqcn);

            long start = System.currentTimeMillis();
            try {
                Object result = pjp.proceed();
                long elapsed = System.currentTimeMillis() - start;
                log.debug("[{}] ◀ {}.{}()  {}ms",
                    layer, className, methodName, elapsed);
                return result;
            } catch (Throwable t) {
                span.tag("error", t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName());
                log.warn("[{}] ✗ {}.{}()  error={}",
                    layer, className, methodName, t.getMessage());
                throw t;
            }
        } finally {
            span.end();
        }
    }
}

