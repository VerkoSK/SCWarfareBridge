package dev.scwarfarebridge.event;

import dev.scwarfarebridge.config.BridgeConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class ExplosionHandler {

    /**
     * Returns true if the block is a SecurityCraft reinforced block.
     * Checks by registry namespace + name prefix - no compile-time SC dependency.
     */
    public static boolean isReinforcedBlock(BlockState state) {
        ResourceLocation rl = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return rl != null
                && "securitycraft".equals(rl.getNamespace())
                && rl.getPath().startsWith("reinforced_");
    }

    /**
     * Fired after Minecraft finishes calculating which blocks an explosion affects.
     * SecurityCraft reinforced blocks have Float.MAX_VALUE blast resistance, so
     * they
     * never end up in the vanilla affected-blocks list. We add a secondary pass
     * here
     * that calculates damage to reinforced blocks based on explosion power, scaled
     * by
     * the configurable resistance factor.
     *
     * Destruction probability formula (per block):
     * chance = maxDestroyChance * (1 - distance / effectiveRadius)
     * where effectiveRadius = explosion.radius * resistanceFactor
     *
     * This means:
     * - Blocks right at the epicenter have the highest (but still capped) chance.
     * - Chance drops linearly to 0 at the edge of effectiveRadius.
     * - Explosions weaker than minExplosionPower have no effect at all.
     */
    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (event.getLevel().isClientSide())
            return;
        if (!(event.getLevel() instanceof ServerLevel serverLevel))
            return;

        Explosion explosion = event.getExplosion();
        float power = explosion.radius;

        float minPower = BridgeConfig.MIN_EXPLOSION_POWER.get().floatValue();
        if (power < minPower)
            return;

        Vec3 center = explosion.getPosition();
        float resistanceFactor = BridgeConfig.RESISTANCE_FACTOR.get().floatValue();
        float effectiveRadius = power * resistanceFactor;
        float maxChance = BridgeConfig.MAX_DESTROY_CHANCE.get().floatValue();

        if (effectiveRadius <= 0)
            return;

        int r = (int) Math.ceil(effectiveRadius);
        BlockPos centerPos = BlockPos.containing(center);

        // Collect into a list first - avoids touching the world during iteration
        List<BlockPos> toDestroy = new ArrayList<>();

        for (BlockPos pos : BlockPos.betweenClosed(
                centerPos.offset(-r, -r, -r),
                centerPos.offset(r, r, r))) {

            BlockState state = serverLevel.getBlockState(pos);
            if (!isReinforcedBlock(state))
                continue;

            double distance = Math.sqrt(
                    center.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
            if (distance > effectiveRadius)
                continue;

            // Linear falloff: full maxChance at epicenter, 0 at effectiveRadius edge
            float chance = maxChance * (1.0f - (float) (distance / effectiveRadius));
            if (serverLevel.random.nextFloat() < chance) {
                toDestroy.add(pos.immutable());
            }
        }

        for (BlockPos pos : toDestroy) {
            serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
