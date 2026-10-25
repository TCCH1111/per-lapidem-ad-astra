package io.github.izakyl.folkways.plugins.hours;

import io.github.izakyl.folkways.core.api.Declaring;
import io.github.izakyl.folkways.front.api.Registering;
import net.neoforged.bus.api.IEventBus;

/** PLA: WP-5 T1 — the hours plugin comes in through one install, like every built-in plugin. */
public final class HoursPlugin {

    private HoursPlugin() {
    }

    public static void install(IEventBus modBus) {
        modBus.addListener(Declaring.class, HoursContent::declare);
        modBus.addListener(Registering.class, HoursContent::enroll);
    }
}