package io.github.izakyl.folkways.plugins.person.living;

import io.github.izakyl.folkways.core.api.colony.Colony;
import io.github.izakyl.folkways.core.api.terms.WorldPos;
import java.util.Optional;
import java.util.UUID;

/**
 * PLA: read-only views over a colony's living state (hunger and bed claims), for the diagnosis
 * layer (docs/plans/PLAN-002-wp1-engine-readable-surface.md, T2).
 *
 * <p>{@code LivingPresence} is a colony-scoped service declared in
 * {@code LivingContent.declare(...)}; it stays package-private, and so do {@code Hunger} and
 * {@code Beds}. This facade lives in the same package, so it can read them, and it hands back only
 * immutable values. Nothing here ticks, sweeps, claims a bed or writes the saved state.
 */
public final class PresenceViews {

    private PresenceViews() {
    }

    /** The resident's belly, or empty when the colony or the resident is unknown to it. */
    public static Optional<HungerView> hunger(Colony colony, UUID resident) {
        if (colony == null || resident == null) {
            return Optional.empty();
        }
        return colony.service(LivingContent.ID, LivingPresence.class)
            .map(presence -> presence.bellyOf(resident))
            .map(PresenceViews::viewOf);
    }

    /** The resident's claimed bed, or {@link BedView#none()} when it has none. */
    public static BedView bed(Colony colony, UUID resident) {
        if (colony == null || resident == null) {
            return BedView.none();
        }
        return colony.service(LivingContent.ID, LivingPresence.class)
            .map(presence -> new BedView(presence.bedClaims().bedFor(resident)))
            .orElseGet(BedView::none);
    }

    /** PLA: M2 N3 — the resident's mood, read off the needs the colony already keeps for it. */
    public static java.util.Optional<io.github.izakyl.folkways.plugins.mood.Mood> mood(
            io.github.izakyl.folkways.core.api.colony.Colony colony, java.util.UUID resident) {
        if (colony == null || resident == null) {
            return java.util.Optional.empty();
        }
        return colony.service(LivingContent.ID, LivingPresence.class)
            .map(presence -> presence.needsOf(resident))
            .map(PresenceViews::moodOf);
    }

    /** Package-private so the gametest can prove the mapping without building a whole colony. */
    static io.github.izakyl.folkways.plugins.mood.Mood moodOf(
            io.github.izakyl.folkways.plugins.mood.Mood.Needs needs) {
        return io.github.izakyl.folkways.plugins.mood.Mood.of(needs);
    }

    // Package-private so the gametest can prove the mapping without building a whole colony.
    static HungerView viewOf(Hunger hunger) {
        return new HungerView(hunger.foodLevel(), Hunger.MAX_FOOD_LEVEL);
    }

    /**
     * An immutable picture of a belly.
     *
     * @param foodLevel    current food level (0 = starving)
     * @param maxFoodLevel the level a full belly holds ({@code Hunger.MAX_FOOD_LEVEL})
     */
    public record HungerView(int foodLevel, int maxFoodLevel) {

        public boolean starving() {
            return foodLevel <= 0;
        }

        /** 0.0 empty … 1.0 full. */
        public float fullness() {
            return maxFoodLevel <= 0 ? 0.0F : (float) foodLevel / maxFoodLevel;
        }
    }

    /** An immutable picture of a bed claim: the bed's cell, or nothing. */
    public record BedView(Optional<WorldPos> bed) {

        public static BedView none() {
            return new BedView(Optional.empty());
        }

        public boolean hasBed() {
            return bed.isPresent();
        }
    }
}