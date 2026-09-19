package io.github.crossacid.whisperway.pathway.ironguard;

import io.github.crossacid.whisperway.data.PathwayProgress;

/**
 * 序列 9 铁卫在某个消化阶段拥有的完整数值。
 *
 * 这个 record 是一张只读数值表，不负责保存玩家数据，也不直接修改玩家属性。
 */
public record IronGuardStage(
        int milestone, // 消化度里程碑
        double maxHealthBonus, // 增加的最大生命值
        double attackDamageBonus, // 增加的基础近战攻击伤害，会叠加到当前武器伤害上
        double armorToughnessBonus, // 增加的护甲韧性，用于减轻高伤害攻击对护甲减伤的穿透
        double knockbackResistanceBonus, // 增加的击退抗性，取值 0～1；0.10 表示增加 10% 击退抗性。
        /**
         * 增加的移动效率，减轻灵魂沙、蜂蜜块、蛛网等方块造成的减速；
         * 不会提高玩家在普通地面上的移动速度。
         */
        double movementEfficiencyBonus,
        float heavySlashMultiplier, // 反击重斩对主目标的伤害倍率，例如 1.50 表示原伤害的 150%
        int secondaryTargets, // 反击重斩最多能够伤害的额外目标数量，不包含主目标
        float secondaryDamageMultiplier, // 反击重斩对额外目标的伤害倍率，例如0.50表示原伤害的 50%。
        boolean canBreakImpactBlocks // 当前阶段的重斩是否可以破坏 impact_breakable 标签中的脆弱方块
) {
        /**
         * 刚服下魔药：0% 消化度。
         */
        public static final IronGuardStage FRESH = new IronGuardStage(
                        0,
                        1.0,
                        0.25,
                        0.0,
                        0.02,
                        0.04,
                        1.25F,
                        0,
                        0.0F,
                        false);

        /**
         * 25% 消化度。
         */
        public static final IronGuardStage QUARTER = new IronGuardStage(
                        25,
                        2.0,
                        0.50,
                        0.25,
                        0.04,
                        0.08,
                        1.30F,
                        1,
                        0.35F,
                        false);

        /**
         * 50% 消化度。
         */
        public static final IronGuardStage HALF = new IronGuardStage(
                        50,
                        3.0,
                        0.75,
                        0.50,
                        0.06,
                        0.12,
                        1.35F,
                        2,
                        0.40F,
                        false);

        /**
         * 75% 消化度。
         */
        public static final IronGuardStage THREE_QUARTERS = new IronGuardStage(
                        75,
                        3.0,
                        1.00,
                        0.75,
                        0.08,
                        0.16,
                        1.40F,
                        3,
                        0.45F,
                        false);

        /**
         * 100% 消化度。
         */
        public static final IronGuardStage COMPLETE = new IronGuardStage(
                        100,
                        4.0,
                        1.00,
                        1.00,
                        0.10,
                        0.20,
                        1.50F,
                        3,
                        0.50F,
                        true);

        /**
         * 根据玩家的消化里程碑选择对应数值。
         *
         * 调用这个方法前，玩家必须已经是序列 9 铁卫。
         */
        public static IronGuardStage fromProgress(
                        PathwayProgress progress) {
                if (!progress.isIronGuardSequence9()) {
                        throw new IllegalArgumentException(
                                        "只有序列 9 铁卫才能取得铁卫阶段数值");
                }

                return switch (progress.digestionMilestone()) {
                        case 100 -> COMPLETE;
                        case 75 -> THREE_QUARTERS;
                        case 50 -> HALF;
                        case 25 -> QUARTER;
                        default -> FRESH;
                };
        }

}
