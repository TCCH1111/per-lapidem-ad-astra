package io.github.izakyl.folkways.plugins.health;

import io.github.izakyl.folkways.core.api.colony.Colony;
import io.github.izakyl.folkways.core.api.colony.ColonyView;
import io.github.izakyl.folkways.front.api.Facing;
import io.github.izakyl.folkways.front.api.notice.Line;
import java.util.List;
import java.util.UUID;

/**
 * PLA: M2 R0-4 — the card says when a resident is too hurt to be given work, and nothing otherwise.
 */
public final class HealthFacing implements Facing {

    private final Colony colony;

    public HealthFacing(Colony colony) {
        this.colony = colony;
    }

    @Override
    public List<Line> lookLines(UUID resident, ColonyView view) {
        return linesFor(HealthContent.resting(colony, resident));
    }

    /** The rule, kept apart from the colony so it can be tested without one. */
    static List<Line> linesFor(boolean resting) {
        return resting
            ? List.of(Line.of(Injury.RESTING), Line.of(Injury.RESTING_ADVICE))
            : List.of();
    }
}
