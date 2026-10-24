package io.github.izakyl.folkways.plugins.steering;

import io.github.izakyl.folkways.base.persist.Reader;
import io.github.izakyl.folkways.core.api.Declaring;
import io.github.izakyl.folkways.core.api.colony.Colony;
import io.github.izakyl.folkways.core.api.work.Standing;
import io.github.izakyl.folkways.core.api.work.Standings;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;

/** PLA: WP-4 T1 — the steering plugin's colony-wide state and its one saved slot. */
public final class SteeringContent {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("folkways", "steering");

    private static final Map<UUID, SteeringState> STATES = new ConcurrentHashMap<>();

    private SteeringContent() {
    }

    /** The engine's view of this state: a cell some forbid zone covers is not for standing on. */
    public static Standing standingOf(SteeringState state) {
        return (resident, cell) -> state == null || !state.forbids(cell);
    }

    public static void declare(Declaring declaring) {
        declaring.colony(ID, scope -> {
            Colony colony = scope.colony();
            SteeringState state = SteeringState.load(Reader.of(colony.kept(ID)));
            scope.share(state);
            // PLA: WP-4 T4b — hand this colony's zones to the engine's schedule.
            STATES.put(colony.id(), state);
            Standings.provide(id -> standingOf(STATES.get(id)));
            scope.closed(closing -> STATES.remove(colony.id()));
            scope.tick(server -> {
                if (state.dirty()) {
                    colony.keep(ID, state.save());
                }
            });
        });
    }
}