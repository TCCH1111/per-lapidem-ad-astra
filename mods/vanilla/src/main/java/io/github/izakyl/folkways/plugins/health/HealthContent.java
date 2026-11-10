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

    /** One game day, the rhythm infection keeps. */
    public static final int DAY_TICKS = 24_000;

    private HealthContent() {
    }

    /** The one refusal-shaped idea this plugin has: a body too hurt to be given work. */
    public static final ResourceLocation RESTING = ResourceLocation.fromNamespaceAndPath("folkways", "resting");

    /**
     * PLA: M2 R1-2 — the colony's dead are gathered once a sweep.
     *
     * <p>A resident dies when their blood is gone or an infection has run its course; the obituary is
     * written before the body goes, because the name is only readable while the body exists. The record
     * lands in the health book, which is the colony's log for now: there is no separate chronicle
     * service yet, and inventing one to hold a single line would be more machinery than meaning.
     *
     * @return how many residents died in this sweep
     */
    public static int sweep(Colony colony, net.minecraft.server.MinecraftServer server, long now) {
        HealthState state = colony.service(ID, HealthState.class).orElse(null);
        if (state == null) {
            return 0;
        }
        int died = 0;
        for (UUID who : java.util.List.copyOf(state.who())) {
            java.util.Optional<String> cause = state.causeOfDeath(who);
            if (cause.isEmpty()) {
                continue;
            }
            String name = "someone";
            boolean killed = false;
            for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
                net.minecraft.world.entity.Entity entity = level.getEntity(who);
                if (entity instanceof net.minecraft.world.entity.Mob mob) {
                    name = mob.getName().getString();
                    mob.kill();
                    killed = true;
                    break;
                }
            }
            state.record(new HealthState.Death(who, name, cause.get(), now, false));
            state.forget(who);
            died++;
            if (!killed && name.equals("someone")) {
                // The body was already gone; the record still stands, because a death must leave a trace.
                continue;
            }
        }
        return died;
    }

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
        registering.facing(ID, colony -> java.util.Optional.of(new HealthFacing(colony)));
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
                if (now % 20 == 0) {
                    sweep(colony, server, now);
                }
                if (now % DAY_TICKS == 0) {
                    // PLA: M2 R1-1 — a day goes by: open wounds may turn, and what has turned deepens.
                    for (UUID who : state.who()) {
                        for (HealthState.Wound one : state.wounds(who)) {
                            if (one.bleeds() && state.infection(who, one.part()) == 0) {
                                state.maybeInfect(who, one.part(), server.overworld().random.nextDouble());
                            }
                        }
                        state.progressInfection(who, 1);
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
