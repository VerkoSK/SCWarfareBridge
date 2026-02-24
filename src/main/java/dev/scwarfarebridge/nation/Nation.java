package dev.scwarfarebridge.nation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.DyeColor;

import java.util.*;

public class Nation {
    private final UUID id;
    private String name;
    private String description;
    private DyeColor color;
    private final Map<UUID, NationRank> members;
    private final Map<UUID, DiplomacyState> diplomacy;
    private final Set<UUID> pendingInvites;

    public Nation(UUID id, String name, DyeColor color) {
        this.id = id;
        this.name = name;
        this.description = "";
        this.color = color;
        this.members = new LinkedHashMap<>();
        this.diplomacy = new HashMap<>();
        this.pendingInvites = new HashSet<>();
    }

    // -----------------------------------------------------------------------
    // NBT serialization
    // -----------------------------------------------------------------------

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        tag.putString("name", name);
        tag.putString("description", description);
        tag.putString("color", color.getName());

        CompoundTag membersTag = new CompoundTag();
        for (Map.Entry<UUID, NationRank> e : members.entrySet()) {
            membersTag.putString(e.getKey().toString(), e.getValue().name());
        }
        tag.put("members", membersTag);

        CompoundTag diplomacyTag = new CompoundTag();
        for (Map.Entry<UUID, DiplomacyState> e : diplomacy.entrySet()) {
            diplomacyTag.putString(e.getKey().toString(), e.getValue().name());
        }
        tag.put("diplomacy", diplomacyTag);

        ListTag invitesTag = new ListTag();
        for (UUID uuid : pendingInvites) {
            invitesTag.add(StringTag.valueOf(uuid.toString()));
        }
        tag.put("pendingInvites", invitesTag);

        return tag;
    }

    public static Nation load(CompoundTag tag) {
        UUID id = tag.getUUID("id");
        String name = tag.getString("name");
        DyeColor color = DyeColor.byName(tag.getString("color"), DyeColor.WHITE);
        Nation nation = new Nation(id, name, color);
        nation.description = tag.getString("description");

        CompoundTag membersTag = tag.getCompound("members");
        for (String key : membersTag.getAllKeys()) {
            try {
                nation.members.put(UUID.fromString(key), NationRank.valueOf(membersTag.getString(key)));
            } catch (Exception ignored) {}
        }

        CompoundTag diplomacyTag = tag.getCompound("diplomacy");
        for (String key : diplomacyTag.getAllKeys()) {
            try {
                nation.diplomacy.put(UUID.fromString(key), DiplomacyState.valueOf(diplomacyTag.getString(key)));
            } catch (Exception ignored) {}
        }

        ListTag invitesTag = tag.getList("pendingInvites", Tag.TAG_STRING);
        for (int i = 0; i < invitesTag.size(); i++) {
            try { nation.pendingInvites.add(UUID.fromString(invitesTag.getString(i))); }
            catch (Exception ignored) {}
        }

        return nation;
    }

    // -----------------------------------------------------------------------
    // Getters / setters
    // -----------------------------------------------------------------------

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public DyeColor getColor() { return color; }
    public void setColor(DyeColor color) { this.color = color; }
    public Map<UUID, NationRank> getMembers() { return Collections.unmodifiableMap(members); }
    public int getMemberCount() { return members.size(); }

    public Optional<UUID> getLeader() {
        return members.entrySet().stream()
                .filter(e -> e.getValue() == NationRank.LEADER)
                .map(Map.Entry::getKey)
                .findFirst();
    }

    public boolean isMember(UUID uuid) { return members.containsKey(uuid); }
    public NationRank getRank(UUID uuid) { return members.getOrDefault(uuid, NationRank.RECRUIT); }
    public boolean hasInvite(UUID uuid) { return pendingInvites.contains(uuid); }
    public Set<UUID> getPendingInvites() { return Collections.unmodifiableSet(pendingInvites); }

    public void addMember(UUID uuid, NationRank rank) {
        members.put(uuid, rank);
        pendingInvites.remove(uuid);
    }

    public void removeMember(UUID uuid) { members.remove(uuid); }
    public void setRank(UUID uuid, NationRank rank) { members.put(uuid, rank); }

    public void addInvite(UUID uuid) { pendingInvites.add(uuid); }
    public void removeInvite(UUID uuid) { pendingInvites.remove(uuid); }

    public DiplomacyState getDiplomacyWith(UUID otherNationId) {
        return diplomacy.getOrDefault(otherNationId, DiplomacyState.NEUTRAL);
    }

    public Map<UUID, DiplomacyState> getDiplomacyMap() { return Collections.unmodifiableMap(diplomacy); }

    public void setDiplomacy(UUID otherNationId, DiplomacyState state) {
        if (state == DiplomacyState.NEUTRAL) {
            diplomacy.remove(otherNationId);
        } else {
            diplomacy.put(otherNationId, state);
        }
    }
}
