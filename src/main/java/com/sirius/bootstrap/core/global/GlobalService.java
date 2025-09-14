package com.sirius.bootstrap.core.global;


import com.fasterxml.jackson.databind.JsonNode;
import com.sirius.bootstrap.core.frame.Frame;
import com.sirius.bootstrap.core.frame.FrameThread;
import com.sirius.bootstrap.core.io.MsgHandler;
import com.sirius.bootstrap.core.sprite.user.UserObject;
import io.netty.channel.ChannelHandlerContext;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

public abstract class GlobalService extends Frame {
    @Autowired
    @Qualifier("globalThread")
    protected FrameThread globalThread;
    @Autowired
    protected Map<String, UserObject> userObjectMap;

    protected Queue<LoginInfo> loginQueue = new LinkedList<>();

    @PostConstruct
    public void postConstruct() {
        bindThread(globalThread);
    }

    @Override
    public void nextFrame() {
        loginHandler();
        logoutHandler();
    }

    public abstract void loginQueue(MsgHandler msgHandler, ChannelHandlerContext ctx, JsonNode jsonNode);

    public abstract void loginHandler();

    public void logoutHandler() {
        long keepLiveTime = System.currentTimeMillis() - 60 * 1000;
        List<UserObject> list = userObjectMap.values().stream().filter(
                userObject -> userObject.isOffline(keepLiveTime)).collect(Collectors.toList());
        for (UserObject userObject : list) {
            userObject.leaveScene();
            String userId = userObject.getId();
            userObjectMap.remove(userId);
        }
    }
}
