package dev.scwarfarebridge.client;

import dev.scwarfarebridge.nation.DiplomacyState;
import dev.scwarfarebridge.nation.NationRank;
import dev.scwarfarebridge.network.S2CNationsDataPacket;
import net.minecraft.world.item.DyeColor;

import java.util.*;

/** Client-side cache of nation data, updated via S2CNationsDataPacket. */
public class ClientNationData {

    public record NationInfo(
            UUID id,
            String name,
            DyeColor color,
            String description,
            Map<UUID, NationRank> members,
            Map<UUID, DiplomacyState> diplomacy,
            Set<UUID> pendingInvites
    ) {
        public int memberCount() { return members.size(); }

        public Optional<UUID> getLeader() {
            return members.entrySet().stream()
                    .filter(e -> e.getValue() == NationRank.LEADER)
                    .map(Map.Entry::getKey)
                    .findFirst();
        }

        public DiplomacyState getDiplomacyWith(UUID otherNationId) {
            return diplomacy.getOrDefault(otherNationId, DiplomacyState.NEUTRAL);
        }
    }

    private static List<NationInfo> nations = new ArrayList<>();
    private static Map<UUID, UUID> playerToNation = new HashMap<>();

    public static void update(List<S2CNationsDataPacket.NationDto> dtos, Map<UUID, UUID> p2n) {
        List<NationInfo> list = new ArrayList<>(dtos.size());
        for (S2CNationsDataPacket.NationDto dto : dtos) {
            list.add(new NationInfo(dto.id(), dto.name(), dto.color(), dto.description(),
                    dto.members(), dto.diplomacy(), dto.pendingInvites()));
        }
        nations = list;
        playerToNation = new HashMap<>(p2n);
    }

    public static List<NationInfo> getNations() { return Collections.unmodifiableList(nations); }

    public static Optional<NationInfo> getNation(UUID id) {
        return nations.stream().filter(n -> n.id().equals(id)).findFirst();
    }

    public static Optional<NationInfo> getPlayerNation(UUID playerUUID) {
        UUID nationId = playerToNation.get(playerUUID);
        if (nationId == null) return Optional.empty();
        return getNation(nationId);
    }
}
