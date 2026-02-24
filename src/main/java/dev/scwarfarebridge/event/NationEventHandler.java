package dev.scwarfarebridge.event;

import dev.scwarfarebridge.SCWarfareBridge;
import dev.scwarfarebridge.client.NationScreen;
import dev.scwarfarebridge.network.S2CNationsDataPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class NationEventHandler {

    /** Key binding: press N to open the Nation screen */
    public static KeyMapping OPEN_NATION_KEY;

    /** Register the key mapping on the mod event bus (client only) */
    @Mod.EventBusSubscriber(modid = SCWarfareBridge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            OPEN_NATION_KEY = new KeyMapping(
                    "key.scwarfarebridge.open_nation",
                    KeyConflictContext.IN_GAME,
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_N,
                    "key.categories.scwarfarebridge"
            );
            event.register(OPEN_NATION_KEY);
        }
    }

    /** Forge event bus – handle key press in-game (client only) */
    @Mod.EventBusSubscriber(modid = SCWarfareBridge.MOD_ID, value = Dist.CLIENT)
    public static class ClientForgeEvents {
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (OPEN_NATION_KEY != null && OPEN_NATION_KEY.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.screen == null) {
                    mc.setScreen(new NationScreen());
                }
            }
        }
    }

    /** On player login to server: push current nation data to them */
    @Mod.EventBusSubscriber(modid = SCWarfareBridge.MOD_ID)
    public static class ServerForgeEvents {
        @SubscribeEvent
        public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                S2CNationsDataPacket.sendToPlayer(player);
            }
        }
    }
}
