package io.github.izakyl.folkways.plugins.hours;

import io.github.izakyl.folkways.core.api.terms.RefusalKind;

/**
 * PLA: WP-5 T5 — what the timetable says when it keeps a colony from working.
 *
 * <p>Keys follow {@link RefusalKind.Named}: {@code folkways.refusal.hours.*}. This is the vocabulary
 * the diagnosis needs; wiring it into the schedule still waits on the change recorded in
 * docs/reports/VERIFY-023 (the veto currently answers yes/no, so an off-hours refusal is reported
 * under the zone's name).
 */
public enum HoursRefusal implements RefusalKind.Named {

    /** Outside the working segments of the colony's timetable. */
    RESTING;

    @Override
    public boolean lasting() {
        // The clock comes round by itself: waiting is exactly what answers this one.
        return false;
    }
}