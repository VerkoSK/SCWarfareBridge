package dev.scwarfarebridge.network;

import dev.scwarfarebridge.nation.DiplomacyState;
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

public class C2SDiplomacyPacket {
    private final UUID targetNationId;
    private final DiplomacyState state;

    public C2SDiplomacyPacket(UUID targetNationId, DiplomacyState state) {
        this.targetNationId = targetNationId;
        this.state = state;
    }

    public static void encode(C2SDiplomacyPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.targetNationId);
        buf.writeEnum(msg.state);
    }

    public static C2SDiplomacyPacket decode(FriendlyByteBuf buf) {
        return new C2SDiplomacyPacket(buf.readUUID(), buf.readEnum(DiplomacyState.class));
    }

    public static void handle(C2SDiplomacyPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            NationSaveData data = NationSaveData.get(player.server);

            Optional<Nation> optNation = data.getPlayerNation(player.getUUID());
            if (optNation.isEmpty()) {
                player.sendSystemMessage(Component.translatable("message.scwarfarebridge.not_in_nation"));
                return;
            }
            Nation nation = optNation.get();

            NationRank rank = nation.getRank(player.getUUID());
            if (rank != NationRank.LEADER && rank != NationRank.OFFICER) {
                player.sendSystemMessage(Component.translatable("message.scwarfarebridge.no_permission"));
                return;
            }

            Optional<Nation> target = data.getNation(msg.targetNationId);
            if (target.isEmpty()) return;

            data.setDiplomacy(nation.getId(), msg.targetNationId, msg.state);
            player.sendSystemMessage(Component.translatable("message.scwarfarebridge.diplomacy_changed",
                    target.get().getName(), msg.state.getDisplayName()));
            S2CNationsDataPacket.broadcastUpdate(player.server);
        });
        ctx.get().setPacketHandled(true);
    }
}
