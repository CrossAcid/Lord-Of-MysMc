package io.github.crossacid.whisperway.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.Command;

import io.github.crossacid.whisperway.Whisperway;
import io.github.crossacid.whisperway.data.ModAttachments;
import io.github.crossacid.whisperway.data.PathwayProgress;
import io.github.crossacid.whisperway.pathway.ironguard.IronGuardAttributeService;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * 秘闻之径开发阶段使用的测试命令。
 */
@EventBusSubscriber(modid = Whisperway.MOD_ID)
public class WhisperwayCommands {
    private WhisperwayCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("whisperway")

                        // /whisperway pathway iron_guard
                        .then(Commands.literal("pathway")
                                .then(Commands.literal("iron_guard")
                                        .requires(source -> source.hasPermission(2))
                                        .executes(context -> setIronGuard(
                                                context.getSource().getPlayerOrException())))

                                // /whisperway pathway clear
                                .then(Commands.literal("clear")
                                        .requires(source -> source.hasPermission(2))
                                        .executes(context -> clearPathway(
                                                context.getSource().getPlayerOrException()))))

                        // /whisperway digestion set <value>
                        .then(Commands.literal("digestion")
                                .then(Commands.literal("set")
                                        .requires(source -> source.hasPermission(2))
                                        .then(Commands.argument(
                                                "value",
                                                IntegerArgumentType.integer(
                                                        0,
                                                        PathwayProgress.MAX_DIGESTION))
                                                .executes(context -> setDigestion(
                                                        context.getSource().getPlayerOrException(),
                                                        IntegerArgumentType.getInteger(
                                                                context,
                                                                "value"))))))

                        // /whisperway inspect
                        .then(Commands.literal("inspect")
                                .executes(context -> inspect(
                                        context.getSource().getPlayerOrException()))));
    }

    private static int setIronGuard(ServerPlayer player) {
        PathwayProgress progress = player.getData(ModAttachments.PATHWAY_PROGRESS).asIronGuardSequence9();

        player.setData(ModAttachments.PATHWAY_PROGRESS, progress);
        IronGuardAttributeService.refresh(player);
        player.sendSystemMessage(Component.literal( "已设置为铁卫途径，序列 9，消化度 0%"));
        return Command.SINGLE_SUCCESS;
    }

    private static int clearPathway(ServerPlayer player) {
        player.setData(ModAttachments.PATHWAY_PROGRESS, PathwayProgress.EMPTY);
        IronGuardAttributeService.refresh(player);
        player.sendSystemMessage(Component.literal("已清除途径数据，恢复为普通人"));
        return Command.SINGLE_SUCCESS;
    }

    private static int setDigestion(ServerPlayer player, int digestion) {
        PathwayProgress current = player.getData(ModAttachments.PATHWAY_PROGRESS);

        if (!current.isIronGuardSequence9()) {
            player.sendSystemMessage(Component.literal("你当前不是序列 9 铁卫，不能设置铁卫消化度"));
            return 0;
        }

        PathwayProgress updated = current.withDigestion(digestion);

        player.setData(ModAttachments.PATHWAY_PROGRESS, updated);

        IronGuardAttributeService.refresh(player);
        
        player.sendSystemMessage(
                Component.literal(
                        "消化度已设置为 "
                                + digestion / 100.0
                                + "%，当前里程碑："
                                + updated.digestionMilestone()
                                + "%"));

        return Command.SINGLE_SUCCESS;
    }

    private static int inspect(ServerPlayer player) {
        PathwayProgress progress = player.getData(ModAttachments.PATHWAY_PROGRESS);

        player.sendSystemMessage(
                Component.literal(
                        "途径数据："
                                + " pathway=" + progress.pathwayId()
                                + ", sequence=" + progress.sequence()
                                + ", digestion=" + progress.digestion()
                                + ", milestone=" + progress.digestionMilestone()
                                + "%"
                )
        );

        return Command.SINGLE_SUCCESS;
    }

}
