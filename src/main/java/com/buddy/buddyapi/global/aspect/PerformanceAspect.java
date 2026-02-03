package com.buddy.buddyapi.global.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Slf4j
@Aspect
@Component
public class PerformanceAspect {

    @Around("@annotation(com.buddy.buddyapi.global.aspect.Timer)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // 실제 타겟 메서드 실행
        Object proceed = joinPoint.proceed();

        stopWatch.stop();

        log.info("[Performance] Method: {} | Execution Time: {} ms",
                joinPoint.getSignature().toShortString(),
                stopWatch.getTotalTimeMillis());

        return proceed;

    }
}
