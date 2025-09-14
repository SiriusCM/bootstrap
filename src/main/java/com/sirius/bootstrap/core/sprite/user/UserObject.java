package com.sirius.bootstrap.core.sprite.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sirius.bootstrap.core.io.MsgListener;
import com.sirius.bootstrap.core.persist.PersistObject;
import com.sirius.bootstrap.core.rpc.RpcObject;
import com.sirius.bootstrap.core.sprite.SpriteObject;
import com.sirius.bootstrap.core.sprite.scene.SceneObject;
import com.sirius.bootstrap.msg.Msg;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EqualsAndHashCode(callSuper = true)
@Data
@Scope("prototype")
@Component
public abstract class UserObject extends SpriteObject {
    @Autowired
    protected List<IUserComponent> componentList;
    @Autowired
    protected RpcObject rpcObject;
    @Autowired
    protected PersistObject persistObject;

    protected Map<String, List<Consumer<Object>>> msgConsumerMap = new HashMap<>();

    protected SceneObject sceneObject;

    protected Channel channel;

    protected ChannelHandlerContext channelHandlerContext;

    protected long logoutTime;

    public void login(String id, Channel channel, ChannelHandlerContext ctx) {
        this.id = id;
        this.channel = channel;
        this.channelHandlerContext = ctx;

        poolMap.put(UserObject.class, this);
        poolMap.put(ChannelHandlerContext.class, ctx);
        componentList.forEach(component -> poolMap.put(component.getClass(), component));

        init();
    }

    public void logout() {
        logoutTime = System.currentTimeMillis();
    }

    public boolean isOffline(long keepLiveTime) {
        if (channel.isOpen()) {
            return false;
        }
        if (logoutTime > keepLiveTime) {
            return false;
        }
        if (rpcObject != null && !rpcObject.getConsumerQueue().isEmpty()) {
            return false;
        }
        return persistObject == null || persistObject.getConsumerQueue().isEmpty();
    }

    public void dispatchMsg(Msg.Message message) throws JsonProcessingException {
        //        Object msg = objectMapper.treeToValue(jsonNode, msgMap.get(type));
        //        msgConsumerMap.get(type).forEach(consumer -> consumer.accept(msg));
    }

    public void replyMsg(Object object) {
        try {
            String str = objectMapper.writeValueAsString(object);
            replyMsg(str);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void enterScene(SceneObject sceneObject) {
        if (sceneObject == null) {
            return;
        }
        this.sceneObject = sceneObject;
        this.sceneObject.getUserObjectMap().put(id, this);
    }

    public void leaveScene() {
        if (sceneObject == null) {
            return;
        }
        sceneObject.getUserObjectMap().remove(id);
    }

    @Override
    public void init() {
        super.init();
        poolMap.forEach((aClass, roleBean) -> {
            try {
                for (Method method : aClass.getDeclaredMethods()) {
                    method.setAccessible(true);
                    if (method.isAnnotationPresent(MsgListener.class)) {
                        MsgListener listener = method.getAnnotation(MsgListener.class);
                        msgConsumerMap.compute(listener.type(), (id, list) -> {
                            if (list == null) {
                                list = new ArrayList<>();
                            }
                            list.add(message -> {
                                try {
                                    method.invoke(roleBean, message);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            });
                            return list;
                        });
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void nextFrame() {
        super.nextFrame();
        for (IUserComponent component : componentList) {
            component.nextFrame();
        }
    }
}
