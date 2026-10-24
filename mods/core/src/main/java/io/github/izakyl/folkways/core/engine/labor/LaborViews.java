package io.github.izakyl.folkways.core.engine.labor;

import net.minecraft.resources.ResourceLocation;

/**
 * PLA: read-only view of urge backoff for the diagnosis layer (docs/plans/PLAN-002-wp1-engine-readable-surface.md, T5).
 *
 * <p>{@code Urges} is package-private and owned by {@link ColonyLabor}; this facade lives in the same
 * package, reads the cooldown without consuming it, and exposes only a number. It never calls
 * {@code refused}, never re-plans, and never clears an entry.
 */
public final class LaborViews {

    private LaborViews() {
    }

    /** Ticks left before this urge may be offered again; {@code 0} when it is not cooling. */
    public static long coolingLeft(ColonyLabor labor, ResourceLocation urge, long now) {
        if (labor == null || urge == null) {
            return 0L;
        }
        return labor.urgeBook().coolingLeft(urge, now);
    }

    /** Whether the urge is still backing off at {@code now}. */
    public static boolean cooling(ColonyLabor labor, ResourceLocation urge, long now) {
        return coolingLeft(labor, urge, now) > 0L;
    }
}