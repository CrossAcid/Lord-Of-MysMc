package io.github.crossacid.whisperway.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.Command;

import io.github.crossacid.whisperway.Whisperway;
import io.github.crossacid.whisperway.data.ModAttachments;
import io.github.crossacid.whisperway.data.PathwayProgress;
import io.github.crossacid.whisperway.pathway.ironguard.IronGuardAttributeService;
import io.github.crossacid.whisperway.pathway.ironguard.IronGuardCombatService;
import io.github.crossacid.whisperway.pathway.ironguard.IronGuardCombatState;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
                                                                                .requires(source -> source
                                                                                                .hasPermission(2))
                                                                                .executes(context -> setIronGuard(
                                                                                                context.getSource()
                                                                                                                .getPlayerOrException())))

                                                                // /whisperway pathway clear
                                                                .then(Commands.literal("clear")
                                                                                .requires(source -> source
                                                                                                .hasPermission(2))
                                                                                .executes(context -> clearPathway(
                                                                                                context.getSource()
                                                                                                                .getPlayerOrException()))))

                                                // /whisperway digestion set <value>
                                                .then(Commands.literal("digestion")
                                                                .then(Commands.literal("set")
                                                                                .requires(source -> source
                                                                                                .hasPermission(2))
                                                                                .then(Commands.argument(
                                                                                                "value",
                                                                                                IntegerArgumentType
                                                                                                                .integer(
                                                                                                                                0,
                                                                                                                                PathwayProgress.MAX_DIGESTION))
                                                                                                .executes(context -> setDigestion(
                                                                                                                context.getSource()
                                                                                                                                .getPlayerOrException(),
                                                                                                                IntegerArgumentType
                                                                                                                                .getInteger(
                                                                                                                                                context,
                                                                                                                                                "value"))))))

                                                // 战势测试命令
                                                .then(Commands.literal("momentum")
                                                                .requires(source -> source.hasPermission(2))

                                                                // /whisperway momentum set <value>
                                                                .then(Commands.literal("set")
                                                                                .then(Commands.argument(
                                                                                                "value",
                                                                                                IntegerArgumentType
                                                                                                                .integer(
                                                                                                                                0,
                                                                                                                                IronGuardCombatState.MAX_MOMENTUM))
                                                                                                .executes(context -> setMomentum(
                                                                                                                context.getSource()
                                                                                                                                .getPlayerOrException(),
                                                                                                                IntegerArgumentType
                                                                                                                                .getInteger(
                                                                                                                                                context,
                                                                                                                                                "value")))))

                                                                // /whisperway momentum add <value>
                                                                .then(Commands.literal("add")
                                                                                .then(Commands.argument(
                                                                                                "value",
                                                                                                IntegerArgumentType
                                                                                                                .integer(
                                                                                                                                1,
                                                                                                                                IronGuardCombatState.MAX_MOMENTUM))
                                                                                                .executes(context -> addMomentum(
                                                                                                                context.getSource()
                                                                                                                                .getPlayerOrException(),
                                                                                                                IntegerArgumentType
                                                                                                                                .getInteger(
                                                                                                                                                context,
                                                                                                                                                "value")))))

                                                                // /whisperway momentum spend <value>
                                                                .then(Commands.literal("spend")
                                                                                .then(Commands.argument(
                                                                                                "value",
                                                                                                IntegerArgumentType
                                                                                                                .integer(
                                                                                                                                1,
                                                                                                                                IronGuardCombatState.MAX_MOMENTUM))
                                                                                                .executes(context -> spendMomentum(
                                                                                                                context.getSource()
                                                                                                                                .getPlayerOrException(),
                                                                                                                IntegerArgumentType
                                                                                                                                .getInteger(
                                                                                                                                                context,
                                                                                                                                                "value")))))

                                                                // /whisperway momentum clear
                                                                .then(Commands.literal("clear")
                                                                                .executes(context -> clearMomentum(
                                                                                                context.getSource()
                                                                                                                .getPlayerOrException()))))

                                                // /whisperway inspect
                                                .then(Commands.literal("inspect")
                                                                .executes(context -> inspect(
                                                                                context.getSource()
                                                                                                .getPlayerOrException()))));
        }

        private static int setIronGuard(ServerPlayer player) {
                PathwayProgress progress = player.getData(ModAttachments.PATHWAY_PROGRESS).asIronGuardSequence9();

                // 重新选择铁卫途径时从零战势开始。
                player.setData(ModAttachments.PATHWAY_PROGRESS, progress);
                IronGuardCombatService.clearMomentum(player);
                IronGuardAttributeService.refresh(player);
                player.sendSystemMessage(Component.literal("已设置为铁卫途径，序列 9，消化度 0%"));
                return Command.SINGLE_SUCCESS;
        }

        private static int clearPathway(ServerPlayer player) {
                player.setData(ModAttachments.PATHWAY_PROGRESS, PathwayProgress.EMPTY);
                // 清除途径时也必须清除战势。
                IronGuardCombatService.clearMomentum(player);
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
                                Component.literal("消化度已设置为 " + digestion / 100.0 + "%，当前里程碑："
                                                + updated.digestionMilestone() + "%"));

                return Command.SINGLE_SUCCESS;
        }

        /**
         * 检查玩家是否为序列 9 铁卫。
         */
        private static boolean requireIronGuard(ServerPlayer player) {
                PathwayProgress progress = player.getData(ModAttachments.PATHWAY_PROGRESS);

                if (!progress.isIronGuardSequence9()) {
                        player.sendSystemMessage(Component.literal("只有序列 9 铁卫才能使用战势"));
                        return false;
                }

                return true;
        }

        private static int setMomentum(ServerPlayer player, int value) {
                if (!requireIronGuard(player)) {
                        return 0;
                }

                IronGuardCombatState state = new IronGuardCombatState(value, player.level().getGameTime());

                player.setData(ModAttachments.IRON_GUARD_COMBAT_STATE, state);

                player.sendSystemMessage(
                                Component.literal("战势已设置为 " + state.momentum() + "/"
                                                + IronGuardCombatState.MAX_MOMENTUM));

                return Command.SINGLE_SUCCESS;
        }

        private static int addMomentum(ServerPlayer player, int amount) {
                if (!requireIronGuard(player)) {
                        return 0;
                }

                IronGuardCombatState current = player.getData(ModAttachments.IRON_GUARD_COMBAT_STATE);
                IronGuardCombatState updated = current.gain(amount, player.level().getGameTime());
                player.setData(ModAttachments.IRON_GUARD_COMBAT_STATE, updated);

                player.sendSystemMessage(Component.literal(
                                "获得 " + amount + " 点战势，当前战势：" + updated.momentum() + "/"
                                                + IronGuardCombatState.MAX_MOMENTUM));

                return Command.SINGLE_SUCCESS;
        }

        private static int spendMomentum(ServerPlayer player, int amount) {
                if (!requireIronGuard(player)) {
                        return 0;
                }

                IronGuardCombatState current = player.getData(ModAttachments.IRON_GUARD_COMBAT_STATE);
                if (!current.canSpend(amount)) {
                        player.sendSystemMessage(
                                        Component.literal("战势不足，当前只有 " + current.momentum() + " 点"));
                        return 0;
                }

                IronGuardCombatState updated = current.spend(amount);
                player.setData(ModAttachments.IRON_GUARD_COMBAT_STATE, updated);
                player.sendSystemMessage(
                                Component.literal("消耗 " + amount + " 点战势，剩余：" + updated.momentum()));

                return Command.SINGLE_SUCCESS;
        }

        private static int clearMomentum(ServerPlayer player) {
                IronGuardCombatService.clearMomentum(player);
                player.sendSystemMessage(Component.literal("战势已清空"));

                return Command.SINGLE_SUCCESS;
        }

        /**
         * 查看玩家当前的途径数据。
         *
         * 这里先显示所有途径共有的数据，再根据当前途径追加专属状态。
         */
        private static int inspect(ServerPlayer player) {
                PathwayProgress progress = player.getData(ModAttachments.PATHWAY_PROGRESS);

                MutableComponent message = Component.literal(
                                "途径数据："
                                                + " pathway=" + progress.pathwayId()
                                                + ", sequence=" + progress.sequence()
                                                + ", digestion=" + progress.digestion()
                                                + ", milestone=" + progress.digestionMilestone()
                                                + "%");

                /*
                 * 只有序列 9 铁卫才显示战势。
                 *
                 * 以后加入其他途径时，可以在这里调用对应的检查方法，
                 * 不需要把其他途径的数据塞进铁卫逻辑。
                 */
                if (progress.isIronGuardSequence9()) {
                        appendIronGuardInspection(player, message);
                }

                player.sendSystemMessage(message);
                return Command.SINGLE_SUCCESS;
        }

        /**
         * 向检查信息中追加铁卫专属状态。
         */
        private static void appendIronGuardInspection(ServerPlayer player, MutableComponent message) {
                IronGuardCombatState combatState = player.getData(ModAttachments.IRON_GUARD_COMBAT_STATE);

                message.append(
                                Component.literal(", momentum=" + combatState.momentum() + "/"
                                                + IronGuardCombatState.MAX_MOMENTUM));
        }

}
