package io.github.izakyl.folkways.plugins.relate;

import io.github.izakyl.folkways.front.api.notice.NoticeKind;
import java.util.Locale;

/**
 * PLA: M2 R1-4/R1-5 — the two things the card says about company: that it is happening, and what the
 * player can do about it.
 *
 * <p>The key is derived from the constant name rather than written out, because the i18n gate reads that
 * rule to prove every declared value has words; an enum that spells its own keys cannot be checked.
 * The advice exists because a state the player cannot act on is just a number.
 */
public enum Relate implements NoticeKind {

    /** Somebody is working beside this resident right now. */
    COMPANION,

    /** What to do with that: keep them posted together. */
    COMPANION_ADVICE,

    /** Somebody they resent is working beside them right now. */
    RIVAL,

    /** What to do with that: post them apart. */
    RIVAL_ADVICE;

    @Override
    public String translationKey() {
        return "folkways.relate." + name().toLowerCase(Locale.ROOT);
    }
}
