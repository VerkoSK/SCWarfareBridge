package dev.scwarfarebridge.network;

import dev.scwarfarebridge.nation.Nation;
import dev.scwarfarebridge.nation.NationSaveData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SCreateNationPacket {
    private final String name;
    private final DyeColor color;

    public C2SCreateNationPacket(String name, DyeColor color) {
        this.name = name;
        this.color = color;
    }

    public static void encode(C2SCreateNationPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.name, 24);
        buf.writeByte(msg.color.getId());
    }

    public static C2SCreateNationPacket decode(FriendlyByteBuf buf) {
        return new C2SCreateNationPacket(buf.readUtf(24), DyeColor.byId(buf.readByte()));
    }

    public static void handle(C2SCreateNationPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            NationSaveData data = NationSaveData.get(player.server);

            if (data.getPlayerNation(player.getUUID()).isPresent()) {
                player.sendSystemMessage(Component.translatable("message.scwarfarebridge.already_in_nation"));
                return;
            }

            String name = msg.name.trim();
            if (name.length() < 3) {
                player.sendSystemMessage(Component.translatable("message.scwarfarebridge.name_too_short"));
                return;
            }
            if (name.length() > 24) {
                player.sendSystemMessage(Component.translatable("message.scwarfarebridge.name_too_long"));
                return;
            }

            if (data.getNationByName(name).isPresent()) {
                player.sendSystemMessage(Component.translatable("message.scwarfarebridge.nation_name_taken"));
                return;
            }

            Nation nation = data.createNation(name, msg.color, player.getUUID());
            player.sendSystemMessage(Component.translatable("message.scwarfarebridge.nation_created", nation.getName()));
            S2CNationsDataPacket.broadcastUpdate(player.server);
        });
        ctx.get().setPacketHandled(true);
    }
}
