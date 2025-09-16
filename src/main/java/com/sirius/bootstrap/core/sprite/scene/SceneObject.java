package com.sirius.bootstrap.core.sprite.scene;

import com.sirius.bootstrap.core.sprite.SpriteObject;
import com.sirius.bootstrap.core.sprite.user.UserObject;
import com.sirius.bootstrap.core.tick.TickQueue;
import com.sirius.bootstrap.core.tick.TickThread;
import com.sirius.bootstrap.msg.Msg;
import jakarta.annotation.PostConstruct;
import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Scope("prototype")
@Component
public class SceneObject extends SpriteObject {
    @Autowired
    @Qualifier("sceneThread")
    private List<TickThread> sceneThreadList;
    //@Autowired
    protected List<ISceneModule> componentList = new ArrayList<>();

    protected final Map<String, UserObject> userObjectMap = new HashMap<>();

    @PostConstruct
    public void postConstruct() {
        int index = (int) (sceneThreadList.size() * Math.random());
        TickThread thread = sceneThreadList.get(index);
        bindThread(thread);
    }

    @Override
    public void start() {
        super.start();
        componentList.forEach(ISceneModule::start);
    }

    @TickQueue
    public void register(UserObject userObject) {
        userObjectMap.put(userObject.getId(), userObject);
    }

    @TickQueue
    public void unRegister(String id) {
        userObjectMap.remove(id);
    }

    @TickQueue
    public void broadcastMsg(Msg.Message message) {
        userObjectMap.values().forEach(onlineObject -> onlineObject.replyMsg(message));
    }

    @Override
    public void tick() {
        super.tick();
        componentList.forEach(ISceneModule::tick);
        for (UserObject userObject : userObjectMap.values()) {
            try {
                userObject.tick();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
