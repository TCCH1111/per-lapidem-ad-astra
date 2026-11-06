package io.github.izakyl.folkways.plugins.health;

import io.github.izakyl.folkways.core.api.Declaring;
import io.github.izakyl.folkways.front.api.Registering;
import net.neoforged.bus.api.IEventBus;

/** PLA: M2 R0-2 — the medical book comes in through one install, like every built-in plugin. */
public final class HealthPlugin {

    private HealthPlugin() {
    }

    public static void install(IEventBus modBus) {
        modBus.addListener(Declaring.class, HealthContent::declare);
        modBus.addListener(Registering.class, HealthContent::enroll);
    }
}
