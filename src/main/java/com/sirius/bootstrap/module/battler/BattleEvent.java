package com.sirius.bootstrap.module.battler;

/**
 * 游戏事件枚举
 * 定义触发状态转换的事件
 */
public enum BattleEvent {
    // 进入副本
    ENTER_DUNGEON,
    // 遭遇敌人（进入战斗）
    ENCOUNTER_ENEMY,
    // 击败敌人（离开战斗）
    DEFEAT_ENEMY,
    // 完成副本
    COMPLETE_DUNGEON,
    // 返回城镇
    RETURN_TO_TOWN,
    // 副本失败
    FAIL_DUNGEON
}
