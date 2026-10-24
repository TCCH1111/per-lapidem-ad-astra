package io.github.izakyl.folkways.plugins.steering;

import io.github.izakyl.folkways.core.api.Declaring;
import net.neoforged.bus.api.IEventBus;

/** PLA: WP-4 T1 — the steering plugin comes in through one install, like every built-in plugin. */
public final class SteeringPlugin {

    private SteeringPlugin() {
    }

    public static void install(IEventBus modBus) {
        modBus.addListener(Declaring.class, SteeringContent::declare);
        // The page that paints zones arrives with T6; it will add a Registering listener here.
    }
}