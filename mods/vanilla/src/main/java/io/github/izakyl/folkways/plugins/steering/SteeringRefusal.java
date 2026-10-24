package io.github.izakyl.folkways.plugins.steering;

import io.github.izakyl.folkways.core.api.terms.RefusalKind;

/**
 * PLA: WP-4 T5 — what a steering zone says when it keeps a resident out.
 *
 * <p>Keys follow {@link RefusalKind.Named}: {@code folkways.refusal.steering.*}. The engine reports
 * {@code Unassigned.Reason.ZONE_FORBIDDEN}; this enum is what the diagnosis layer turns it into, so a
 * board row and {@code /plaa why} read the same words.
 */
public enum SteeringRefusal implements RefusalKind.Named {

    /** A forbid zone covers the cell. */
    FORBIDDEN,

    /** An allow-only zone admits other subjects but not this one. */
    ONLY_FOR_OTHERS,

    /** A limit zone has as many residents inside as it allows. */
    QUOTA_FULL;

    public static SteeringRefusal of(ZoneRule.Verdict verdict) {
        return switch (verdict) {
            case ALLOWED -> throw new IllegalArgumentException("allowed is not a refusal");
            case FORBIDDEN -> FORBIDDEN;
            case ONLY_FOR_OTHERS -> ONLY_FOR_OTHERS;
            case QUOTA_FULL -> QUOTA_FULL;
        };
    }

    @Override
    public boolean lasting() {
        return switch (this) {
            // Both stay until the player redraws or removes the zone.
            case FORBIDDEN, ONLY_FOR_OTHERS -> true;
            // This one clears by itself as residents leave.
            case QUOTA_FULL -> false;
        };
    }
}