package io.github.izakyl.folkways.plugins.threat;

import io.github.izakyl.folkways.base.persist.Reader;
import io.github.izakyl.folkways.base.persist.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;

/**
 * PLA: M2 T0 — a threat is known before it arrives, and the colony can count the days.
 *
 * <p>Deliberately stops at the gates: the state machine walks a threat from foretold to at-the-gates
 * and no further, because what happens when it gets there is a resolution rule that has not been
 * agreed. A threat that quietly resolved itself with no consequences would be a lie told by the
 * schedule, which is worse than a threat that visibly waits.
 */
public final class ThreatState {

    /** How far a threat has come. */
    public enum Stage {
        /** Known and counting down. */
        FORETOLD,
        /** It has arrived and nothing has decided the outcome yet. */
        AT_THE_GATES,
        /** Dealt with, one way or another. */
        RESOLVED
    }

    /** One danger, from the first rumour to whatever ends it. */
    public record Threat(String kind, int strength, long foretoldAt, long arrivesAt, Stage stage) {

        public Threat {
            kind = kind == null || kind.isBlank() ? "raiders" : kind;
            strength = Math.max(0, strength);
        }
    }

    private final List<Threat> threats = new ArrayList<>();

    /** Announces a danger that will arrive in {@code days} game days. */
    public Threat foretell(String kind, int strength, long now, int days) {
        Threat one = new Threat(kind, strength, now, now + Math.max(1, days) * 24_000L, Stage.FORETOLD);
        threats.add(one);
        return one;
    }

    public Optional<Threat> pending() {
        for (Threat one : threats) {
            if (one.stage() != Stage.RESOLVED) {
                return Optional.of(one);
            }
        }
        return Optional.empty();
    }

    /**
     * Walks every threat forward to the clock.
     *
     * @return the threats that arrived during this step, so the caller can announce them
     */
    public List<Threat> advance(long now) {
        List<Threat> arrived = new ArrayList<>();
        for (int i = 0; i < threats.size(); i++) {
            Threat one = threats.get(i);
            if (one.stage() == Stage.FORETOLD && now >= one.arrivesAt()) {
                Threat moved = new Threat(one.kind(), one.strength(), one.foretoldAt(), one.arrivesAt(),
                    Stage.AT_THE_GATES);
                threats.set(i, moved);
                arrived.add(moved);
            }
        }
        return List.copyOf(arrived);
    }

    /**
     * Marks every threat at the gates as dealt with. Called only after the caller has applied the
     * resolution's consequences; a threat is never resolved by the mere passing of time.
     */
    public void settle() {
        for (int i = 0; i < threats.size(); i++) {
            Threat one = threats.get(i);
            if (one.stage() == Stage.AT_THE_GATES) {
                threats.set(i, new Threat(one.kind(), one.strength(), one.foretoldAt(), one.arrivesAt(),
                    Stage.RESOLVED));
            }
        }
    }

    public List<Threat> all() {
        return List.copyOf(threats);
    }

    public CompoundTag save() {
        return Writer.of()
            .children(TAG_THREATS, threats, one -> Writer.of()
                .string(TAG_KIND, one.kind())
                .integer(TAG_STRENGTH, one.strength())
                .longValue(TAG_FORETOLD, one.foretoldAt())
                .longValue(TAG_ARRIVES, one.arrivesAt())
                .string(TAG_STAGE, one.stage().name())
                .tag())
            .tag();
    }

    public static ThreatState load(Reader reader) {
        ThreatState state = new ThreatState();
        for (Reader row : reader.children(TAG_THREATS)) {
            Stage stage;
            try {
                stage = Stage.valueOf(row.string(TAG_STAGE).orElse(Stage.FORETOLD.name()));
            } catch (IllegalArgumentException unknown) {
                stage = Stage.FORETOLD;
            }
            state.threats.add(new Threat(
                row.string(TAG_KIND).orElse("raiders"),
                row.integer(TAG_STRENGTH).orElse(0),
                row.longValue(TAG_FORETOLD).orElse(0L),
                row.longValue(TAG_ARRIVES).orElse(Long.MAX_VALUE),
                stage));
        }
        return state;
    }

    // ---- PLA: M2 T1/T2 — readiness and a deterministic, proportional resolution -----------------

    /**
     * PLA: M2 T2 (owner decision A/B/A) — the outcome of a threat meeting a colony.
     *
     * <p>Deterministic (A): the same strength and readiness always give the same answer, so a crisis
     * can be replayed. Proportional (B): the loss is the shortfall between strength and readiness, not
     * a fixed table. Capped (A): wounds top out just below "severe", so a defeat costs the colony rest
     * and care but never kills outright — death stays reserved for an later, explicitly worse tier.
     */
    public record Resolution(int loss, int severity, boolean held) {
    }

    /**
     * Resolves one threat against the colony's readiness.
     *
     * @param strength  how strong the danger is
     * @param readiness how prepared the colony is (a plain count of countable facts)
     * @return a deterministic outcome; loss is the proportional shortfall and severity is its capped
     *         form as a wound, never reaching the level the health book calls severe
     */
    public static Resolution resolve(int strength, int readiness) {
        int loss = Math.max(0, Math.max(0, strength) - Math.max(0, readiness));
        // Wound severity runs 1..5 and "severe" is 4; cap at 3 so a resolved threat never kills.
        int severity = loss <= 0 ? 0 : Math.min(3, 1 + (loss - 1) / 2);
        return new Resolution(loss, severity, loss == 0);
    }

    /**
     * PLA: M2 T1 — readiness as a plain count of the facts a colony can point to. Every input here is
     * something the player can see and question, so the number never wanders off on its own. Kept pure
     * so the contribution of each fact can be tested on its own.
     */
    public static int readiness(int militia, int weapons, int barriers, int warningDays) {
        return Math.max(0, militia) + Math.max(0, weapons) / 2 + Math.max(0, barriers) / 2
            + Math.min(5, Math.max(0, warningDays));
    }

    private static final String TAG_THREATS = "t";
    private static final String TAG_KIND = "k";
    private static final String TAG_STRENGTH = "s";
    private static final String TAG_FORETOLD = "f";
    private static final String TAG_ARRIVES = "a";
    private static final String TAG_STAGE = "g";
}
