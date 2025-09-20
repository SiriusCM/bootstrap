package com.sirius.bootstrap.module.move;

import com.sirius.bootstrap.core.io.MsgListener;
import com.sirius.bootstrap.core.ioc.AutoBean;
import com.sirius.bootstrap.core.ioc.Module;
import com.sirius.bootstrap.core.sprite.scene.SceneObject;
import com.sirius.bootstrap.core.sprite.user.IUserModule;
import com.sirius.bootstrap.core.sprite.user.UserObject;
import com.sirius.bootstrap.msg.Msg;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Module
public class MoveModule implements IUserModule {
    @AutoBean
    private UserObject userObject;

    @Override
    public void start() {

    }

    @Override
    public void tick() {

    }

    @MsgListener(msgIdCase = Msg.Message.MsgIdCase.MOVEMESSAGE)
    public void move(Msg.Message reqMsg) {
        log.info(reqMsg.toString());
        SceneObject sceneObject = userObject.getSceneObject();
        sceneObject.broadcastMsg(reqMsg);
    }
}
