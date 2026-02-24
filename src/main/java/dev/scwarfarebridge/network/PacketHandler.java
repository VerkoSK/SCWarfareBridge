package dev.scwarfarebridge.network;

import dev.scwarfarebridge.SCWarfareBridge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class PacketHandler {

    private static final String PROTOCOL_VERSION = "1";
    private static int id = 0;

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(SCWarfareBridge.MOD_ID, "main"))
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .simpleChannel();

    public static void init() {
        CHANNEL.registerMessage(id++, C2SJoinNationPacket.class,
                C2SJoinNationPacket::encode, C2SJoinNationPacket::decode,
                C2SJoinNationPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, C2SLeaveNationPacket.class,
                C2SLeaveNationPacket::encode, C2SLeaveNationPacket::decode,
                C2SLeaveNationPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, C2SCreateNationPacket.class,
                C2SCreateNationPacket::encode, C2SCreateNationPacket::decode,
                C2SCreateNationPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, C2SDiplomacyPacket.class,
                C2SDiplomacyPacket::encode, C2SDiplomacyPacket::decode,
                C2SDiplomacyPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, C2SManageMemberPacket.class,
                C2SManageMemberPacket::encode, C2SManageMemberPacket::decode,
                C2SManageMemberPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, S2CNationsDataPacket.class,
                S2CNationsDataPacket::encode, S2CNationsDataPacket::decode,
                S2CNationsDataPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToAll(Object packet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
