package dev.scwarfarebridge.network;

import dev.scwarfarebridge.client.ClientNationData;
import dev.scwarfarebridge.nation.DiplomacyState;
import dev.scwarfarebridge.nation.Nation;
import dev.scwarfarebridge.nation.NationRank;
import dev.scwarfarebridge.nation.NationSaveData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

public class S2CNationsDataPacket {

    /** Lightweight DTO for network transfer */
    public record NationDto(
            UUID id,
            String name,
            DyeColor color,
            String description,
            Map<UUID, NationRank> members,
            Map<UUID, DiplomacyState> diplomacy,
            Set<UUID> pendingInvites
    ) {}

    private final List<NationDto> nations;
    private final Map<UUID, UUID> playerToNation;

    public S2CNationsDataPacket(List<NationDto> nations, Map<UUID, UUID> playerToNation) {
        this.nations = nations;
        this.playerToNation = playerToNation;
    }

    public static void encode(S2CNationsDataPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.nations.size());
        for (NationDto dto : msg.nations) {
            buf.writeUUID(dto.id());
            buf.writeUtf(dto.name(), 24);
            buf.writeByte(dto.color().getId());
            buf.writeUtf(dto.description(), 256);

            buf.writeInt(dto.members().size());
            for (Map.Entry<UUID, NationRank> e : dto.members().entrySet()) {
                buf.writeUUID(e.getKey());
                buf.writeEnum(e.getValue());
            }

            buf.writeInt(dto.diplomacy().size());
            for (Map.Entry<UUID, DiplomacyState> e : dto.diplomacy().entrySet()) {
                buf.writeUUID(e.getKey());
                buf.writeEnum(e.getValue());
            }

            buf.writeInt(dto.pendingInvites().size());
            for (UUID invite : dto.pendingInvites()) {
                buf.writeUUID(invite);
            }
        }

        buf.writeInt(msg.playerToNation.size());
        for (Map.Entry<UUID, UUID> e : msg.playerToNation.entrySet()) {
            buf.writeUUID(e.getKey());
            buf.writeUUID(e.getValue());
        }
    }

    public static S2CNationsDataPacket decode(FriendlyByteBuf buf) {
        int nationCount = buf.readInt();
        List<NationDto> nations = new ArrayList<>(nationCount);
        for (int i = 0; i < nationCount; i++) {
            UUID id = buf.readUUID();
            String name = buf.readUtf(24);
            DyeColor color = DyeColor.byId(buf.readByte());
            String description = buf.readUtf(256);

            int memberCount = buf.readInt();
            Map<UUID, NationRank> members = new LinkedHashMap<>(memberCount);
            for (int m = 0; m < memberCount; m++) {
                members.put(buf.readUUID(), buf.readEnum(NationRank.class));
            }

            int diplomacyCount = buf.readInt();
            Map<UUID, DiplomacyState> diplomacy = new HashMap<>(diplomacyCount);
            for (int d = 0; d < diplomacyCount; d++) {
                diplomacy.put(buf.readUUID(), buf.readEnum(DiplomacyState.class));
            }

            int inviteCount = buf.readInt();
            Set<UUID> invites = new HashSet<>(inviteCount);
            for (int v = 0; v < inviteCount; v++) {
                invites.add(buf.readUUID());
            }

            nations.add(new NationDto(id, name, color, description, members, diplomacy, invites));
        }

        int mapSize = buf.readInt();
        Map<UUID, UUID> playerToNation = new HashMap<>(mapSize);
        for (int i = 0; i < mapSize; i++) {
            playerToNation.put(buf.readUUID(), buf.readUUID());
        }

        return new S2CNationsDataPacket(nations, playerToNation);
    }

    public static void handle(S2CNationsDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientNationData.update(msg.nations, msg.playerToNation));
        ctx.get().setPacketHandled(true);
    }

    // -----------------------------------------------------------------------
    // Server-side helper: build and send updated data to all players
    // -----------------------------------------------------------------------

    public static void broadcastUpdate(MinecraftServer server) {
        S2CNationsDataPacket packet = buildPacket(NationSaveData.get(server));
        PacketHandler.sendToAll(packet);
    }

    public static void sendToPlayer(net.minecraft.server.level.ServerPlayer player) {
        S2CNationsDataPacket packet = buildPacket(NationSaveData.get(player.server));
        PacketHandler.sendToPlayer(player, packet);
    }

    private static S2CNationsDataPacket buildPacket(NationSaveData data) {
        List<NationDto> dtos = new ArrayList<>();
        Map<UUID, UUID> playerToNation = new HashMap<>();

        for (Nation nation : data.getNations()) {
            dtos.add(new NationDto(
                    nation.getId(),
                    nation.getName(),
                    nation.getColor(),
                    nation.getDescription(),
                    new LinkedHashMap<>(nation.getMembers()),
                    new HashMap<>(nation.getDiplomacyMap()),
                    new HashSet<>(nation.getPendingInvites())
            ));
            for (UUID player : nation.getMembers().keySet()) {
                playerToNation.put(player, nation.getId());
            }
        }

        return new S2CNationsDataPacket(dtos, playerToNation);
    }
}
