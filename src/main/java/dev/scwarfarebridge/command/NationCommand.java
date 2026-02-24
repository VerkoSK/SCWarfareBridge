package dev.scwarfarebridge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.scwarfarebridge.nation.DiplomacyState;
import dev.scwarfarebridge.nation.Nation;
import dev.scwarfarebridge.nation.NationRank;
import dev.scwarfarebridge.nation.NationSaveData;
import dev.scwarfarebridge.network.S2CNationsDataPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = dev.scwarfarebridge.SCWarfareBridge.MOD_ID)
public class NationCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("nation")
                .then(Commands.literal("list")
                        .executes(NationCommand::listNations))
                .then(Commands.literal("info")
                        .executes(NationCommand::myNationInfo)
                        .then(Commands.argument("nation", StringArgumentType.string())
                                .executes(ctx -> nationInfo(ctx, StringArgumentType.getString(ctx, "nation")))))
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(ctx -> createNation(ctx, StringArgumentType.getString(ctx, "name"), DyeColor.BLUE))
                                .then(Commands.argument("color", StringArgumentType.word())
                                        .executes(ctx -> createNation(ctx, StringArgumentType.getString(ctx, "name"),
                                                DyeColor.byName(StringArgumentType.getString(ctx, "color"), DyeColor.BLUE))))))
                .then(Commands.literal("leave")
                        .executes(NationCommand::leaveNation))
                .then(Commands.literal("disband")
                        .executes(NationCommand::disbandNation))
                .then(Commands.literal("invite")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> invitePlayer(ctx, EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("kick")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> kickPlayer(ctx, EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("promote")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> changeRank(ctx, EntityArgument.getPlayer(ctx, "player"), true))))
                .then(Commands.literal("demote")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> changeRank(ctx, EntityArgument.getPlayer(ctx, "player"), false))))
                .then(Commands.literal("ally")
                        .then(Commands.argument("nation", StringArgumentType.string())
                                .executes(ctx -> setDiplomacy(ctx, StringArgumentType.getString(ctx, "nation"), DiplomacyState.ALLIED))))
                .then(Commands.literal("war")
                        .then(Commands.argument("nation", StringArgumentType.string())
                                .executes(ctx -> setDiplomacy(ctx, StringArgumentType.getString(ctx, "nation"), DiplomacyState.AT_WAR))))
                .then(Commands.literal("neutral")
                        .then(Commands.argument("nation", StringArgumentType.string())
                                .executes(ctx -> setDiplomacy(ctx, StringArgumentType.getString(ctx, "nation"), DiplomacyState.NEUTRAL))))
                .then(Commands.literal("accept")
                        .then(Commands.argument("nation", StringArgumentType.string())
                                .executes(ctx -> acceptInvite(ctx, StringArgumentType.getString(ctx, "nation"))))));
    }

    private static int listNations(CommandContext<CommandSourceStack> ctx) {
        NationSaveData data = NationSaveData.get(ctx.getSource().getServer());
        var nations = data.getNations();
        if (nations.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("§7No nations exist yet."), false);
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("§6§l=== Nations ==="), false);
        for (Nation n : nations) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§e" + n.getName() + " §7[" + n.getMemberCount() + " members]"), false);
        }
        return nations.size();
    }

    private static int myNationInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        NationSaveData data = NationSaveData.get(ctx.getSource().getServer());
        Optional<Nation> optNation = data.getPlayerNation(player.getUUID());
        if (optNation.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("message.scwarfarebridge.not_in_nation"));
            return 0;
        }
        return printNationInfo(ctx, optNation.get());
    }

    private static int nationInfo(CommandContext<CommandSourceStack> ctx, String name) {
        NationSaveData data = NationSaveData.get(ctx.getSource().getServer());
        Optional<Nation> optNation = data.getNationByName(name);
        if (optNation.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("Nation not found: " + name));
            return 0;
        }
        return printNationInfo(ctx, optNation.get());
    }

    private static int printNationInfo(CommandContext<CommandSourceStack> ctx, Nation nation) {
        NationSaveData data = NationSaveData.get(ctx.getSource().getServer());
        ctx.getSource().sendSuccess(() -> Component.literal("§6§l=== " + nation.getName() + " ==="), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§eMembers: §f" + nation.getMemberCount()), false);
        for (var entry : nation.getMembers().entrySet()) {
            NationRank rank = entry.getValue();
            ServerPlayer online = ctx.getSource().getServer().getPlayerList().getPlayer(entry.getKey());
            String playerName = online != null
                    ? online.getName().getString()
                    : entry.getKey().toString().substring(0, 8);
            ctx.getSource().sendSuccess(() -> Component.literal(rank.getColor() + "  [" + rank.name() + "] §f" + playerName), false);
        }
        if (!nation.getDiplomacyMap().isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("§bDiplomacy:"), false);
            for (var entry : nation.getDiplomacyMap().entrySet()) {
                Optional<Nation> other = data.getNation(entry.getKey());
                String otherName = other.map(Nation::getName).orElse("Unknown");
                DiplomacyState state = entry.getValue();
                ctx.getSource().sendSuccess(() -> Component.literal(
                        "  " + state.getColor() + otherName + ": " + state.getDisplayName().getString()), false);
            }
        }
        return 1;
    }

    private static int createNation(CommandContext<CommandSourceStack> ctx, String name, DyeColor color)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        NationSaveData data = NationSaveData.get(ctx.getSource().getServer());
        if (data.getPlayerNation(player.getUUID()).isPresent()) {
            ctx.getSource().sendFailure(Component.translatable("message.scwarfarebridge.already_in_nation"));
            return 0;
        }
        if (name.length() < 3 || name.length() > 24) {
            ctx.getSource().sendFailure(Component.literal("Nation name must be 3-24 characters."));
            return 0;
        }
        if (data.getNationByName(name).isPresent()) {
            ctx.getSource().sendFailure(Component.translatable("message.scwarfarebridge.nation_name_taken"));
            return 0;
        }
        Nation nation = data.createNation(name, color, player.getUUID());
        ctx.getSource().sendSuccess(() -> Component.translatable("message.scwarfarebridge.nation_created", nation.getName()), true);
        S2CNationsDataPacket.broadcastUpdate(ctx.getSource().getServer());
        return 1;
    }

    private static int leaveNation(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        NationSaveData data = NationSaveData.get(ctx.getSource().getServer());
        if (data.getPlayerNation(player.getUUID()).isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("message.scwarfarebridge.not_in_nation"));
            return 0;
        }
        data.removePlayerFromNation(player.getUUID());
        ctx.getSource().sendSuccess(() -> Component.translatable("message.scwarfarebridge.nation_left"), false);
        S2CNationsDataPacket.broadcastUpdate(ctx.getSource().getServer());
        return 1;
    }

    private static int disbandNation(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        NationSaveData data = NationSaveData.get(ctx.getSource().getServer());
        Optional<Nation> optNation = data.getPlayerNation(player.getUUID());
        if (optNation.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("message.scwarfarebridge.not_in_nation"));
            return 0;
        }
        if (optNation.get().getRank(player.getUUID()) != NationRank.LEADER) {
            ctx.getSource().sendFailure(Component.translatable("message.scwarfarebridge.no_permission"));
            return 0;
        }
        data.disbandNation(optNation.get().getId());
        ctx.getSource().sendSuccess(() -> Component.translatable("message.scwarfarebridge.nation_disbanded"), true);
        S2CNationsDataPacket.broadcastUpdate(ctx.getSource().getServer());
        return 1;
    }

    private static int invitePlayer(CommandContext<CommandSourceStack> ctx, ServerPlayer target)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        NationSaveData data = NationSaveData.get(ctx.getSource().getServer());
        Optional<Nation> optNation = data.getPlayerNation(player.getUUID());
        if (optNation.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("message.scwarfarebridge.not_in_nation"));
            return 0;
        }
        NationRank rank = optNation.get().getRank(player.getUUID());
        if (rank != NationRank.LEADER && rank != NationRank.OFFICER) {
            ctx.getSource().sendFailure(Component.translatable("message.scwarfarebridge.no_permission"));
            return 0;
        }
        data.addInvite(optNation.get().getId(), target.getUUID());
        target.sendSystemMessage(Component.translatable("message.scwarfarebridge.invite_received", optNation.get().getName()));
        ctx.getSource().sendSuccess(() -> Component.translatable("message.scwarfarebridge.player_invited",
                target.getName().getString()), false);
        S2CNationsDataPacket.broadcastUpdate(ctx.getSource().getServer());
        return 1;
    }

    private static int kickPlayer(CommandContext<CommandSourceStack> ctx, ServerPlayer target)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        NationSaveData data = NationSaveData.get(ctx.getSource().getServer());
        Optional<Nation> optNation = data.getPlayerNation(player.getUUID());
        if (optNation.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("message.scwarfarebridge.not_in_nation"));
            return 0;
        }
        NationRank myRank = optNation.get().getRank(player.getUUID());
        NationRank targetRank = optNation.get().getRank(target.getUUID());
        if (!myRank.canManage(targetRank)) {
            ctx.getSource().sendFailure(Component.translatable("message.scwarfarebridge.no_permission"));
            return 0;
        }
        data.removePlayerFromNation(target.getUUID());
        target.sendSystemMessage(Component.translatable("message.scwarfarebridge.nation_left"));
        ctx.getSource().sendSuccess(() -> Component.translatable("message.scwarfarebridge.player_kicked",
                target.getName().getString()), false);
        S2CNationsDataPacket.broadcastUpdate(ctx.getSource().getServer());
        return 1;
    }

    private static int changeRank(CommandContext<CommandSourceStack> ctx, ServerPlayer target, boolean promote)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        NationSaveData data = NationSaveData.get(ctx.getSource().getServer());
        Optional<Nation> optNation = data.getPlayerNation(player.getUUID());
        if (optNation.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("message.scwarfarebridge.not_in_nation"));
            return 0;
        }
        if (optNation.get().getRank(player.getUUID()) != NationRank.LEADER) {
            ctx.getSource().sendFailure(Component.translatable("message.scwarfarebridge.no_permission"));
            return 0;
        }
        NationRank current = optNation.get().getRank(target.getUUID());
        NationRank newRank;
        if (promote) {
            newRank = current == NationRank.RECRUIT ? NationRank.OFFICER : NationRank.LEADER;
            if (newRank == NationRank.LEADER) {
                data.setPlayerRank(optNation.get().getId(), player.getUUID(), NationRank.OFFICER);
            }
        } else {
            newRank = NationRank.RECRUIT;
        }
        data.setPlayerRank(optNation.get().getId(), target.getUUID(), newRank);
        NationRank finalRank = newRank;
        ctx.getSource().sendSuccess(() -> Component.translatable("message.scwarfarebridge.rank_changed",
                target.getName().getString(), finalRank.getDisplayName().getString()), false);
        S2CNationsDataPacket.broadcastUpdate(ctx.getSource().getServer());
        return 1;
    }

    private static int setDiplomacy(CommandContext<CommandSourceStack> ctx, String nationName, DiplomacyState state)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        NationSaveData data = NationSaveData.get(ctx.getSource().getServer());
        Optional<Nation> myNation = data.getPlayerNation(player.getUUID());
        if (myNation.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("message.scwarfarebridge.not_in_nation"));
            return 0;
        }
        NationRank rank = myNation.get().getRank(player.getUUID());
        if (rank != NationRank.LEADER && rank != NationRank.OFFICER) {
            ctx.getSource().sendFailure(Component.translatable("message.scwarfarebridge.no_permission"));
            return 0;
        }
        Optional<Nation> target = data.getNationByName(nationName);
        if (target.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("Nation not found: " + nationName));
            return 0;
        }
        data.setDiplomacy(myNation.get().getId(), target.get().getId(), state);
        ctx.getSource().sendSuccess(() -> Component.translatable("message.scwarfarebridge.diplomacy_changed",
                target.get().getName(), state.getDisplayName().getString()), true);
        S2CNationsDataPacket.broadcastUpdate(ctx.getSource().getServer());
        return 1;
    }

    private static int acceptInvite(CommandContext<CommandSourceStack> ctx, String nationName)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        NationSaveData data = NationSaveData.get(ctx.getSource().getServer());
        if (data.getPlayerNation(player.getUUID()).isPresent()) {
            ctx.getSource().sendFailure(Component.translatable("message.scwarfarebridge.already_in_nation"));
            return 0;
        }
        Optional<Nation> target = data.getNationByName(nationName);
        if (target.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("Nation not found: " + nationName));
            return 0;
        }
        if (!target.get().hasInvite(player.getUUID())) {
            ctx.getSource().sendFailure(Component.translatable("message.scwarfarebridge.no_invite"));
            return 0;
        }
        data.addPlayerToNation(player.getUUID(), target.get().getId());
        ctx.getSource().sendSuccess(() -> Component.translatable("message.scwarfarebridge.invite_accepted"), false);
        S2CNationsDataPacket.broadcastUpdate(ctx.getSource().getServer());
        return 1;
    }
}
