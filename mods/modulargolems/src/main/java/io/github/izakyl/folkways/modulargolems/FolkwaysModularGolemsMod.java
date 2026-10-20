package io.github.izakyl.folkways.modulargolems;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(FolkwaysModularGolemsMod.ID)
public final class FolkwaysModularGolemsMod {

    public static final String ID = "plaa_modulargolems"; // PLA: ADR-023 modid

    public FolkwaysModularGolemsMod(IEventBus modBus) {
        GolemPlugin.install(modBus);
    }
}
