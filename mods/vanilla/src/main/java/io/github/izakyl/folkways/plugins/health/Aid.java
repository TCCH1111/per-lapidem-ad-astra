package io.github.izakyl.folkways.plugins.health;

import io.github.izakyl.folkways.FolkwaysMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * PLA: M2 R1 — the dressing a wound is dealt with by.
 *
 * <p>Registered before the rule that uses it, because both candidate rules need the same thing: a wound
 * can only be treated if there is something to treat it with. How a resident gets one, and whether any
 * resident may or only a medic, is the open question the plan sets out.
 */
public final class Aid {

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FolkwaysMod.MOD_ID);

    public static final DeferredItem<Item> BANDAGE = ITEMS.register("bandage",
        () -> new Item(new Item.Properties().stacksTo(16)));

    private Aid() {
    }

    public static ResourceLocation id() {
        return ResourceLocation.fromNamespaceAndPath(FolkwaysMod.MOD_ID, "bandage");
    }

    public static void install(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(BuildCreativeModeTabContentsEvent.class, event -> {
            if (event.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) {
                event.accept(BANDAGE.get());
            }
        });
    }
}
