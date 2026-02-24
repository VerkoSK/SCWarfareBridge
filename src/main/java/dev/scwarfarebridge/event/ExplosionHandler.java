package dev.scwarfarebridge.event;

import dev.scwarfarebridge.config.BridgeConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ExplosionHandler {

    // -----------------------------------------------------------------------
    // Detection
    // -----------------------------------------------------------------------

    /**
     * Returns true if the block is a SecurityCraft reinforced block.
     * Uses registry name lookup — no compile-time SecurityCraft dependency.
     */
    public static boolean isReinforcedBlock(BlockState state) {
        ResourceLocation rl = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return rl != null
                && "securitycraft".equals(rl.getNamespace())
                && rl.getPath().startsWith("reinforced_");
    }

    // -----------------------------------------------------------------------
    // Explosion-based handler (HE shells, missiles, nukes, etc.)
    // -----------------------------------------------------------------------

    /**
     * Runs a secondary damage pass against SecurityCraft reinforced blocks after
     * every explosion. Vanilla + SBW CustomExplosion both fire this event.
     *
     * AP shells that hit a reinforced block fall back to causeExplode() in SBW,
     * so they also end up here — as long as the explosion radius is above
     * minExplosionPower (default 3.0).
     */
    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        Explosion explosion = event.getExplosion();
        float power = explosion.radius;  // made public via accesstransformer.cfg

        float minPower = BridgeConfig.MIN_EXPLOSION_POWER.get().floatValue();
        if (power < minPower) return;

        Vec3 center = explosion.getPosition();
        float effectiveRadius = power * BridgeConfig.RESISTANCE_FACTOR.get().floatValue();
        float maxChance = BridgeConfig.MAX_DESTROY_CHANCE.get().floatValue();
        if (effectiveRadius <= 0) return;

        int r = (int) Math.ceil(effectiveRadius);
        BlockPos centerPos = BlockPos.containing(center);

        List<BlockPos> toDestroy = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(
                centerPos.offset(-r, -r, -r),
                centerPos.offset(r, r, r))) {

            BlockState state = serverLevel.getBlockState(pos);
            if (!isReinforcedBlock(state)) continue;

            double distance = Math.sqrt(
                    center.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
            if (distance > effectiveRadius) continue;

            // Linear falloff: maxChance at epicenter, 0 at edge
            float chance = maxChance * (1.0f - (float) (distance / effectiveRadius));
            if (serverLevel.random.nextFloat() < chance) {
                toDestroy.add(pos.immutable());
            }
        }

        for (BlockPos pos : toDestroy) {
            serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    // -----------------------------------------------------------------------
    // SBW ProjectileHitEvent.HitBlock handler (AP shell direct-penetration path)
    // Registered dynamically in SCWarfareBridge only when SBW is loaded.
    // Uses reflection so we have zero compile-time dependency on SBW.
    // -----------------------------------------------------------------------

    /**
     * Called (via dynamic registration) when a SBW projectile hits a block.
     * The event object is com.atsuishio.superbwarfare.api.event.ProjectileHitEvent.HitBlock.
     *
     * We use this to catch AP shells during their penetration phase — before
     * the final explosion is created — so reinforced blocks feel the hit
     * immediately rather than only through the secondary explosion event.
     */
    @SuppressWarnings("unchecked")
    public static void onSBWProjectileHitBlock(Object event) {
        try {
            // ProjectileHitEvent.HitBlock has a public field `pos`
            Field posField = event.getClass().getField("pos");
            BlockPos pos = (BlockPos) posField.get(event);

            // projectile is in the parent ProjectileHitEvent class
            Field projField = findField(event.getClass(), "projectile");
            if (projField == null) return;
            projField.setAccessible(true);
            Projectile projectile = (Projectile) projField.get(event);

            if (!(projectile.level() instanceof ServerLevel serverLevel)) return;

            BlockState state = serverLevel.getBlockState(pos);
            if (!isReinforcedBlock(state)) return;

            float power = getProjectilePower(projectile);
            handleDirectHit(serverLevel, pos, power);

        } catch (Exception ignored) {
            // Reflection failure or SBW API change — silently skip
        }
    }

    /**
     * Handles a projectile making direct contact with a reinforced block.
     * Destroys the hit block (probabilistically) and applies a small
     * splash to immediately adjacent reinforced neighbours.
     */
    private static void handleDirectHit(ServerLevel level, BlockPos pos, float power) {
        float minPower = BridgeConfig.MIN_EXPLOSION_POWER.get().floatValue();
        if (power < minPower) return;

        float maxChance = BridgeConfig.MAX_DESTROY_CHANCE.get().floatValue();
        float resistanceFactor = BridgeConfig.RESISTANCE_FACTOR.get().floatValue();
        // Scale up for direct hits compared to explosion splash
        float effectiveRadius = Math.max(1.0f, power * resistanceFactor);

        // The directly hit block gets the full max chance
        if (level.random.nextFloat() < maxChance) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }

        // Adjacent reinforced blocks get reduced splash (half max chance, falls off)
        if (effectiveRadius >= 1.5f) {
            Vec3 center = Vec3.atCenterOf(pos);
            int r = (int) Math.ceil(effectiveRadius);
            List<BlockPos> toDestroy = new ArrayList<>();

            for (BlockPos nearby : BlockPos.betweenClosed(
                    pos.offset(-r, -r, -r), pos.offset(r, r, r))) {
                if (nearby.equals(pos)) continue;

                BlockState nearbyState = level.getBlockState(nearby);
                if (!isReinforcedBlock(nearbyState)) continue;

                double dist = Math.sqrt(
                        center.distanceToSqr(nearby.getX() + 0.5, nearby.getY() + 0.5, nearby.getZ() + 0.5));
                if (dist > effectiveRadius) continue;

                float splashChance = maxChance * 0.4f * (1.0f - (float) (dist / effectiveRadius));
                if (level.random.nextFloat() < splashChance) {
                    toDestroy.add(nearby.immutable());
                }
            }

            for (BlockPos p : toDestroy) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Tries to read explosionRadius from a projectile via reflection.
     * Checks the projectile's own class and its superclass chain.
     * Falls back to a reasonable default (10.0) if the field is not found.
     */
    private static float getProjectilePower(Projectile projectile) {
        Class<?> cls = projectile.getClass();
        while (cls != null && cls != Object.class) {
            for (Field field : cls.getDeclaredFields()) {
                String name = field.getName();
                if (name.equals("explosionRadius") || name.equals("explosionPower")
                        || name.equalsIgnoreCase("explosionradius")) {
                    try {
                        field.setAccessible(true);
                        Object val = field.get(projectile);
                        if (val instanceof Float f && f > 0) return f;
                        if (val instanceof Double d && d > 0) return d.floatValue();
                    } catch (Exception ignored) {}
                }
            }
            cls = cls.getSuperclass();
        }
        // Unknown projectile — treat as moderate weapon
        return 10.0f;
    }

    /** Walks the class hierarchy to find a field by name. */
    private static Field findField(Class<?> cls, String name) {
        while (cls != null && cls != Object.class) {
            try {
                return cls.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                cls = cls.getSuperclass();
            }
        }
        return null;
    }
}
