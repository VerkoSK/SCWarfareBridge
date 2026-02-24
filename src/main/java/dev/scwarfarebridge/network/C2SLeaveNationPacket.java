package dev.scwarfarebridge.network;

import dev.scwarfarebridge.nation.Nation;
import dev.scwarfarebridge.nation.NationSaveData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class C2SLeaveNationPacket {
    public C2SLeaveNationPacket() {}

    public static void encode(C2SLeaveNationPacket msg, FriendlyByteBuf buf) {}

    public static C2SLeaveNationPacket decode(FriendlyByteBuf buf) { return new C2SLeaveNationPacket(); }

    public static void handle(C2SLeaveNationPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            NationSaveData data = NationSaveData.get(player.server);

            Optional<Nation> nation = data.getPlayerNation(player.getUUID());
            if (nation.isEmpty()) {
                player.sendSystemMessage(Component.translatable("message.scwarfarebridge.not_in_nation"));
                return;
            }

            data.removePlayerFromNation(player.getUUID());
            player.sendSystemMessage(Component.translatable("message.scwarfarebridge.nation_left"));
            S2CNationsDataPacket.broadcastUpdate(player.server);
        });
        ctx.get().setPacketHandled(true);
    }
}
