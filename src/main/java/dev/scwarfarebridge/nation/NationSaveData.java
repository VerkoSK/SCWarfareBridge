package dev.scwarfarebridge.nation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.*;

public class NationSaveData extends SavedData {

    public static final String DATA_NAME = "scwarfarebridge_nations";

    /** All nations, keyed by nation UUID */
    private final Map<UUID, Nation> nations = new LinkedHashMap<>();
    /** Fast lookup: player UUID -> nation UUID */
    private final Map<UUID, UUID> playerToNation = new HashMap<>();

    public NationSaveData() {}

    // -----------------------------------------------------------------------
    // Persistence
    // -----------------------------------------------------------------------

    public static NationSaveData load(CompoundTag tag) {
        NationSaveData data = new NationSaveData();
        ListTag list = tag.getList("nations", 10);
        for (int i = 0; i < list.size(); i++) {
            Nation nation = Nation.load(list.getCompound(i));
            data.nations.put(nation.getId(), nation);
        }
        // Rebuild playerToNation index
        for (Nation nation : data.nations.values()) {
            for (UUID player : nation.getMembers().keySet()) {
                data.playerToNation.put(player, nation.getId());
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Nation nation : nations.values()) list.add(nation.save());
        tag.put("nations", list);
        return tag;
    }

    public static NationSaveData get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(NationSaveData::load, NationSaveData::new, DATA_NAME);
    }

    // -----------------------------------------------------------------------
    // Queries
    // -----------------------------------------------------------------------

    public Collection<Nation> getNations() { return Collections.unmodifiableCollection(nations.values()); }

    public Optional<Nation> getNation(UUID nationId) { return Optional.ofNullable(nations.get(nationId)); }

    public Optional<Nation> getNationByName(String name) {
        return nations.values().stream()
                .filter(n -> n.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    public Optional<Nation> getPlayerNation(UUID playerUUID) {
        UUID nationId = playerToNation.get(playerUUID);
        return nationId == null ? Optional.empty() : Optional.ofNullable(nations.get(nationId));
    }

    public boolean areAllied(UUID p1, UUID p2) {
        UUID n1 = playerToNation.get(p1);
        UUID n2 = playerToNation.get(p2);
        if (n1 == null || n2 == null) return false;
        if (n1.equals(n2)) return true;
        Nation nation1 = nations.get(n1);
        return nation1 != null && nation1.getDiplomacyWith(n2) == DiplomacyState.ALLIED;
    }

    public boolean areAtWar(UUID p1, UUID p2) {
        UUID n1 = playerToNation.get(p1);
        UUID n2 = playerToNation.get(p2);
        if (n1 == null || n2 == null || n1.equals(n2)) return false;
        Nation nation1 = nations.get(n1);
        return nation1 != null && nation1.getDiplomacyWith(n2) == DiplomacyState.AT_WAR;
    }

    // -----------------------------------------------------------------------
    // Mutations
    // -----------------------------------------------------------------------

    public Nation createNation(String name, DyeColor color, UUID leaderUUID) {
        Nation nation = new Nation(UUID.randomUUID(), name, color);
        nation.addMember(leaderUUID, NationRank.LEADER);
        nations.put(nation.getId(), nation);
        playerToNation.put(leaderUUID, nation.getId());
        setDirty();
        return nation;
    }

    public void disbandNation(UUID nationId) {
        Nation nation = nations.remove(nationId);
        if (nation != null) {
            for (UUID player : nation.getMembers().keySet()) playerToNation.remove(player);
            for (Nation other : nations.values()) other.setDiplomacy(nationId, DiplomacyState.NEUTRAL);
        }
        setDirty();
    }

    /** Removes player from current nation (if any) and adds them to the target nation. */
    public boolean addPlayerToNation(UUID playerUUID, UUID nationId) {
        Nation nation = nations.get(nationId);
        if (nation == null) return false;
        removePlayerFromNation(playerUUID);
        nation.addMember(playerUUID, NationRank.RECRUIT);
        playerToNation.put(playerUUID, nationId);
        setDirty();
        return true;
    }

    public void removePlayerFromNation(UUID playerUUID) {
        UUID nationId = playerToNation.remove(playerUUID);
        if (nationId == null) return;
        Nation nation = nations.get(nationId);
        if (nation == null) return;
        nation.removeMember(playerUUID);
        if (nation.getMembers().isEmpty()) {
            nations.remove(nationId);
            for (Nation other : nations.values()) other.setDiplomacy(nationId, DiplomacyState.NEUTRAL);
        } else if (nation.getLeader().isEmpty()) {
            // Promote highest-ranking remaining member
            nation.getMembers().entrySet().stream()
                    .filter(e -> e.getValue() == NationRank.OFFICER)
                    .findFirst()
                    .or(() -> nation.getMembers().entrySet().stream().findFirst())
                    .ifPresent(e -> nation.setRank(e.getKey(), NationRank.LEADER));
        }
        setDirty();
    }

    public void setDiplomacy(UUID nationId, UUID otherNationId, DiplomacyState state) {
        Nation nation = nations.get(nationId);
        if (nation == null) return;
        nation.setDiplomacy(otherNationId, state);
        // Alliances are mutual; wars and neutral are unilateral
        if (state == DiplomacyState.ALLIED) {
            Nation other = nations.get(otherNationId);
            if (other != null) other.setDiplomacy(nationId, DiplomacyState.ALLIED);
        }
        setDirty();
    }

    public void setPlayerRank(UUID nationId, UUID playerUUID, NationRank rank) {
        Nation nation = nations.get(nationId);
        if (nation != null && nation.isMember(playerUUID)) {
            nation.setRank(playerUUID, rank);
            setDirty();
        }
    }

    public void addInvite(UUID nationId, UUID playerUUID) {
        Nation nation = nations.get(nationId);
        if (nation != null) {
            nation.addInvite(playerUUID);
            setDirty();
        }
    }
}
