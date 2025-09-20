package com.sirius.bootstrap.module.battler;

import lombok.Getter;
import lombok.ToString;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineEventResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 玩家类
 * 持有玩家ID和当前状态
 */
@Getter
@ToString
public class BattlePlayer {

    private final String playerId;

    private final transient StateMachine<BattleState, BattleEvent> battleMachine;

    public BattlePlayer(String playerId, StateMachine<BattleState, BattleEvent> battleMachine) {
        this.playerId = playerId;
        this.battleMachine = battleMachine;
    }

    /**
     * 处理玩家事件，触发状态转换
     */
    public boolean handlePlayerEvent(BattleEvent event) {
        // 创建并发送事件消息
        Message<BattleEvent> message = MessageBuilder.withPayload(event).build();
        Flux<StateMachineEventResult<BattleState, BattleEvent>> results = battleMachine.sendEvent(Mono.just(message));

        // 获取事件处理结果
        StateMachineEventResult<BattleState, BattleEvent> result = results.blockLast();

        // 判断事件是否被接受处理
        return result != null &&
                StateMachineEventResult.ResultType.ACCEPTED.equals(result.getResultType());
    }

    public BattleState getCurrentState() {
        return battleMachine.getState().getId();
    }
}
