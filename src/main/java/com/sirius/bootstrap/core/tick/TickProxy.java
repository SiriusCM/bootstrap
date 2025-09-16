package com.sirius.bootstrap.core.tick;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


@Aspect
@Scope("prototype")
@Component
public class TickProxy {

    private TickObject tickObject;

    @Around("execution(* com.sirius.bootstrap.core.tick.TickObject.start(..))")
    public void start(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        tickObject = (TickObject) proceedingJoinPoint.getTarget();
        proceedingJoinPoint.proceed();
    }

    @Around("@annotation(com.sirius.bootstrap.core.tick.TickQueue)")
    public void tickQueue(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        if (tickObject == null) {
            proceedingJoinPoint.proceed();
        } else {
            tickObject.offerQueue(s -> {
                try {
                    proceedingJoinPoint.proceed();
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
