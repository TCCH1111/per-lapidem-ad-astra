package io.github.izakyl.folkways.plugins.health;

import io.github.izakyl.folkways.core.api.work.Node;
import io.github.izakyl.folkways.core.api.work.NodeSpec;
import io.github.izakyl.folkways.core.api.work.Outcome;
import io.github.izakyl.folkways.core.api.work.Stances;
import io.github.izakyl.folkways.core.api.work.WorkEffort;
import io.github.izakyl.folkways.core.api.work.WorkSite;
import io.github.izakyl.folkways.core.api.work.Workload;
import io.github.izakyl.folkways.core.api.work.Worker;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * PLA: M2 R1 treatment A — a resident dresses their own wound.
 *
 * <p>The node is deliberately short and local: every resident already knows how to reach into their own
 * pack, so self-care needs no journey, no work site beyond where they stand, and no new engine hook. The
 * act itself is the shared {@link HealthContent#dress} function; this only supplies the moment, which is
 * the one thing the owner's decision A settled.
 *
 * <p>A no-op here (the wound healed, or the bandage was used elsewhere while the plan was being made) is
 * reported as done, not as a refusal: there is nothing for the planner to re-plan or explain — the body
 * simply did not need the work after all.
 */
public final class DressAction implements Node {

    /** Long enough to read as an action, short enough that bleeding stops promptly. */
    private static final int DRESS_TICKS = 24;

    private final HealthState state;
    private final UUID who;
    private final NodeSpec spec;

    public DressAction(ServerLevel level, HealthState state, UUID who, BlockPos at) {
        this.state = state;
        this.who = who;
        this.spec = NodeSpec.of(UUID.randomUUID(), HealthContent.ID,
                WorkSite.on(level, who, at), Stances.WHEREVER, Workload.Once.of(DRESS_TICKS))
            .effort(WorkEffort.NONE)
            .doing(io.github.izakyl.folkways.core.api.work.Doings.WORKING)
            .done();
    }

    @Override
    public NodeSpec spec() {
        return spec;
    }

    @Override
    public Outcome commit(ServerLevel level, Worker worker) {
        // Use the worker's live pack at commit time, not a snapshot taken while planning.
        HealthContent.dress(state, who, worker.pack());
        return Outcome.done();
    }
}
