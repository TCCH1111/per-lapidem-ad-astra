package io.github.izakyl.folkways.plugins.hours;

import io.github.izakyl.folkways.base.persist.Reader;
import io.github.izakyl.folkways.core.api.Declaring;
import io.github.izakyl.folkways.core.api.colony.Colony;
import io.github.izakyl.folkways.core.api.work.Standing;
import io.github.izakyl.folkways.core.api.work.Standings;
import io.github.izakyl.folkways.front.api.Delegation;
import io.github.izakyl.folkways.front.api.Enrollment;
import io.github.izakyl.folkways.front.api.Facing;
import io.github.izakyl.folkways.front.api.Fronts;
import io.github.izakyl.folkways.front.api.Registering;
import io.github.izakyl.folkways.front.api.Schema;
import io.github.izakyl.folkways.front.api.Settings;
import io.github.izakyl.folkways.front.api.panel.PageKind;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * PLA: WP-5 T1 — the hours plugin: one saved timetable per colony and the clock it reads.
 *
 * <p>The clock is sampled on the server tick and the veto answers from that sample, so the schedule's
 * question stays a pure read. Off by default: until the player turns it on, {@link HoursState#working}
 * is always true and the provider allows every cell.
 */
public final class HoursContent {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("folkways", "hours");

    public static final String ENABLED = "enabled";

    public static final String PRESET = "preset";

    static final PageKind PAGE = PageKind.boarded(ID, "folkways.page.hours",
        ResourceLocation.withDefaultNamespace("clock"));

    /** PLA: WP-5 T4 — the two knobs, in the four settings the base already knows how to draw. */
    static final Schema SETTINGS = new Schema(List.of(
        new Schema.Setting.Flag(ENABLED, "folkways.settings.hours.enabled",
            ResourceLocation.withDefaultNamespace("clock"), false),
        new Schema.Setting.Choice(PRESET, "folkways.settings.hours.preset",
            ResourceLocation.withDefaultNamespace("lantern"),
            List.of(ResourceLocation.withDefaultNamespace("clock"),
                ResourceLocation.withDefaultNamespace("daylight_detector"),
                ResourceLocation.withDefaultNamespace("lantern"),
                ResourceLocation.withDefaultNamespace("iron_sword")),
            ResourceLocation.withDefaultNamespace("clock"))));

    /** One timetable per colony, so a second colony cannot borrow the first one's clock. */
    private static final java.util.Map<java.util.UUID, HoursState> STATES = new java.util.concurrent.ConcurrentHashMap<>();

    private HoursContent() {
    }

    /** The engine's view of one timetable: a colony off its hours refuses every cell. */
    public static Standing standingOf(HoursState state) {
        return (resident, cell) -> state == null || state.working()
            ? java.util.Optional.empty()
            : java.util.Optional.of(HoursRefusal.RESTING);
    }

    public static void enroll(Registering registering) {
        registering.facing(ID, colony -> colony.service(ID, HoursState.class)
            .map(state -> (Facing) new HoursFacing(state)));
        registering.enrollment(ID, new Enrollment(List.of(), SETTINGS, List.of(PAGE)));
    }

    public static void declare(Declaring declaring) {
        declaring.colony(ID, scope -> {
            Colony colony = scope.colony();
            HoursState state = HoursState.load(Reader.of(colony.kept(ID)));
            scope.share(state);
            STATES.put(colony.id(), state);
            Standings.provide(ID, id -> standingOf(STATES.get(id)), HoursRefusal.RESTING,
                io.github.izakyl.folkways.core.engine.plan.Unassigned.Reason.HOURS_RESTING);
            // PLA: WP-8 — a colony that closes takes its timetable, and its veto, with it.
            scope.closed(closing -> {
                STATES.remove(colony.id());
                if (STATES.isEmpty()) {
                    Standings.remove(ID);
                }
            });
            scope.tick(server -> {
                state.clock(server.overworld().getDayTime());
                // PLA: WP-5 T4 — the page owns the switch and the pick; the tick copies them in.
                Settings chosen = Fronts.of(colony).settings(ID);
                state.enabled(chosen.flag(ENABLED));
                state.preset(HoursPreset.named(chosen.choice(PRESET).getPath()));
                if (server.getTickCount() % 200 == 0) {
                    colony.keep(ID, state.save());
                }
            });
        });
    }
}