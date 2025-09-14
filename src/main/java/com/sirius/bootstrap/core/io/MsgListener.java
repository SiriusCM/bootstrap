package com.sirius.bootstrap.core.io;


import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MsgListener {
    String type();
}
