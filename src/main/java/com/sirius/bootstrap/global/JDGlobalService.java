package com.sirius.bootstrap.global;

import com.fasterxml.jackson.databind.JsonNode;
import com.sirius.bootstrap.core.global.GlobalService;
import com.sirius.bootstrap.core.io.MsgHandler;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.stereotype.Service;

@Service
public class JDGlobalService extends GlobalService {
    @Override
    public void loginQueue(MsgHandler msgHandler, ChannelHandlerContext channelHandlerContext, JsonNode jsonNode) {

    }

    @Override
    public void loginHandler() {

    }
}
