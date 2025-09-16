package com.sirius.bootstrap.core.io;

import com.sirius.bootstrap.core.global.GlobalService;
import com.sirius.bootstrap.core.sprite.user.UserObject;
import com.sirius.bootstrap.msg.Msg;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Slf4j
@EqualsAndHashCode(callSuper = true)
@Data
@Scope("prototype")
@Component
public class MsgHandler extends SimpleChannelInboundHandler<Msg.Message> {
    @Autowired
    private GlobalService globalService;

    private ChannelHandlerContext ctx;

    private UserObject userObject;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        globalService.loginQueue(this);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Msg.Message message) throws Exception {
        userObject.dispatchMsg(message);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (userObject == null) {
            return;
        }
        userObject.logout();
    }
}
