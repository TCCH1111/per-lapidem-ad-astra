package io.github.izakyl.folkways.plugins.mood;

import java.util.Locale;

/**
 * PLA: M2 N1 — a resident's mood, read off the needs it already has.
 *
 * <p>This is arithmetic, not simulation: it takes a snapshot the colony can produce (hunger and
 * weariness as the base reports them, whether work was refused recently, whether there is any work
 * at all) and returns one band. Every band must answer "what should the player do", or it does not
 * belong here — a mood the player cannot act on is another number, and this project has already
 * paid for numbers nobody can act on (VERIFY-031, VERIFY-033).
 */
public enum Mood {

    /** Fed, rested, working: nothing to do. */
    CONTENT,

    /** One need is getting low, or work has gone quiet for a while. */
    WORRIED,

    /** A need is below what a resident wants to work at, or work was refused. */
    STRESSED,

    /** A need is at the level where the base says the resident must stop and see to it. */
    DESPERATE;

    /**
     * The snapshot a colony can hand in.
     *
     * @param foodLevel    current food level, 0 empty
     * @param foodFull     what a full belly holds (the base's own maximum)
     * @param foodUrgent   below the level the base itself calls urgent
     * @param alertness    current rest level, 0 exhausted
     * @param restSpent    the level the base treats as fully rested
     * @param restUrgent   the level the base treats as needing sleep now
     * @param workRefused  work was refused for this resident since the last look
     * @param workWaiting  there is work on the colony's plan waiting to be taken
     */
    public record Needs(int foodLevel, int foodFull, boolean foodUrgent,
                        int alertness, int restSpent, boolean restUrgent,
                        boolean workRefused, boolean workWaiting) {
    }

    /** How the mood reads to a player: one line, and what to do about it. */
    public String titleKey() {
        return "folkways.mood." + name().toLowerCase(Locale.ROOT) + ".title";
    }

    public String adviceKey() {
        return "folkways.mood." + name().toLowerCase(Locale.ROOT) + ".advice";
    }

    /** Pure: reads the snapshot and nothing else. */
    public static Mood of(Needs needs) {
        if (needs == null) {
            return CONTENT;
        }
        if (needs.foodUrgent() || needs.restUrgent()) {
            return DESPERATE;
        }
        boolean hungry = needs.foodFull() > 0 && needs.foodLevel() * 4 <= needs.foodFull();
        boolean tired = needs.restSpent() > 0 && needs.alertness() * 4 <= needs.restSpent();
        if (hungry || tired) {
            return STRESSED;
        }
        if (needs.workRefused()) {
            return STRESSED;
        }
        boolean gettingLow = needs.foodFull() > 0 && needs.foodLevel() * 2 <= needs.foodFull();
        boolean wearing = needs.restSpent() > 0 && needs.alertness() * 2 <= needs.restSpent();
        if (gettingLow || wearing || !needs.workWaiting()) {
            return WORRIED;
        }
        return CONTENT;
    }
}