package io.github.izakyl.folkways.plugins.hours;

import io.github.izakyl.folkways.base.persist.Reader;
import io.github.izakyl.folkways.core.api.Declaring;
import io.github.izakyl.folkways.core.api.colony.Colony;
import io.github.izakyl.folkways.core.api.work.Standing;
import io.github.izakyl.folkways.core.api.work.Standings;
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

    private HoursContent() {
    }

    public static void declare(Declaring declaring) {
        declaring.colony(ID, scope -> {
            Colony colony = scope.colony();
            HoursState state = HoursState.load(Reader.of(colony.kept(ID)));
            scope.share(state);
            Standings.provide(ID, id -> id.equals(colony.id())
                ? (resident, cell) -> state.working()
                : Standing.ALL);
            scope.tick(server -> {
                state.clock(server.overworld().getDayTime());
                if (server.getTickCount() % 200 == 0) {
                    colony.keep(ID, state.save());
                }
            });
        });
    }
}