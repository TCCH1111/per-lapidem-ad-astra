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
                        // It is here. What happens next belongs to the resolution rule, which is not
                        // agreed yet, so the colony says the true thing and stops there.
                        server.getPlayerList().broadcastSystemMessage(Component.translatable(
                            "folkways.threat.at_the_gates", Component.translatable(kindKey(arrived.kind()))),
                            false);
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
