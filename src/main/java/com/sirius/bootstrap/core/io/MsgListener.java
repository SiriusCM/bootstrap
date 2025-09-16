package com.sirius.bootstrap.core.io;


import com.sirius.bootstrap.msg.Msg;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MsgListener {

    Msg.Message.MsgIdCase msgIdCase();
}
