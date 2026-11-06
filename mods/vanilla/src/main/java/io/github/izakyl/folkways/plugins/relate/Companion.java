package io.github.izakyl.folkways.plugins.relate;

import io.github.izakyl.folkways.front.api.notice.NoticeKind;

/**
 * PLA: M2 R1-4 — the one thing the card says about company.
 *
 * <p>A NoticeKind rather than a literal so the words come from the language files like every other
 * line the game shows, and a bare key so it follows the same naming the rest of our notices use.
 */
public enum Companion implements NoticeKind {

    TOGETHER;

    @Override
    public String translationKey() {
        return "folkways.relate.companion";
    }
}
