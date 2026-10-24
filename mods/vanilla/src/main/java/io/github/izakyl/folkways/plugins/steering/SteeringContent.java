package io.github.izakyl.folkways.plugins.steering;

import io.github.izakyl.folkways.base.persist.Reader;
import io.github.izakyl.folkways.core.api.Declaring;
import io.github.izakyl.folkways.core.api.colony.Colony;
import net.minecraft.resources.ResourceLocation;

/** PLA: WP-4 T1 — the steering plugin's colony-wide state and its one saved slot. */
public final class SteeringContent {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("folkways", "steering");

    private SteeringContent() {
    }

    public static void declare(Declaring declaring) {
        declaring.colony(ID, scope -> {
            Colony colony = scope.colony();
            SteeringState state = SteeringState.load(Reader.of(colony.kept(ID)));
            scope.share(state);
            scope.tick(server -> {
                if (state.dirty()) {
                    colony.keep(ID, state.save());
                }
            });
        });
    }
}