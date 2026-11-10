package io.github.izakyl.folkways.plugins.threat;

import io.github.izakyl.folkways.core.api.Declaring;
import io.github.izakyl.folkways.front.api.Registering;
import net.neoforged.bus.api.IEventBus;

/** PLA: M2 T0 — danger that is announced before it arrives, in one install like every plugin. */
public final class ThreatPlugin {

    private ThreatPlugin() {
    }

    public static void install(IEventBus modBus) {
        modBus.addListener(Declaring.class, ThreatContent::declare);
        modBus.addListener(Registering.class, ThreatContent::enroll);
    }
}
