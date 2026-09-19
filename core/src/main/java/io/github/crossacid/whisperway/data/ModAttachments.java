package io.github.crossacid.whisperway.data;

import io.github.crossacid.whisperway.Whisperway;
import io.github.crossacid.whisperway.pathway.ironguard.IronGuardCombatState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * 注册秘闻之径使用的玩家附加数据。
 */
public final class ModAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(
            NeoForgeRegistries.ATTACHMENT_TYPES,
            Whisperway.MOD_ID);

    /**
     * 玩家永久保存的途径进度。
     *
     * serialize：使用 PathwayProgress.CODEC 保存到玩家存档。
     * copyOnDeath：玩家死亡后保留途径、序列和消化度。
     */
    public static final Supplier<AttachmentType<PathwayProgress>> PATHWAY_PROGRESS = ATTACHMENT_TYPES.register(
            "pathway_progress",
            () -> AttachmentType
                    .builder(() -> PathwayProgress.EMPTY)
                    .serialize(PathwayProgress.CODEC)
                    .copyOnDeath()
                    .build());

    /**
     * 铁卫当前战斗中的临时状态。
     *
     * 没有调用 serialize，因此不会写入玩家存档。
     * 没有调用 copyOnDeath，因此玩家死亡后不会保留战势。
     */
    public static final Supplier<AttachmentType<IronGuardCombatState>> IRON_GUARD_COMBAT_STATE = ATTACHMENT_TYPES
            .register(
                    "iron_guard_combat_state",
                    () -> AttachmentType
                            .builder(() -> IronGuardCombatState.EMPTY)
                            .build());

    private ModAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
