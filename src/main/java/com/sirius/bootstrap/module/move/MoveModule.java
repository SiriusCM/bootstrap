package com.sirius.bootstrap.module.move;

import com.sirius.bootstrap.core.io.MsgListener;
import com.sirius.bootstrap.core.ioc.AutoBean;
import com.sirius.bootstrap.core.sprite.user.IUserModule;
import com.sirius.bootstrap.core.sprite.user.UserObject;
import com.sirius.bootstrap.msg.Msg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Slf4j
@Scope("prototype")
@Component
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
        builder.setPlayerId(moveMessage.getPlayerId() + "1");
        builder.setPosX(moveMessage.getPosX() + 3);
        builder.setPosY(moveMessage.getPosY());
        builder.setPosZ(moveMessage.getPosZ());
        builder.setRotX(moveMessage.getRotX());
        builder.setRotY(moveMessage.getRotY());
        builder.setRotZ(moveMessage.getRotZ());

        Msg.Message.Builder resMsg = Msg.Message.newBuilder();
        resMsg.setMoveMessage(builder);
        userObject.replyMsg(resMsg.build());
        log.info(resMsg.toString());
    }
}
