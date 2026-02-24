package dev.scwarfarebridge.api;

import dev.scwarfarebridge.config.BridgeConfig;
import dev.scwarfarebridge.event.ExplosionHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Public API for SC Warfare Bridge.
 *
 * Other mods (e.g. penetrating projectiles, custom weapons) can call these
 * methods to integrate with the bridge's reinforced-block damage system
 * without needing SecurityCraft on the compile classpath.
 *
 * Usage example (inside a projectile tick that bores through blocks):
 * 
 * <pre>
 * if (SCWarfareBridgeAPI.tryBreakReinforced(serverLevel, pos, myWeaponPower)) {
 *     // block was reinforced and got destroyed (or survived) - apply heavy
 *     // slowdown
 *     velocity.scale(0.35);
 * }
 * </pre>
 */
public final class SCWarfareBridgeAPI {

    private SCWarfareBridgeAPI() {
    }

    /**
     * Returns true if the block at the given position is a SecurityCraft
     * reinforced block, regardless of whether SecurityCraft is loaded.
     */
    public static boolean isReinforced(ServerLevel level, BlockPos pos) {
        return ExplosionHandler.isReinforcedBlock(level.getBlockState(pos));
    }

    /**
     * Returns true if the given BlockState is a SecurityCraft reinforced block.
     */
    public static boolean isReinforced(BlockState state) {
        return ExplosionHandler.isReinforcedBlock(state);
    }

    /**
     * Attempts to break a SecurityCraft reinforced block at the given position
     * using direct block force (bypasses explosion resistance).
     *
     * The break succeeds probabilistically based on weaponPower and the
     * configured maxDestroyChance. The block is never guaranteed to break on a
     * single hit, reflecting the reinforced nature of the material.
     *
     * @param level       the server level
     * @param pos         position of the block to attempt breaking
     * @param weaponPower raw weapon power value (e.g. explosionDamage / 10,
     *                    or explosion radius). Used to scale break chance.
     *                    Values around 20-30 give ~60-80% break chance.
     * @return true if the block was reinforced (whether or not it was destroyed)
     */
    public static boolean tryBreakReinforced(ServerLevel level, BlockPos pos, float weaponPower) {
        BlockState state = level.getBlockState(pos);
        if (!ExplosionHandler.isReinforcedBlock(state))
            return false;

        float minPower = BridgeConfig.MIN_EXPLOSION_POWER.get().floatValue();
        if (weaponPower < minPower)
            return true; // it IS reinforced but too weak to break

        float maxChance = BridgeConfig.MAX_DESTROY_CHANCE.get().floatValue();
        // Scale chance: weaponPower=minPower -> 0%, weaponPower=minPower*4 -> maxChance
        float t = Math.min(1.0f, (weaponPower - minPower) / (minPower * 3.0f));
        float chance = maxChance * t;

        if (level.random.nextFloat() < chance) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
        return true;
    }
}
