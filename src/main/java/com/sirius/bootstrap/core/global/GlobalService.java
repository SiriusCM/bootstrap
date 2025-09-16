package com.sirius.bootstrap.core.global;


import com.sirius.bootstrap.core.io.MsgHandler;
import com.sirius.bootstrap.core.sprite.scene.SceneObject;
import com.sirius.bootstrap.core.sprite.user.UserObject;
import com.sirius.bootstrap.core.tick.TickObject;
import com.sirius.bootstrap.core.tick.TickThread;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class GlobalService extends TickObject {
    @Autowired
    private ConfigurableApplicationContext applicationContext;
    @Autowired
    @Qualifier("globalThread")
    protected TickThread globalThread;
    @Autowired
    protected Map<String, UserObject> userObjectMap;

    protected Queue<MsgHandler> loginQueue = new LinkedBlockingQueue<>();

    private SceneObject battleSceneObject;

    @PostConstruct
    public void postConstruct() {
        bindThread(globalThread);
        battleSceneObject = applicationContext.getBean(SceneObject.class);
        battleSceneObject.start();
    }

    @Override
    public void tick() {
        super.tick();
        loginHandler();
        logoutHandler();
    }

    public void loginQueue(MsgHandler msgHandler) {
        loginQueue.offer(msgHandler);
    }

    public void loginHandler() {
        while (!loginQueue.isEmpty()) {
            MsgHandler msgHandler = loginQueue.poll();
            UserObject userObject = applicationContext.getBean(UserObject.class);
            msgHandler.setUserObject(userObject);
            userObject.login(UUID.randomUUID().toString(), msgHandler.getCtx());
            userObject.start();

            userObject.enterScene(battleSceneObject);
        }
    }

    public void logoutHandler() {
        long keepLiveTime = System.currentTimeMillis() - 60 * 1000;
        List<UserObject> list = userObjectMap.values().stream().filter(
                userObject -> userObject.isOffline(keepLiveTime)).toList();
        for (UserObject userObject : list) {
            userObject.leaveScene();
            String userId = userObject.getId();
            userObjectMap.remove(userId);
        }
    }
}
