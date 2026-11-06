package io.github.izakyl.folkways.plugins.health;

import io.github.izakyl.folkways.front.api.notice.NoticeKind;
import java.util.Locale;

/**
 * PLA: M2 R0-4 — what the card says about a body that is too hurt to be given work.
 *
 * <p>Only what is true today: the resident is not given planned labour, and the player's move is to
 * hand the urgent work to somebody else. It deliberately does not promise recovery, because there is
 * no treatment mechanism yet.
 */
public enum Injury implements NoticeKind {

    /** Too hurt to be given planned labour. */
    RESTING,

    /** What to do about it: give the work to somebody else. */
    RESTING_ADVICE;

    @Override
    public String translationKey() {
        return "folkways.injury." + name().toLowerCase(Locale.ROOT);
    }
}
