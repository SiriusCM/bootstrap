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
        Msg.MoveMessage moveMessage = reqMsg.getMoveMessage();

        Msg.MoveMessage.Builder builder = Msg.MoveMessage.newBuilder();
        builder.setPlayerId(moveMessage.getPlayerId());
        builder.setPosX(moveMessage.getPosX());
        builder.setPosY(moveMessage.getPosY());
        builder.setPosZ(moveMessage.getPosZ());
        builder.setRotX(moveMessage.getRotX());
        builder.setRotY(moveMessage.getRotY());
        builder.setRotZ(moveMessage.getRotZ());

        Msg.Message.Builder resMsg = Msg.Message.newBuilder();
        resMsg.setMoveMessage(builder);
        userObject.replyMsg(resMsg.build());
        log.info(resMsg.toString());

        SceneObject sceneObject = userObject.getSceneObject();
        sceneObject.broadcastMsg(resMsg.build());
    }
}
