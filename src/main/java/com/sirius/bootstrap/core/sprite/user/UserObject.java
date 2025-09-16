package com.sirius.bootstrap.core.sprite.user;

import com.sirius.bootstrap.core.io.MsgListener;
import com.sirius.bootstrap.core.persist.PersistObject;
import com.sirius.bootstrap.core.rpc.RpcObject;
import com.sirius.bootstrap.core.sprite.SpriteObject;
import com.sirius.bootstrap.core.sprite.scene.SceneObject;
import com.sirius.bootstrap.msg.Msg;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
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
public class UserObject extends SpriteObject {
    @Autowired
    protected List<IUserModule> componentList;
    @Autowired
    protected RpcObject rpcObject;
    @Autowired
    protected PersistObject persistObject;

    protected SceneObject sceneObject;

    protected Map<Msg.Message.MsgIdCase, List<Consumer<Object>>> msgConsumerMap = new HashMap<>();

    protected ChannelHandlerContext channelHandlerContext;

    protected Channel channel;

    protected long logoutTime;

    @Override
    public void start() {
        super.start();
        rpcObject.start();
        persistObject.start();
        poolMap.forEach((aClass, roleBean) -> {
            try {
                for (Method method : aClass.getDeclaredMethods()) {
                    method.setAccessible(true);
                    if (method.isAnnotationPresent(MsgListener.class)) {
                        MsgListener listener = method.getAnnotation(MsgListener.class);
                        msgConsumerMap.compute(listener.msgIdCase(), (id, list) -> {
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

    public void login(String id, ChannelHandlerContext ctx) {
        this.id = id;
        this.channelHandlerContext = ctx;
        this.channel = ctx.channel();

        poolMap.put(UserObject.class, this);
        poolMap.put(ChannelHandlerContext.class, ctx);
        componentList.forEach(component -> poolMap.put(component.getClass(), component));
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

    public void dispatchMsg(Msg.Message message) {
        if (msgConsumerMap.get(message.getMsgIdCase()) == null) {
            return;
        }
        msgConsumerMap.get(message.getMsgIdCase()).forEach(consumer -> consumer.accept(message));
    }

    public void replyMsg(Msg.Message message) {
        channelHandlerContext.writeAndFlush(message);
    }

    public void enterScene(SceneObject sceneObject) {
        if (sceneObject == null) {
            return;
        }
        this.sceneObject = sceneObject;
        sceneObject.register(this);
    }

    public void leaveScene() {
        if (sceneObject == null) {
            return;
        }
        sceneObject.unRegister(id);
    }

    @Override
    public void tick() {
        super.tick();
        for (IUserModule component : componentList) {
            component.tick();
        }
    }
}
