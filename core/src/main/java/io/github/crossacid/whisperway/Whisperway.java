package io.github.crossacid.whisperway;

import io.github.crossacid.whisperway.data.ModAttachments;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Whisperway.MOD_ID)
public final class Whisperway {
    public static final String MOD_ID = "whisperway";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Whisperway(IEventBus modEventBus, ModContainer modContainer) {
        ModAttachments.register(modEventBus);
        LOGGER.info("Whisperway initialized");
    }
}
