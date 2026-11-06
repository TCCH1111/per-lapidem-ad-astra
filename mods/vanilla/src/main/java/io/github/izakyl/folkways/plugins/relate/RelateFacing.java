package io.github.izakyl.folkways.plugins.relate;

import io.github.izakyl.folkways.core.api.colony.Colony;
import io.github.izakyl.folkways.core.api.colony.ColonyView;
import io.github.izakyl.folkways.front.api.Facing;
import io.github.izakyl.folkways.front.api.notice.Line;
import java.util.List;
import java.util.UUID;

/**
 * PLA: M2 R1-4 — the resident card says when a friend is working beside them.
 *
 * <p>Only when it is true: a resident working alone is not interesting, and a card that always carries
 * a line teaches the player to stop reading it.
 */
public final class RelateFacing implements Facing {

    private final Colony colony;

    public RelateFacing(Colony colony) {
        this.colony = colony;
    }

    @Override
    public List<Line> lookLines(UUID resident, ColonyView view) {
        return linesFor(RelationContent.amongFriends(colony, resident));
    }

    /** The rule, kept apart from the colony so it can be tested without one. */
    static List<Line> linesFor(boolean amongFriends) {
        return amongFriends ? List.of(Line.of(Companion.TOGETHER)) : List.of();
    }
}
