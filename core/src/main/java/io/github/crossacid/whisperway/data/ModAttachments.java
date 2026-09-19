package io.github.crossacid.whisperway.data;

import io.github.crossacid.whisperway.Whisperway;
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
     * 
     * copyOnDeath：玩家死亡后保留途径、序列和消化度。
     */
    public static final Supplier<AttachmentType<PathwayProgress>> PATHWAY_PROGRESS = ATTACHMENT_TYPES.register(
            "pathway_progress",
            () -> AttachmentType
                    .builder(() -> PathwayProgress.EMPTY)
                    .serialize(PathwayProgress.CODEC)
                    .copyOnDeath()
                    .build());

    private ModAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
