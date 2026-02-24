package dev.scwarfarebridge;

import dev.scwarfarebridge.config.BridgeConfig;
import dev.scwarfarebridge.event.ExplosionHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(SCWarfareBridge.MOD_ID)
public class SCWarfareBridge {

    public static final String MOD_ID = "scwarfarebridge";

    @SuppressWarnings({"unchecked", "rawtypes"})
    public SCWarfareBridge() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, BridgeConfig.SPEC);

        // Always register the explosion hook (HE shells, missiles, nukes, ...)
        MinecraftForge.EVENT_BUS.register(ExplosionHandler.class);

        // Dynamically register the SBW ProjectileHitEvent.HitBlock listener when
        // Superb Warfare is present. This catches AP shells the moment they contact
        // a reinforced block — before (or instead of) their small explosion fires.
        // Zero compile-time dependency on SBW: we load the class by name at runtime.
        if (ModList.get().isLoaded("superbwarfare")) {
            try {
                Class hitBlockClass = Class.forName(
                        "com.atsuishio.superbwarfare.api.event.ProjectileHitEvent$HitBlock");
                MinecraftForge.EVENT_BUS.addListener(
                        EventPriority.NORMAL, false, hitBlockClass,
                        event -> ExplosionHandler.onSBWProjectileHitBlock(event));
            } catch (ClassNotFoundException ignored) {
                // Older SBW build without this event — explosion hook still covers it
            }
        }
    }
}
