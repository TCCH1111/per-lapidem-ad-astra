package io.github.izakyl.folkways.plugins.mood;

import io.github.izakyl.folkways.core.api.colony.Colony;
import io.github.izakyl.folkways.core.api.colony.ColonyView;
import io.github.izakyl.folkways.front.api.Facing;
import io.github.izakyl.folkways.front.api.notice.Line;
import io.github.izakyl.folkways.plugins.person.living.PresenceViews;
import java.util.List;
import java.util.UUID;

/**
 * PLA: M2 N6 鈥?the resident card says how the resident is doing, in the same place the base puts
 * everything else about them. The card is assembled from every enrolled Facing's lookLines, so this
 * needs no change to the resident page itself.
 *
 * <p>Only a resident who is not content speaks up: a content one would spend a line saying nothing,
 * and the card is already long.
 */
public final class MoodFacing implements Facing {

    private final Colony colony;

    public MoodFacing(Colony colony) {
        this.colony = colony;
    }

    @Override
    public List<Line> lookLines(UUID resident, ColonyView view) {
        return PresenceViews.mood(colony, resident)
            .<List<Line>>map(MoodFacing::linesFor)
            .orElseGet(List::of);
    }

    /**
     * The rule, kept apart from the colony so it can be tested without one: a resident who is getting
     * on with it says nothing, and nobody says anything about nobody.
     */
    static List<Line> linesFor(Mood mood) {
        if (mood == null || mood == Mood.CONTENT) {
            return List.of();
        }
        return List.of(Line.of(mood));
    }
}
