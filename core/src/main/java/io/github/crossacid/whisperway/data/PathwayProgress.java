package io.github.crossacid.whisperway.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import io.github.crossacid.whisperway.Whisperway;

/**
 * 玩家当前进度
 * 
 * @param pathwayId: 途径ID
 * @param sequence:  当前序列, -1为普通人
 * @param digestion: 消化度, 0-10000
 */
public record PathwayProgress(ResourceLocation pathwayId, int sequence, int digestion) {
    public static final int NO_SEQUENCE = -1;
    public static final int MAX_DIGESTION = 10_000;

    public static final ResourceLocation NONE = ResourceLocation.fromNamespaceAndPath(Whisperway.MOD_ID, "none");
    public static final ResourceLocation IRON_GUARD = ResourceLocation.fromNamespaceAndPath(Whisperway.MOD_ID,
            "iron_guard");

    public static final PathwayProgress EMPTY = new PathwayProgress(NONE, NO_SEQUENCE, 0);

    /**
     * 注册Data Attachment的Codec, 用于将途径数据保存到玩家存档中
     */
    public static final Codec<PathwayProgress> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(ResourceLocation.CODEC.optionalFieldOf("pathway_id", NONE).forGetter(PathwayProgress::pathwayId),
                    Codec.INT.optionalFieldOf("sequence", NO_SEQUENCE).forGetter(PathwayProgress::sequence),
                    Codec.intRange(0, MAX_DIGESTION).optionalFieldOf("digestion", 0)
                            .forGetter(PathwayProgress::digestion))
            .apply(instance, PathwayProgress::new));

    /**
     * 读取旧存档数值，限制其在合法范围内
     */
    public PathwayProgress {
        if (pathwayId == null) {
            pathwayId = NONE;
        }
        digestion = Mth.clamp(digestion, 0, MAX_DIGESTION);
    }

    public boolean isOrdinary() {
        return pathwayId.equals(NONE) || sequence == NO_SEQUENCE;
    }

    public boolean isIronGuardSequence9() {
        return pathwayId.equals(IRON_GUARD) && sequence == 9;
    }

    public PathwayProgress withDigestion(int newDigestion) {
        return new PathwayProgress(
                pathwayId,
                sequence,
                newDigestion);
    }

    public PathwayProgress asIronGuardSequence9() {
        return new PathwayProgress(
                IRON_GUARD,
                9,
                0);
    }

    public PathwayProgress clear() {
        return EMPTY;
    }

    /**
     * 返回当前已经达到的消化里程碑。
     */
    public int digestionMilestone() {
        if (digestion >= 10_000) {
            return 100;
        }

        if (digestion >= 7_500) {
            return 75;
        }

        if (digestion >= 5_000) {
            return 50;
        }

        if (digestion >= 2_500) {
            return 25;
        }

        return 0;
    }
}
