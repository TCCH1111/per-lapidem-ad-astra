package io.github.izakyl.folkways.plugins.health;

import io.github.izakyl.folkways.base.persist.Reader;
import io.github.izakyl.folkways.core.api.Declaring;
import io.github.izakyl.folkways.core.api.colony.Colony;
import io.github.izakyl.folkways.front.api.Enrollment;
import io.github.izakyl.folkways.front.api.Registering;
import java.util.List;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceLocation;

/**
 * PLA: M2 R0-2 — blood runs on the colony's clock.
 *
 * <p>Only the bookkeeping half of the medical slice: open wounds cost blood every second, treated ones
 * stop, and the number is saved with the colony. Nothing here changes behaviour yet, so a colony with no
 * wounds behaves exactly as before.
 */
public final class HealthContent {

    public static final ResourceLocation ID =
        ResourceLocation.fromNamespaceAndPath("folkways", "health");

    /** How often blood is let, in ticks. */
    public static final int BLEED_TICKS = 20;

    /** How often the book is written down, in ticks. */
    public static final int PERSIST_TICKS = 200;

    private HealthContent() {
    }

    /** The one refusal-shaped idea this plugin has: a body too hurt to be given work. */
    public static final ResourceLocation RESTING = ResourceLocation.fromNamespaceAndPath("folkways", "resting");

    /** Whether this colony's book says the resident is too hurt for planned labour. */
    public static boolean resting(io.github.izakyl.folkways.core.api.colony.Colony colony, UUID who) {
        if (colony == null || who == null) {
            return false;
        }
        return colony.service(ID, HealthState.class)
            .map(state -> state.severe(who))
            .orElse(false);
    }

    public static void enroll(Registering registering) {
        registering.enrollment(ID, new Enrollment(List.of()));
    }

    public static void declare(Declaring declaring) {
        declaring.colony(ID, scope -> {
            Colony colony = scope.colony();
            HealthState state = HealthState.load(Reader.of(colony.kept(ID)));
            scope.share(state);
            scope.tick(server -> {
                long now = server.getTickCount();
                if (now % BLEED_TICKS == 0) {
                    for (UUID who : state.who()) {
                        state.bleed(who, BLEED_TICKS);
                    }
                }
                if (now % PERSIST_TICKS == 0) {
                    colony.keep(ID, state.save());
                }
            });
            scope.closed(closing -> colony.keep(ID, state.save()));
        });
    }
}
