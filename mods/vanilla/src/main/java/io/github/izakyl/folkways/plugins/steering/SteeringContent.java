package io.github.izakyl.folkways.plugins.steering;

import io.github.izakyl.folkways.base.persist.Reader;
import io.github.izakyl.folkways.core.api.Declaring;
import io.github.izakyl.folkways.core.api.colony.Colony;
import io.github.izakyl.folkways.core.api.work.Standing;
import io.github.izakyl.folkways.core.api.work.Standings;
import io.github.izakyl.folkways.front.api.Delegation;
import io.github.izakyl.folkways.front.api.Enrollment;
import io.github.izakyl.folkways.front.api.Facing;
import io.github.izakyl.folkways.front.api.Registering;
import io.github.izakyl.folkways.front.api.Schema;
import io.github.izakyl.folkways.front.api.Shape;
import io.github.izakyl.folkways.front.api.panel.PageKind;
import io.github.izakyl.folkways.plugins.FolkwaysVanillaConfig;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;

/** PLA: WP-4 T1 — the steering plugin's colony-wide state and its one saved slot. */
public final class SteeringContent {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("folkways", "steering");

    public static final ResourceLocation ZONE =
        ResourceLocation.fromNamespaceAndPath("folkways", "steering_zone");

    public static final String REMOVE = "steering.remove:";

    static final PageKind PAGE = PageKind.boarded(ID, "folkways.page.steering",
        ResourceLocation.withDefaultNamespace("barrier"));

    private static final Map<UUID, SteeringState> STATES = new ConcurrentHashMap<>();

    private SteeringContent() {
    }

    /** The engine's view of this state: a cell some forbid zone covers is not for standing on. */
    public static Standing standingOf(SteeringState state) {
        return (resident, cell) -> state == null || !state.forbids(cell);
    }

    /**
     * PLA: WP-4 T7 — the one place the engine's reason becomes words. A board row and any future
     * diagnosis page read the same refusal, so they cannot drift apart.
     */
    public static Optional<io.github.izakyl.folkways.core.api.terms.RefusalKind> why(
            io.github.izakyl.folkways.core.engine.plan.Unassigned.Reason reason) {
        return reason == io.github.izakyl.folkways.core.engine.plan.Unassigned.Reason.ZONE_FORBIDDEN
            ? Optional.of(SteeringRefusal.FORBIDDEN)
            : Optional.empty();
    }

    public static void enroll(Registering registering) {
        registering.facing(ID, colony -> colony.service(ID, SteeringState.class)
            .map(state -> (Facing) new SteeringFacing(state)));
        registering.enrollment(ID, new Enrollment(
            List.of(new Delegation(ZONE, new Shape.Volume(FolkwaysVanillaConfig.maxFarmCells()),
                ResourceLocation.withDefaultNamespace("barrier"))),
            Schema.none(), List.of(PAGE)));
    }

    public static void declare(Declaring declaring) {
        declaring.colony(ID, scope -> {
            Colony colony = scope.colony();
            SteeringState state = SteeringState.load(Reader.of(colony.kept(ID)));
            scope.share(state);
            // PLA: WP-4 T4b — hand this colony's zones to the engine's schedule.
            STATES.put(colony.id(), state);
            Standings.provide(ID, id -> standingOf(STATES.get(id)));
            scope.closed(closing -> STATES.remove(colony.id()));
            scope.tick(server -> {
                if (state.dirty()) {
                    colony.keep(ID, state.save());
                }
            });
        });
    }
}