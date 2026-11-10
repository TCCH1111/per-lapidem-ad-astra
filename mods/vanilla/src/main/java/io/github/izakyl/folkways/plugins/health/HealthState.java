package io.github.izakyl.folkways.plugins.health;

import io.github.izakyl.folkways.base.persist.Reader;
import io.github.izakyl.folkways.base.persist.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

/**
 * PLA: M2 R0-1 — what has happened to a resident's body, and how much blood is left.
 *
 * <p>This is the data half of the medical slice and nothing else: it records wounds and lets blood run,
 * but it does not yet change behaviour, because whether being hurt should also keep somebody off work is
 * a gameplay decision that has not been made. Keeping the two apart now means the answer can change
 * without touching the shape of what is stored.
 *
 * <p>A wound remembers where, what, how bad, from where, and when, because the design treats a wound
 * without a source as a defect rather than a detail.
 */
public final class HealthState {

    /** Full blood, in the same arbitrary unit the rest of the plugin uses. */
    public static final double MAX_BLOOD = 100.0;

    /** Chance per day that an untreated open wound becomes infected. */
    public static final double INFECTION_CHANCE_PER_DAY = 0.10;

    /** How much infection an untreated infected wound gains per day. */
    public static final int INFECTION_GAIN_PER_DAY = 6;

    /** At this much infection the part is heading for necrosis (PRD-013's 60). */
    public static final int NECROTIC_AT = 60;

    /** At or above this severity a wound is severe: the resident is not given planned work. */
    public static final int SEVERE_SEVERITY = 4;

    /** Below this fraction of full blood a resident is severe regardless of the wounds themselves. */
    public static final double SEVERE_BLOOD_FRACTION = 0.4;

    /** Blood lost per tick per point of severity, while a wound is open. */
    public static final double BLEED_PER_SEVERITY = 0.02;

    /** One injury. */
    public record Wound(String part, String kind, int severity, String source, long since, boolean treated) {

        public Wound {
            if (part == null || part.isBlank()) {
                throw new IllegalArgumentException("a wound needs a body part");
            }
            if (kind == null || kind.isBlank()) {
                throw new IllegalArgumentException("a wound needs a kind");
            }
            severity = Math.max(0, Math.min(5, severity));
            source = source == null || source.isBlank() ? "unknown" : source;
        }

        public boolean bleeds() {
            return !treated && severity > 0;
        }
    }

    private final Map<UUID, Double> blood = new HashMap<>();
    private final Map<UUID, List<Wound>> wounds = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> infections = new HashMap<>();
    private final List<Death> deaths = new ArrayList<>();

    public double blood(UUID who) {
        return blood.getOrDefault(who, MAX_BLOOD);
    }

    public List<Wound> wounds(UUID who) {
        return List.copyOf(wounds.getOrDefault(who, List.of()));
    }

    public boolean hurt(UUID who) {
        return !wounds(who).isEmpty();
    }

    /** Records a new wound; a resident already at zero stays at zero rather than going negative. */
    public void wound(UUID who, Wound one) {
        wounds.computeIfAbsent(who, id -> new ArrayList<>()).add(one);
        blood.putIfAbsent(who, MAX_BLOOD);
    }

    /** Marks the oldest matching open wound as treated, which stops it bleeding. */
    public boolean treat(UUID who, String part) {
        List<Wound> list = wounds.get(who);
        if (list == null) {
            return false;
        }
        for (int i = 0; i < list.size(); i++) {
            Wound one = list.get(i);
            if (one.bleeds() && (part == null || one.part().equals(part))) {
                list.set(i, new Wound(one.part(), one.kind(), one.severity(), one.source(), one.since(), true));
                return true;
            }
        }
        return false;
    }

    /**
     * Lets blood run for {@code ticks}.
     *
     * @return the blood after the wound has bled, floored at zero
     */
    public double bleed(UUID who, long ticks) {
        List<Wound> list = wounds.get(who);
        if (list == null || list.isEmpty() || ticks <= 0) {
            return blood(who);
        }
        double lost = 0;
        for (Wound one : list) {
            if (one.bleeds()) {
                lost += one.severity() * BLEED_PER_SEVERITY * ticks;
            }
        }
        double next = Math.max(0.0, blood(who) - lost);
        blood.put(who, next);
        return next;
    }

    /** Everyone the book knows about, so the clock can let their blood run. */
    public java.util.Set<UUID> who() {
        return java.util.Set.copyOf(wounds.keySet());
    }

    /**
     * Whether this resident is badly enough hurt to be kept from planned labour (owner's choice C:
     * light injuries work on, severe ones stop).
     *
     * <p>The line is a judgement, not a measurement: a wound of four or worse, or less than forty per
     * cent of their blood left. Both numbers live here so they can be tuned in one place later.
     */
    public boolean severe(UUID who) {
        if (blood(who) < MAX_BLOOD * SEVERE_BLOOD_FRACTION) {
            return true;
        }
        for (Wound one : wounds(who)) {
            if (one.severity() >= SEVERE_SEVERITY) {
                return true;
            }
        }
        return necrotic(who);
    }

    /**
     * PLA: M2 R1-1 — an untreated wound may turn. The roll is taken as an argument rather than drawn
     * here, so the rule is a pure function of its inputs and a test can say what happens on a bad day.
     *
     * @return true when the roll infected the part
     */
    public boolean maybeInfect(UUID who, String part, double roll) {
        if (who == null || part == null || part.isBlank()) {
            return false;
        }
        if (!woundAt(who, part).isPresent() || infection(who, part) > 0) {
            return false;
        }
        if (roll >= INFECTION_CHANCE_PER_DAY) {
            return false;
        }
        infections.computeIfAbsent(who, id -> new HashMap<>()).merge(part, 1, Integer::sum);
        return true;
    }

    /** How infected one part is, from zero to a hundred. */
    public int infection(UUID who, String part) {
        Map<String, Integer> parts = infections.get(who);
        if (parts == null) {
            return 0;
        }
        return Math.max(0, Math.min(100, parts.getOrDefault(part, 0)));
    }

    public boolean infected(UUID who) {
        Map<String, Integer> parts = infections.get(who);
        return parts != null && parts.values().stream().anyMatch(value -> value > 0);
    }

    /** Lets infection deepen for a number of days, capped at a hundred. */
    public void progressInfection(UUID who, int days) {
        Map<String, Integer> parts = infections.get(who);
        if (parts == null || days <= 0) {
            return;
        }
        parts.replaceAll((part, value) -> Math.min(100, value + INFECTION_GAIN_PER_DAY * days));
    }

    /** Whether any part has reached the point where the flesh is dying. */
    public boolean necrotic(UUID who) {
        Map<String, Integer> parts = infections.get(who);
        return parts != null && parts.values().stream().anyMatch(value -> value >= NECROTIC_AT);
    }

    private Optional<Wound> woundAt(UUID who, String part) {
        List<Wound> list = wounds.get(who);
        if (list == null) {
            return Optional.empty();
        }
        return list.stream().filter(one -> one.part().equals(part)).findFirst();
    }

    /** Removes everything belonging to somebody who is no longer here. */
    public void forget(UUID who) {
        blood.remove(who);
        wounds.remove(who);
        infections.remove(who);
    }

    public int size() {
        return wounds.size();
    }

    public CompoundTag save() {
        List<Entry> rows = new ArrayList<>();
        for (Map.Entry<UUID, List<Wound>> subject : wounds.entrySet()) {
            int index = 0;
            for (Wound one : subject.getValue()) {
                rows.add(new Entry(subject.getKey(), index++, one));
            }
        }
        List<Infection> infectionRows = new ArrayList<>();
        for (Map.Entry<UUID, Map<String, Integer>> subject : infections.entrySet()) {
            for (Map.Entry<String, Integer> one : subject.getValue().entrySet()) {
                infectionRows.add(new Infection(subject.getKey(), one.getKey(), one.getValue()));
            }
        }
        List<Vital> vitals = new ArrayList<>();
        for (Map.Entry<UUID, Double> one : blood.entrySet()) {
            vitals.add(new Vital(one.getKey(), (int) Math.round(one.getValue() * 10.0)));
        }
        return Writer.of()
            .children(TAG_WOUNDS, rows, row -> Writer.of()
                .uuid(TAG_WHO, row.who())
                .integer(TAG_INDEX, row.index())
                .string(TAG_PART, row.wound().part())
                .string(TAG_KIND, row.wound().kind())
                .integer(TAG_SEVERITY, row.wound().severity())
                .string(TAG_SOURCE, row.wound().source())
                .longValue(TAG_SINCE, row.wound().since())
                .flag(TAG_TREATED, row.wound().treated())
                .tag())
            .children(TAG_DEATHS, deaths, one -> Writer.of()
                .uuid(TAG_WHO, one.who())
                .string(TAG_DEATH_NAME, one.name())
                .string(TAG_DEATH_CAUSE, one.cause())
                .longValue(TAG_SINCE, one.at())
                .tag())
            .children(TAG_INFECTIONS, infectionRows, row -> Writer.of()
                .uuid(TAG_WHO, row.who())
                .string(TAG_PART, row.part())
                .integer(TAG_INFECTION, row.amount())
                .tag())
            .children(TAG_BLOOD, vitals, row -> Writer.of()
                .uuid(TAG_WHO, row.who())
                .integer(TAG_BLOOD_VALUE, row.tenths())
                .tag())
            .tag();
    }

    public static HealthState load(Reader reader) {
        HealthState state = new HealthState();
        for (Reader row : reader.children(TAG_WOUNDS)) {
            Optional<UUID> who = row.uuid(TAG_WHO);
            if (who.isEmpty()) {
                continue;
            }
            state.wound(who.get(), new Wound(
                row.string(TAG_PART).orElse("body"),
                row.string(TAG_KIND).orElse("cut"),
                row.integer(TAG_SEVERITY).orElse(1),
                row.string(TAG_SOURCE).orElse("unknown"),
                row.longValue(TAG_SINCE).orElse(0L),
                row.flag(TAG_TREATED).orElse(false)));
        }
        for (Reader row : reader.children(TAG_DEATHS)) {
            row.uuid(TAG_WHO).ifPresent(who -> state.deaths.add(new Death(who,
                row.string(TAG_DEATH_NAME).orElse("someone"),
                row.string(TAG_DEATH_CAUSE).orElse("unknown"),
                row.longValue(TAG_SINCE).orElse(0L))));
        }
        for (Reader row : reader.children(TAG_INFECTIONS)) {
            Optional<String> part = row.string(TAG_PART);
            row.uuid(TAG_WHO).filter(who -> part.isPresent()).ifPresent(who ->
                state.infections.computeIfAbsent(who, id -> new HashMap<>())
                    .merge(part.get(), row.integer(TAG_INFECTION).orElse(0), Integer::sum));
        }
        for (Reader row : reader.children(TAG_BLOOD)) {
            row.uuid(TAG_WHO).ifPresent(who -> state.blood.put(who,
                Math.min(MAX_BLOOD, row.integer(TAG_BLOOD_VALUE).orElse((int) (MAX_BLOOD * 10)) / 10.0)));
        }
        return state;
    }

    private record Entry(UUID who, int index, Wound wound) {
    }

    /** One death, kept so the colony can say what happened and to whom (PRD-009 G2). */
    public record Death(UUID who, String name, String cause, long at) {

        public Death {
            name = name == null || name.isBlank() ? "someone" : name;
            cause = cause == null || cause.isBlank() ? "unknown" : cause;
        }
    }

    /**
     * PLA: M2 R1-3 groundwork — every death leaves a trace.
     *
     * <p>The design asks that a death have weight, which starts with the colony being able to say who
     * died, of what and when. Where the body goes and how the burial works is a later slice, but the
     * record is needed whichever of those answers is chosen.
     */
    public void record(Death one) {
        deaths.add(one);
    }

    /** The most recent deaths first, at most {@code limit}. */
    public List<Death> recentDeaths(int limit) {
        List<Death> out = new ArrayList<>();
        for (int i = deaths.size() - 1; i >= 0 && out.size() < Math.max(0, limit); i--) {
            out.add(deaths.get(i));
        }
        return List.copyOf(out);
    }

    public int deathCount() {
        return deaths.size();
    }

    /** One part's infection, as a row. */
    private record Infection(UUID who, String part, int amount) {
    }

    /** Blood as tenths, because the persistor stores integers and blood needs one decimal. */
    private record Vital(UUID who, int tenths) {
    }

    private static final String TAG_WOUNDS = "w";
    private static final String TAG_BLOOD = "b";
    private static final String TAG_WHO = "u";
    private static final String TAG_INDEX = "i";
    private static final String TAG_PART = "p";
    private static final String TAG_KIND = "k";
    private static final String TAG_SEVERITY = "s";
    private static final String TAG_SOURCE = "src";
    private static final String TAG_SINCE = "t";
    private static final String TAG_TREATED = "x";
    private static final String TAG_BLOOD_VALUE = "v";
    private static final String TAG_INFECTIONS = "n";
    private static final String TAG_DEATHS = "d";
    private static final String TAG_DEATH_NAME = "dn";
    private static final String TAG_DEATH_CAUSE = "dc";
    private static final String TAG_INFECTION = "inf";
}
