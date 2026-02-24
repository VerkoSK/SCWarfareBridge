package dev.scwarfarebridge;

import dev.scwarfarebridge.config.BridgeConfig;
import dev.scwarfarebridge.event.ExplosionHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(SCWarfareBridge.MOD_ID)
public class SCWarfareBridge {

    public static final String MOD_ID = "scwarfarebridge";

    public SCWarfareBridge() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, BridgeConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(ExplosionHandler.class);
    }
}
