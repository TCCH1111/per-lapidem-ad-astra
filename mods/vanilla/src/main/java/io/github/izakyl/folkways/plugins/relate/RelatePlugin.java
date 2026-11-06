package io.github.izakyl.folkways.plugins.relate;

import io.github.izakyl.folkways.core.api.Declaring;
import io.github.izakyl.folkways.front.api.Registering;
import net.neoforged.bus.api.IEventBus;

/** PLA: M2 R1-2 — who works beside whom comes in through one install, like every built-in plugin. */
public final class RelatePlugin {

    private RelatePlugin() {
    }

    public static void install(IEventBus modBus) {
        modBus.addListener(Declaring.class, RelationContent::declare);
        modBus.addListener(Registering.class, RelationContent::enroll);
    }
}