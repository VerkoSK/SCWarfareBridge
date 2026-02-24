package dev.scwarfarebridge;

import dev.scwarfarebridge.config.BridgeConfig;
import dev.scwarfarebridge.event.ExplosionHandler;
import dev.scwarfarebridge.item.ModItems;
import dev.scwarfarebridge.network.PacketHandler;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SCWarfareBridge.MOD_ID)
public class SCWarfareBridge {

    public static final String MOD_ID = "scwarfarebridge";

    @SuppressWarnings({"unchecked", "rawtypes"})
    public SCWarfareBridge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, BridgeConfig.SPEC);

        // Register items
        ModItems.register(modBus);

        // Register network packets
        PacketHandler.init();

        // Always register the explosion hook (HE shells, missiles, nukes, ...)
        MinecraftForge.EVENT_BUS.register(ExplosionHandler.class);

        // Add Nation Passport to the Tools & Utilities creative tab
        modBus.addListener(this::buildCreativeTab);

        // Dynamically register the SBW ProjectileHitEvent.HitBlock listener when
        // Superb Warfare is present.
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

    private void buildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.NATION_PASSPORT);
        }
    }
}
