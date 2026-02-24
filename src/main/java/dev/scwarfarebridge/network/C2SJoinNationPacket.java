package dev.scwarfarebridge.network;

import dev.scwarfarebridge.nation.Nation;
import dev.scwarfarebridge.nation.NationSaveData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class C2SJoinNationPacket {
    private final UUID nationId;

    public C2SJoinNationPacket(UUID nationId) { this.nationId = nationId; }

    public static void encode(C2SJoinNationPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.nationId);
    }

    public static C2SJoinNationPacket decode(FriendlyByteBuf buf) {
        return new C2SJoinNationPacket(buf.readUUID());
    }

    public static void handle(C2SJoinNationPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            NationSaveData data = NationSaveData.get(player.server);

            if (data.getPlayerNation(player.getUUID()).isPresent()) {
                player.sendSystemMessage(Component.translatable("message.scwarfarebridge.already_in_nation"));
                return;
            }

            Optional<Nation> nation = data.getNation(msg.nationId);
            if (nation.isEmpty()) return;

            if (!nation.get().hasInvite(player.getUUID())) {
                player.sendSystemMessage(Component.translatable("message.scwarfarebridge.no_invite"));
                return;
            }

            data.addPlayerToNation(player.getUUID(), msg.nationId);
            player.sendSystemMessage(Component.translatable("message.scwarfarebridge.nation_joined", nation.get().getName()));
            S2CNationsDataPacket.broadcastUpdate(player.server);
        });
        ctx.get().setPacketHandled(true);
    }
}
