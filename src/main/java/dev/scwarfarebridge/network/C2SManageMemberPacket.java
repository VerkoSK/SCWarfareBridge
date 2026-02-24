package dev.scwarfarebridge.network;

import dev.scwarfarebridge.nation.Nation;
import dev.scwarfarebridge.nation.NationRank;
import dev.scwarfarebridge.nation.NationSaveData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/** Handles invite/kick/promote/demote actions from the leader or officer. */
public class C2SManageMemberPacket {

    public enum Action { INVITE, KICK, PROMOTE, DEMOTE }

    private final UUID targetPlayerUUID;
    private final Action action;

    public C2SManageMemberPacket(UUID targetPlayerUUID, Action action) {
        this.targetPlayerUUID = targetPlayerUUID;
        this.action = action;
    }

    public static void encode(C2SManageMemberPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.targetPlayerUUID);
        buf.writeEnum(msg.action);
    }

    public static C2SManageMemberPacket decode(FriendlyByteBuf buf) {
        return new C2SManageMemberPacket(buf.readUUID(), buf.readEnum(Action.class));
    }

    public static void handle(C2SManageMemberPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;
            NationSaveData data = NationSaveData.get(sender.server);

            Optional<Nation> optNation = data.getPlayerNation(sender.getUUID());
            if (optNation.isEmpty()) {
                sender.sendSystemMessage(Component.translatable("message.scwarfarebridge.not_in_nation"));
                return;
            }
            Nation nation = optNation.get();
            NationRank senderRank = nation.getRank(sender.getUUID());

            switch (msg.action) {
                case INVITE -> {
                    if (senderRank != NationRank.LEADER && senderRank != NationRank.OFFICER) {
                        sender.sendSystemMessage(Component.translatable("message.scwarfarebridge.no_permission"));
                        return;
                    }
                    data.addInvite(nation.getId(), msg.targetPlayerUUID);
                    ServerPlayer target = sender.server.getPlayerList().getPlayer(msg.targetPlayerUUID);
                    if (target != null) {
                        target.sendSystemMessage(Component.translatable("message.scwarfarebridge.invite_received", nation.getName()));
                    }
                    sender.sendSystemMessage(Component.translatable("message.scwarfarebridge.player_invited",
                            msg.targetPlayerUUID.toString()));
                    S2CNationsDataPacket.broadcastUpdate(sender.server);
                }
                case KICK -> {
                    NationRank targetRank = nation.getRank(msg.targetPlayerUUID);
                    if (!senderRank.canManage(targetRank)) {
                        sender.sendSystemMessage(Component.translatable("message.scwarfarebridge.no_permission"));
                        return;
                    }
                    data.removePlayerFromNation(msg.targetPlayerUUID);
                    ServerPlayer target = sender.server.getPlayerList().getPlayer(msg.targetPlayerUUID);
                    if (target != null) {
                        target.sendSystemMessage(Component.translatable("message.scwarfarebridge.nation_left"));
                    }
                    sender.sendSystemMessage(Component.translatable("message.scwarfarebridge.player_kicked",
                            msg.targetPlayerUUID.toString()));
                    S2CNationsDataPacket.broadcastUpdate(sender.server);
                }
                case PROMOTE -> {
                    if (senderRank != NationRank.LEADER) {
                        sender.sendSystemMessage(Component.translatable("message.scwarfarebridge.no_permission"));
                        return;
                    }
                    NationRank current = nation.getRank(msg.targetPlayerUUID);
                    NationRank promoted = current == NationRank.RECRUIT ? NationRank.OFFICER : NationRank.LEADER;
                    if (promoted == NationRank.LEADER) {
                        // Demote sender to officer when transferring leadership
                        data.setPlayerRank(nation.getId(), sender.getUUID(), NationRank.OFFICER);
                    }
                    data.setPlayerRank(nation.getId(), msg.targetPlayerUUID, promoted);
                    sender.sendSystemMessage(Component.translatable("message.scwarfarebridge.rank_changed",
                            msg.targetPlayerUUID.toString(), promoted.getDisplayName()));
                    S2CNationsDataPacket.broadcastUpdate(sender.server);
                }
                case DEMOTE -> {
                    if (senderRank != NationRank.LEADER) {
                        sender.sendSystemMessage(Component.translatable("message.scwarfarebridge.no_permission"));
                        return;
                    }
                    NationRank current = nation.getRank(msg.targetPlayerUUID);
                    // OFFICER -> RECRUIT, RECRUIT stays RECRUIT
                    NationRank demoted = current == NationRank.OFFICER ? NationRank.RECRUIT : NationRank.RECRUIT;
                    data.setPlayerRank(nation.getId(), msg.targetPlayerUUID, demoted);
                    sender.sendSystemMessage(Component.translatable("message.scwarfarebridge.rank_changed",
                            msg.targetPlayerUUID.toString(), demoted.getDisplayName()));
                    S2CNationsDataPacket.broadcastUpdate(sender.server);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
