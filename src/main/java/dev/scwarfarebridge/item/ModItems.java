package dev.scwarfarebridge.item;

import dev.scwarfarebridge.SCWarfareBridge;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, SCWarfareBridge.MOD_ID);

    public static final RegistryObject<Item> NATION_PASSPORT =
            ITEMS.register("nation_passport", NationPassportItem::new);

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
