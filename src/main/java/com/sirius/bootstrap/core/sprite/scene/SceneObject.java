package com.sirius.bootstrap.core.sprite.scene;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sirius.bootstrap.core.frame.FrameThread;
import com.sirius.bootstrap.core.sprite.SpriteObject;
import com.sirius.bootstrap.core.sprite.user.UserObject;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@Scope("prototype")
@Component
public class SceneObject extends SpriteObject {
    @Autowired
    @Qualifier("sceneThread")
    private List<FrameThread> sceneThreadList;
    @Autowired
    protected List<ISceneCompoent> componentList;

    protected final Map<String, UserObject> userObjectMap = new HashMap<>();

    @PostConstruct
    public void postConstruct() {
        int index = (int) (sceneThreadList.size() * Math.random());
        FrameThread thread = sceneThreadList.get(index);
        bindThread(thread);
    }

    public void register(UserObject userObject) {
        userObjectMap.put(userObject.getId(), userObject);
    }

    public void unRegister(String id) {
        userObjectMap.remove(id);
    }

    public void broadcastMsg(Object object) {
        try {
            String str = objectMapper.writeValueAsString(object);
            userObjectMap.values().forEach(onlineObject -> {
                onlineObject.replyMsg(str);
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void init() {
        super.init();
        componentList.forEach(ISceneCompoent::init);
    }

    @Override
    public void nextFrame() {
        super.nextFrame();
        componentList.forEach(ISceneCompoent::nextFrame);
        for (UserObject userObject : userObjectMap.values()) {
            try {
                userObject.nextFrame();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
