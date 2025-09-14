package com.sirius.bootstrap.core.frame;

import com.sirius.bootstrap.core.sprite.scene.SceneObject;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


@Aspect
@Scope("prototype")
@Component
public class FrameProxy {

    private Frame frame;

    @Around("execution(* com.jdt.game.core.frame.Frame.bindThread(..))")
    public void bindThread(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        frame = (Frame) proceedingJoinPoint.getTarget();
    }

    @Around("execution(* com.jdt.game.core.sprite.user.UserObject.enterScene(..))")
    public void enterScene(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        proceedingJoinPoint.proceed();
        frame = (SceneObject) proceedingJoinPoint.getArgs()[0];
    }

    @Around("@annotation(com.jdt.game.core.frame.FrameQueue)")
    public void frameQueue(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        if (frame == null) {
            proceedingJoinPoint.proceed();
        } else {
            frame.offerQueue(s -> {
                try {
                    proceedingJoinPoint.proceed();
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
