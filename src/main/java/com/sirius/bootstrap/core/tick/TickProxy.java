package com.sirius.bootstrap.core.tick;

import com.sirius.bootstrap.core.sprite.scene.SceneObject;
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

    @Around("execution(* com.jdt.game.core.tick.TickObject.bindThread(..))")
    public void bindThread(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        tickObject = (TickObject) proceedingJoinPoint.getTarget();
    }

    @Around("execution(* com.jdt.game.core.sprite.user.UserObject.enterScene(..))")
    public void enterScene(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        proceedingJoinPoint.proceed();
        tickObject = (SceneObject) proceedingJoinPoint.getArgs()[0];
    }

    @Around("@annotation(com.jdt.game.core.tick.TickQueue)")
    public void frameQueue(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
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
