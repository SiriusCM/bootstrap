package com.sirius.bootstrap.module.battler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.StateMachinePersist;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.service.DefaultStateMachineService;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.DefaultStateMachineContext;

import java.util.EnumSet;

/**
 * 游戏状态机配置
 * 定义状态转换规则和监听器
 */
@Configuration
@EnableStateMachineFactory
public class BattleStateMachineConfig extends StateMachineConfigurerAdapter<BattleState, BattleEvent> {

    /**
     * 配置状态
     */
    @Override
    public void configure(StateMachineStateConfigurer<BattleState, BattleEvent> states) throws Exception {
        states.withStates()
                .initial(BattleState.IN_TOWN) // 初始状态：在城镇
                .states(EnumSet.allOf(BattleState.class));
    }

    /**
     * 配置状态转换规则
     */
    @Override
    public void configure(StateMachineTransitionConfigurer<BattleState, BattleEvent> transitions) throws Exception {
        transitions
                // 从城镇进入副本
                .withExternal()
                .source(BattleState.IN_TOWN).target(BattleState.IN_DUNGEON)
                .event(BattleEvent.ENTER_DUNGEON)
                .and()
                // 副本中遭遇敌人，进入战斗
                .withExternal()
                .source(BattleState.IN_DUNGEON).target(BattleState.IN_COMBAT)
                .event(BattleEvent.ENCOUNTER_ENEMY)
                .and()
                // 战斗中击败敌人，返回副本状态
                .withExternal()
                .source(BattleState.IN_COMBAT).target(BattleState.IN_DUNGEON)
                .event(BattleEvent.DEFEAT_ENEMY)
                .and()
                // 完成副本
                .withExternal()
                .source(BattleState.IN_DUNGEON).target(BattleState.DUNGEON_COMPLETED)
                .event(BattleEvent.COMPLETE_DUNGEON)
                .and()
                // 从副本完成状态返回城镇
                .withExternal()
                .source(BattleState.DUNGEON_COMPLETED).target(BattleState.IN_TOWN)
                .event(BattleEvent.RETURN_TO_TOWN)
                .and()
                // 副本失败返回城镇
                .withExternal()
                .source(BattleState.IN_DUNGEON).target(BattleState.IN_TOWN)
                .event(BattleEvent.FAIL_DUNGEON)
                .and()
                // 战斗中失败也返回城镇
                .withExternal()
                .source(BattleState.IN_COMBAT).target(BattleState.IN_TOWN)
                .event(BattleEvent.FAIL_DUNGEON);
    }

    /**
     * 配置状态机监听器
     * 监听状态转换事件并输出日志
     */
    @Override
    public void configure(StateMachineConfigurationConfigurer<BattleState, BattleEvent> config) throws Exception {
        config.withConfiguration()
                .listener(new StateMachineListenerAdapter<>() {
                    @Override
                    public void stateChanged(State<BattleState, BattleEvent> from, State<BattleState, BattleEvent> to) {
                        if (from != null) {
                            System.out.printf("状态转换: %s -> %s%n", from.getId(), to.getId());

                            // 根据状态转换输出相应的游戏提示
                            if (to.getId() == BattleState.IN_DUNGEON) {
                                System.out.println("=== 进入副本，准备开始冒险！ ===");
                            } else if (to.getId() == BattleState.IN_COMBAT) {
                                System.out.println("=== 遭遇敌人，进入战斗！ ===");
                            } else if (to.getId() == BattleState.DUNGEON_COMPLETED) {
                                System.out.println("=== 副本挑战成功！获得丰厚奖励！ ===");
                            } else if (to.getId() == BattleState.IN_TOWN && from.getId() != null) {
                                System.out.println("=== 回到安全的城镇，休整一下吧 ===");
                            }
                        }
                    }
                });
    }

    /**
     * 配置状态持久化
     * 实际游戏中可以保存到数据库，这里简单实现
     */
    @Bean
    public StateMachinePersist<BattleState, BattleEvent, String> stateMachinePersist() {
        return new StateMachinePersist<>() {
            @Override
            public void write(StateMachineContext<BattleState, BattleEvent> context, String playerId) {
                // 实际项目中可以将状态保存到数据库
                System.out.printf("保存玩家 %s 的状态: %s%n", playerId, context.getState());
            }

            @Override
            public StateMachineContext<BattleState, BattleEvent> read(String playerId) {
                // 实际项目中可以从数据库读取状态
                System.out.printf("加载玩家 %s 的状态%n", playerId);
                return new DefaultStateMachineContext<>(BattleState.IN_TOWN, null, null, null);
            }
        };
    }

    /**
     * 配置状态机服务
     * 用于管理多个玩家的状态机实例
     */
    @Bean
    public StateMachineService<BattleState, BattleEvent> stateMachineService(StateMachineFactory<BattleState, BattleEvent> factory, StateMachinePersist<BattleState, BattleEvent, String> persist) {
        return new DefaultStateMachineService<>(factory, persist);
    }
}
