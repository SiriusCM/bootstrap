package com.sirius.bootstrap.core.global;

import com.sirius.bootstrap.core.io.MsgHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.Data;

@Data
public class LoginInfo {

    private MsgHandler msgHandler;

    private ChannelHandlerContext ctx;
}
