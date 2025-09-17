package com.sirius.bootstrap.module.battler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.stereotype.Service;

/**
 * 游戏服务类
 * 处理玩家状态转换和事件
 */
@Service
public class BattleService {
    @Autowired
    private StateMachineService<BattleState, BattleEvent> stateMachineService;

    /**
     * 创建新玩家
     */
    public BattlePlayer createPlayer(String playerId) {
        StateMachine<BattleState, BattleEvent> stateMachine = stateMachineService.acquireStateMachine(playerId);
        stateMachine.startReactively().block();
        return new BattlePlayer(playerId, stateMachine);
    }

    /**
     * 释放状态机
     */
    public void releaseStateMachine(BattlePlayer battlePlayer) {
        StateMachine<BattleState, BattleEvent> battleMachine = battlePlayer.getBattleMachine();
        battleMachine.stopReactively().block();
        stateMachineService.releaseStateMachine(battleMachine.getId());
    }

    /**
     * 战斗状态机示例
     */
    @Bean
    public CommandLineRunner demo(BattleService battleService) {
        return args -> {
            System.out.println("=== 开始游戏状态机演示 ===");

            // 创建玩家
            BattlePlayer battlePlayer = battleService.createPlayer("Warrior");
            System.out.println("创建玩家: " + battlePlayer);

            // 玩家从城镇进入副本
            boolean enterSuccess = battlePlayer.handlePlayerEvent(BattleEvent.ENTER_DUNGEON);
            System.out.printf("进入副本: %s, 当前状态: %s%n",
                    enterSuccess ? "成功" : "失败", battlePlayer.getCurrentState());

            // 副本中遭遇敌人
            boolean combatSuccess = battlePlayer.handlePlayerEvent(BattleEvent.ENCOUNTER_ENEMY);
            System.out.printf("遭遇敌人: %s, 当前状态: %s%n",
                    combatSuccess ? "成功" : "失败", battlePlayer.getCurrentState());

            // 击败敌人
            boolean defeatSuccess = battlePlayer.handlePlayerEvent(BattleEvent.DEFEAT_ENEMY);
            System.out.printf("击败敌人: %s, 当前状态: %s%n",
                    defeatSuccess ? "成功" : "失败", battlePlayer.getCurrentState());

            // 完成副本
            boolean completeSuccess = battlePlayer.handlePlayerEvent(BattleEvent.COMPLETE_DUNGEON);
            System.out.printf("完成副本: %s, 当前状态: %s%n",
                    completeSuccess ? "成功" : "失败", battlePlayer.getCurrentState());

            // 返回城镇
            boolean returnSuccess = battlePlayer.handlePlayerEvent(BattleEvent.RETURN_TO_TOWN);
            System.out.printf("返回城镇: %s, 当前状态: %s%n",
                    returnSuccess ? "成功" : "失败", battlePlayer.getCurrentState());

            // 测试无效操作（在城镇中尝试击败敌人）
            boolean invalidSuccess = battlePlayer.handlePlayerEvent(BattleEvent.DEFEAT_ENEMY);
            System.out.printf("在城镇中尝试击败敌人: %s, 当前状态: %s%n",
                    invalidSuccess ? "成功" : "失败（符合预期）", battlePlayer.getCurrentState());

            System.out.println("=== 游戏状态机演示结束 ===");
        };
    }
}
