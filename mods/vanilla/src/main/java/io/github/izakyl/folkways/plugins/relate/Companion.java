package io.github.izakyl.folkways.plugins.relate;

import io.github.izakyl.folkways.front.api.notice.NoticeKind;

/**
 * PLA: M2 R1-4/R1-5 — the two things the card says about company: that it is happening, and what the
 * player can do about it.
 *
 * <p>NoticeKinds rather than literals so the words come from the language files like every other line
 * the game shows. The advice exists because a state the player cannot act on is just a number, and this
 * project has already paid twice for numbers nobody can act on.
 */
public enum Companion implements NoticeKind {

    /** Somebody is working beside this resident right now. */
    TOGETHER("folkways.relate.companion"),

    /** What to do with that: keep them posted together. */
    TOGETHER_ADVICE("folkways.relate.companion.advice");

    private final String key;

    Companion(String key) {
        this.key = key;
    }

    @Override
    public String translationKey() {
        return key;
    }
}
