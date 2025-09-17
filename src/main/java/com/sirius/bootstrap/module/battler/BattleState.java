package com.sirius.bootstrap.module.battler;

/**
 * 游戏状态枚举
 * 定义玩家可能处于的状态
 */
public enum BattleState {
    // 城镇状态
    IN_TOWN,
    // 副本中状态
    IN_DUNGEON,
    // 战斗中状态（副本内的子状态）
    IN_COMBAT,
    // 副本完成状态
    DUNGEON_COMPLETED
}
