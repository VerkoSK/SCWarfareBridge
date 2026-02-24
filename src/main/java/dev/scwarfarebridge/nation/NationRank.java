package dev.scwarfarebridge.nation;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum NationRank {
    LEADER("nation.rank.leader", ChatFormatting.GOLD),
    OFFICER("nation.rank.officer", ChatFormatting.YELLOW),
    RECRUIT("nation.rank.recruit", ChatFormatting.WHITE);

    private final String translationKey;
    private final ChatFormatting color;

    NationRank(String translationKey, ChatFormatting color) {
        this.translationKey = translationKey;
        this.color = color;
    }

    public ChatFormatting getColor() { return color; }

    public Component getDisplayName() {
        return Component.translatable(translationKey).withStyle(color);
    }

    /** Returns true if this rank has authority over the given target rank */
    public boolean canManage(NationRank target) {
        return this.ordinal() < target.ordinal();
    }
}
