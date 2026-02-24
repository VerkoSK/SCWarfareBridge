package dev.scwarfarebridge.nation;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum DiplomacyState {
    ALLIED("nation.diplomacy.allied", ChatFormatting.GREEN),
    NEUTRAL("nation.diplomacy.neutral", ChatFormatting.GRAY),
    AT_WAR("nation.diplomacy.at_war", ChatFormatting.RED);

    private final String translationKey;
    private final ChatFormatting color;

    DiplomacyState(String translationKey, ChatFormatting color) {
        this.translationKey = translationKey;
        this.color = color;
    }

    public ChatFormatting getColor() { return color; }

    public Component getDisplayName() {
        return Component.translatable(translationKey).withStyle(color);
    }
}
