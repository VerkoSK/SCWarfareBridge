package dev.scwarfarebridge.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class BridgeConfig {

    public static final ForgeConfigSpec.DoubleValue MIN_EXPLOSION_POWER;
    public static final ForgeConfigSpec.DoubleValue RESISTANCE_FACTOR;
    public static final ForgeConfigSpec.DoubleValue MAX_DESTROY_CHANCE;
    public static final ForgeConfigSpec SPEC;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("SC Warfare Bridge - Configuration").push("general");

        MIN_EXPLOSION_POWER = builder
                .comment(
                        "Minimum explosion radius required to damage SecurityCraft reinforced blocks.",
                        "Explosions smaller than this (small grenades, pistols) have zero effect.",
                        "Default: 3.0  |  Range: 0.1 - 100.0")
                .defineInRange("minExplosionPower", 3.0, 0.1, 100.0);

        RESISTANCE_FACTOR = builder
                .comment(
                        "Resistance multiplier for reinforced blocks against explosions.",
                        "Effective destruction radius = explosion_radius * this_value.",
                        "Lower = tougher blocks. 1.0 = same as normal blocks. 0.0 = indestructible.",
                        "Default: 0.35  |  Range: 0.0 - 1.0",
                        "",
                        "Examples with default 0.35:",
                        "  Tank shell  (radius  8) -> ~2.8 block effect radius",
                        "  GBU-57      (radius 22) -> ~7.7 block effect radius",
                        "  Nuke        (radius 30) -> ~10.5 block effect radius")
                .defineInRange("resistanceFactor", 0.35, 0.0, 1.0);

        MAX_DESTROY_CHANCE = builder
                .comment(
                        "Maximum chance (0.0-1.0) that a reinforced block at ground zero is destroyed.",
                        "Even at the epicenter, destruction is probabilistic - blocks are tough.",
                        "Default: 0.65  |  Range: 0.0 - 1.0")
                .defineInRange("maxDestroyChance", 0.65, 0.0, 1.0);

        builder.pop();
        SPEC = builder.build();
    }
}
