package io.github.izakyl.folkways.plugins.threat;

import io.github.izakyl.folkways.base.persist.Reader;
import io.github.izakyl.folkways.core.api.Declaring;
import io.github.izakyl.folkways.core.api.colony.Colony;
import io.github.izakyl.folkways.front.api.Enrollment;
import io.github.izakyl.folkways.front.api.Registering;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** PLA: M2 T0 — the colony hears about a danger before it arrives. */
public final class ThreatContent {

    public static final ResourceLocation ID =
        ResourceLocation.fromNamespaceAndPath("folkways", "threat");

    /** How often the countdown is read. */
    public static final int TICK_TICKS = 100;

    /** How often the threat book is written down. */
    public static final int PERSIST_TICKS = 200;

    private ThreatContent() {
    }

    /** The language key a kind of danger is announced under. */
    public static String kindKey(String kind) {
        return "folkways.threat.kind." + (kind == null || kind.isBlank() ? "raiders" : kind);
    }

    /**
     * PLA: M2 T3 — writes the resolution's consequences into the colony's health book. One wound per
     * point of loss, spread over the residents and each capped by the resolution, so a defeated colony
     * is hurt and has to recover but nobody dies from a single raid. Reuses the medical book rather than
     * inventing a second injury model.
     */
    static void apply(Colony colony, ThreatState.Resolution result, long now) {
        if (result == null || result.loss() <= 0 || result.severity() <= 0) {
            return;
        }
        io.github.izakyl.folkways.plugins.health.HealthState health = colony.service(
                io.github.izakyl.folkways.plugins.health.HealthContent.ID,
                io.github.izakyl.folkways.plugins.health.HealthState.class).orElse(null);
        if (health == null) {
            return;
        }
        java.util.List<io.github.izakyl.folkways.core.api.resident.Resident> people =
            new java.util.ArrayList<>(colony.residents());
        if (people.isEmpty()) {
            return;
        }
        for (int i = 0; i < result.loss(); i++) {
            io.github.izakyl.folkways.plugins.health.HealthContent.wound(health,
                people.get(i % people.size()).id(), result.severity(), "raid wounds", now);
        }
    }

    public static void enroll(Registering registering) {
        registering.enrollment(ID, new Enrollment(List.of()));
    }

    public static void declare(Declaring declaring) {
        declaring.colony(ID, scope -> {
            Colony colony = scope.colony();
            ThreatState state = ThreatState.load(Reader.of(colony.kept(ID)));
            scope.share(state);
            scope.tick(server -> {
                long now = server.getTickCount();
                if (now % TICK_TICKS == 0) {
                    for (ThreatState.Threat arrived : state.advance(now)) {
                        // It is here. The owner's A/B/A rule decides the outcome deterministically:
                        // readiness is a count of what the colony can point to, loss is the shortfall,
                        // and every wound it causes is capped below "severe" by the resolution itself.
                        server.getPlayerList().broadcastSystemMessage(Component.translatable(
                            "folkways.threat.at_the_gates", Component.translatable(kindKey(arrived.kind()))),
                            false);
                        int militia = colony.residents().size();
                        int readiness = ThreatState.readiness(militia, 0, 0, 0);
                        ThreatState.Resolution result = ThreatState.resolve(arrived.strength(), readiness);
                        apply(colony, result, now);
                        server.getPlayerList().broadcastSystemMessage(Component.translatable(
                            result.held() ? "folkways.threat.held" : "folkways.threat.hurt",
                            Component.translatable(kindKey(arrived.kind())), result.loss()), false);
                    }
                    if (state.all().stream().anyMatch(t -> t.stage() == ThreatState.Stage.AT_THE_GATES)) {
                        state.settle();
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
