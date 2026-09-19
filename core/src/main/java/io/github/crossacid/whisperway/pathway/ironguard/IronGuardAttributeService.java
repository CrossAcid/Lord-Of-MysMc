package io.github.crossacid.whisperway.pathway.ironguard;

import io.github.crossacid.whisperway.Whisperway;
import io.github.crossacid.whisperway.data.ModAttachments;
import io.github.crossacid.whisperway.data.PathwayProgress;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 把铁卫阶段数值转换成玩家身上的原版属性修饰器。
 */
@EventBusSubscriber(modid = Whisperway.MOD_ID)
public final class IronGuardAttributeService {
    /*
     * 每种属性使用固定 ID。
     *
     * 刷新属性时，可以通过这些 ID 找到并替换旧修饰器，
     * 避免重复添加同一种属性。
     */
    private static final ResourceLocation MAX_HEALTH_ID = id("iron_guard_max_health");

    private static final ResourceLocation ATTACK_DAMAGE_ID = id("iron_guard_attack_damage");

    private static final ResourceLocation ARMOR_TOUGHNESS_ID = id("iron_guard_armor_toughness");

    private static final ResourceLocation KNOCKBACK_RESISTANCE_ID = id("iron_guard_knockback_resistance");

    private static final ResourceLocation MOVEMENT_EFFICIENCY_ID = id("iron_guard_movement_efficiency");

    private IronGuardAttributeService() {
    }

    /**
     * 根据玩家当前途径和消化度，重新计算全部铁卫属性。
     */
    public static void refresh(ServerPlayer player) {
        /*
         * 先清除旧修饰器。
         * 这样从 25% 升到 50% 时，不会把两个阶段的数值叠加起来。
         * 玩家清除途径时，也能恢复原版属性。
         */
        removeAllIronGuardModifiers(player);
        PathwayProgress progress = player.getData(ModAttachments.PATHWAY_PROGRESS);
        if (!progress.isIronGuardSequence9()) {
            clampCurrentHealth(player);
            return;
        }
        IronGuardStage stage = IronGuardStage.fromProgress(progress);
        addModifier(player, Attributes.MAX_HEALTH, MAX_HEALTH_ID, stage.maxHealthBonus());
        addModifier(player, Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE_ID, stage.attackDamageBonus());
        addModifier(player, Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS_ID, stage.armorToughnessBonus());
        addModifier(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE_ID, stage.knockbackResistanceBonus());
        addModifier(player, Attributes.MOVEMENT_EFFICIENCY, MOVEMENT_EFFICIENCY_ID, stage.movementEfficiencyBonus());

        clampCurrentHealth(player);
    }

    /**
     * 给玩家的一项原版属性添加临时修饰器。
     * 临时修饰器不会重复写入原版属性存档。
     * 玩家进入世界或重生时，我们会根据途径数据重新生成。
     */
    private static void addModifier(
            ServerPlayer player,
            Holder<Attribute> attribute,
            ResourceLocation modifierId,
            double amount) {
        AttributeInstance instance = player.getAttribute(attribute);

        if (instance == null) {
            Whisperway.LOGGER.warn("无法为玩家 {} 添加属性修饰器 {}", player.getGameProfile().getName(), modifierId);
            return;
        }

        if (amount == 0.0) {
            return;
        }

        instance.addOrUpdateTransientModifier(
                new AttributeModifier(modifierId, amount, AttributeModifier.Operation.ADD_VALUE));
    }

    /**
     * 移除玩家身上所有由序列 9 铁卫添加的属性。
     */
    private static void removeAllIronGuardModifiers(ServerPlayer player) {
        removeModifier(player, Attributes.MAX_HEALTH, MAX_HEALTH_ID);
        removeModifier(player, Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE_ID);
        removeModifier(player, Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS_ID);
        removeModifier(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE_ID);
        removeModifier(player, Attributes.MOVEMENT_EFFICIENCY, MOVEMENT_EFFICIENCY_ID);
    }

    /**
     * 按固定 ID 移除某一项属性修饰器。
     */
    private static void removeModifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation modifierId) {
        AttributeInstance instance = player.getAttribute(attribute);

        if (instance != null) {
            instance.removeModifier(modifierId);
        }
    }

    /**
     * 防止当前生命超过最大生命。
     */
    private static void clampCurrentHealth(ServerPlayer player) {
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /**
     * 玩家进入世界时，重新应用属性。
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            refresh(player);
        }
    }

    /**
     * 玩家死亡重生时，新的玩家实体需要重新应用属性。
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            refresh(player);
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Whisperway.MOD_ID, path);
    }
}
