package io.github.izakyl.folkways.plugins.steering;

import io.github.izakyl.folkways.base.persist.Reader;
import io.github.izakyl.folkways.core.api.Declaring;
import io.github.izakyl.folkways.core.api.colony.Colony;
import io.github.izakyl.folkways.core.api.work.Veto;
import io.github.izakyl.folkways.core.api.work.Vetoes;
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

    /** colony → resident → kind, refreshed once a second so the veto can name who is asking. */
    private static final Map<UUID, Map<UUID, ResourceLocation>> KINDS = new ConcurrentHashMap<>();

    private SteeringContent() {
    }

    /**
     * The engine's view of this state, for callers that cannot name the resident.
     *
     * <p>Only forbid zones with an empty subject list can answer without knowing who is asking.
     */
    public static Veto standingOf(SteeringState state) {
        return standingOf(state, null);
    }

    /**
     * PLA: M2 D20 — the engine's view of this state, now able to say who is asking.
     *
     * <p>The veto is handed a resident id and a cell and nothing else, so the colony keeps a
     * resident-to-kind map and this closure reads it. When the subject is unknown the old rule stands
     * (only an open forbid zone refuses), because refusing everybody on a guess would ground a whole
     * colony the first time the map was stale.
     *
     * <p>The inside count for limit zones is passed as zero: counting who stands inside on every cell
     * question would scan the colony per query. Limits therefore still act through the diagnosis layer
     * until there is a cheap counter.
     */
    public static Veto standingOf(SteeringState state,
            java.util.function.Function<UUID, ResourceLocation> kinds) {
        return (resident, cell) -> {
            if (state == null) {
                return java.util.Optional.empty();
            }
            ResourceLocation subject = kinds == null ? null : kinds.apply(resident);
            if (subject == null) {
                return state.forbids(cell)
                    ? java.util.Optional.of(SteeringRefusal.FORBIDDEN)
                    : java.util.Optional.empty();
            }
            return state.refusal(cell, subject, 0).map(SteeringRefusal::of);
        };
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
            Map<UUID, ResourceLocation> kinds = KINDS.computeIfAbsent(colony.id(),
                id -> new ConcurrentHashMap<>());
            Vetoes.provide(ID, id -> {
                Map<UUID, ResourceLocation> who = KINDS.get(id);
                return standingOf(STATES.get(id), who == null ? null : who::get);
            }, SteeringRefusal.FORBIDDEN,
                io.github.izakyl.folkways.core.engine.plan.Unassigned.Reason.ZONE_FORBIDDEN);
            scope.closed(closing -> {
                STATES.remove(colony.id());
                KINDS.remove(colony.id());
            });
            scope.tick(server -> {
                if (state.dirty()) {
                    colony.keep(ID, state.save());
                }
                if (server.getTickCount() % 100 == 0) {
                    kinds.clear();
                    for (io.github.izakyl.folkways.core.api.resident.Resident one : colony.residents()) {
                        kinds.put(one.id(), one.kind());
                    }
                }
            });
        });
    }
}