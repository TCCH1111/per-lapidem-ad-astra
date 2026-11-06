package io.github.izakyl.folkways.plugins.relate;

import io.github.izakyl.folkways.base.persist.Reader;
import io.github.izakyl.folkways.base.persist.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * PLA: M2 R1 — what one colony remembers about its residents knowing each other.
 *
 * <p>One-directional, like the PRD asks: A may be fond of B while B has not decided. Two values only,
 * a bond and when it last moved, because the slice is "they work beside each other and warm to each
 * other", not the whole social simulation. Nothing here reads the world; the colony hands in pairs and
 * this decides whether a touch counts, which keeps the arithmetic testable without a colony.
 */
public final class RelationState {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("folkways", "relate");

    /** Per the PRD: nobody keeps more than thirty people in mind. */
    public static final int CAP = 30;

    /** Half a game hour, so two residents who pass each other once do not become friends. */
    public static final long TOUCH_TICKS = 500L;

    /** Marks a moment that never happened, so the first bond or grudge is not blocked by a cooldown. */
    private static final long NEVER = -1L;

    /** From the PRD's bands: acquainted at ten, friend at forty. */
    public static final int ACQUAINTED = 10;
    public static final int FRIEND = 40;
    public static final int FRICTION = 20;
    public static final int RESENTMENT = 40;
    public static final int ENMITY = 60;

    private static final String TAG_BONDS = "bonds";
    private static final String TAG_SUBJECT = "s";
    private static final String TAG_TARGET = "t";
    private static final String TAG_BOND = "b";
    private static final String TAG_GRUDGE = "g";
    private static final String TAG_GRUDGE_LAST = "gl";
    private static final String TAG_LAST = "l";
    private static final String TAG_SHARED = "sh";

    /** subject → target → bond. */
    private final Map<UUID, Map<UUID, Bond>> book = new HashMap<>();

    /** One side of one relationship. */
    public record Bond(UUID target, int bond, int grudge, long lastTouch, long lastGrudge, int shared) {
    }

    /** A row of the book, flattened for saving. */
    private record Entry(UUID subject, Bond bond) {
    }

    /**
     * Counts one meeting, if enough time has passed since the last one.
     *
     * @return true when the bond actually moved, so the caller can show it
     */
    public boolean touch(UUID subject, UUID target, long now) {
        if (subject == null || target == null || subject.equals(target)) {
            return false;
        }
        Map<UUID, Bond> known = book.computeIfAbsent(subject, id -> new HashMap<>());
        Bond before = known.get(target);
        if (before != null && before.lastTouch() != NEVER && now - before.lastTouch() < TOUCH_TICKS) {
            return false;
        }
        int next = before == null ? 1 : Math.min(100, before.bond() + 1);
        int grudge = before == null ? 0 : before.grudge();
        long lastGrudge = before == null ? NEVER : before.lastGrudge();
        int shared = before == null ? 0 : before.shared();
        if (before == null) {
            makeRoomFor(known);
        }
        known.put(target, new Bond(target, next, grudge, now, lastGrudge, shared));
        return true;
    }

    /** Marks a shared experience, which the PRD uses as a second gate for the closest bands. */
    public void share(UUID subject, UUID target, long now) {
        Map<UUID, Bond> known = book.get(subject);
        if (known == null) {
            return;
        }
        Bond before = known.get(target);
        if (before != null) {
            known.put(target, new Bond(target, before.bond(), before.grudge(), now, before.lastGrudge(),
                Math.min(255, before.shared() + 1)));
        }
    }

    public boolean friction(UUID subject, UUID target, long now) {
        if (subject == null || target == null || subject.equals(target)) {
            return false;
        }
        Map<UUID, Bond> known = book.computeIfAbsent(subject, id -> new HashMap<>());
        Bond before = known.get(target);
        if (before != null && before.lastGrudge() != NEVER && now - before.lastGrudge() < TOUCH_TICKS) {
            return false;
        }
        int bond = before == null ? 0 : before.bond();
        int grudge = before == null ? 1 : Math.min(100, before.grudge() + 1);
        int shared = before == null ? 0 : before.shared();
        if (before == null) {
            makeRoomFor(known);
        }
        known.put(target, new Bond(target, bond, grudge, before == null ? NEVER : before.lastTouch(), now, shared));
        return true;
    }

    public int grudge(UUID subject, UUID target) {
        Map<UUID, Bond> known = book.get(subject);
        if (known == null) {
            return 0;
        }
        Bond one = known.get(target);
        return one == null ? 0 : one.grudge();
    }

    public boolean resents(UUID subject, UUID target) {
        return grudge(subject, target) >= RESENTMENT;
    }

    public boolean mutualResentment(UUID one, UUID other) {
        return resents(one, other) && resents(other, one);
    }

    public int bond(UUID subject, UUID target) {
        Map<UUID, Bond> known = book.get(subject);
        if (known == null) {
            return 0;
        }
        Bond bond = known.get(target);
        return bond == null ? 0 : bond.bond();
    }

    public boolean acquainted(UUID subject, UUID target) {
        return bond(subject, target) >= ACQUAINTED;
    }

    public boolean friends(UUID subject, UUID target) {
        return bond(subject, target) >= FRIEND;
    }

    /** Both sides agree, which is what "they work well together" has to mean. */
    public boolean mutualFriends(UUID one, UUID other) {
        return friends(one, other) && friends(other, one);
    }

    public int size(UUID subject) {
        Map<UUID, Bond> known = book.get(subject);
        return known == null ? 0 : known.size();
    }

    /** Everyone this resident holds an opinion about, strongest first. */
    public List<Bond> known(UUID subject) {
        Map<UUID, Bond> known = book.get(subject);
        if (known == null) {
            return List.of();
        }
        List<Bond> all = new ArrayList<>(known.values());
        all.sort(Comparator.comparingInt(Bond::bond).reversed());
        return List.copyOf(all);
    }

    /**
     * Forgets anyone who is no longer in the colony: a resident who died or left must not keep
     * occupying one of the thirty slots, and nobody should be fond of a ghost.
     */
    public int prune(Set<UUID> present) {
        int removed = 0;
        for (Map<UUID, Bond> known : book.values()) {
            int before = known.size();
            known.keySet().removeIf(target -> !present.contains(target));
            removed += before - known.size();
        }
        int subjects = book.size();
        book.keySet().removeIf(id -> !present.contains(id));
        return removed + (subjects - book.size()) * 0;
    }

    /** Out with the least regarded, then the least recently touched, when the head is full. */
    private void makeRoomFor(Map<UUID, Bond> known) {
        if (known.size() < CAP) {
            return;
        }
        Optional<Bond> weakest = known.values().stream()
            .min(Comparator.comparingInt(Bond::bond).thenComparingLong(Bond::lastTouch));
        weakest.ifPresent(worst -> known.remove(worst.target()));
    }

    public CompoundTag save() {
        List<Entry> rows = new ArrayList<>();
        for (Map.Entry<UUID, Map<UUID, Bond>> subject : book.entrySet()) {
            for (Bond bond : subject.getValue().values()) {
                rows.add(new Entry(subject.getKey(), bond));
            }
        }
        return Writer.of()
            .children(TAG_BONDS, rows, row -> Writer.of()
                .uuid(TAG_SUBJECT, row.subject())
                .uuid(TAG_TARGET, row.bond().target())
                .integer(TAG_BOND, row.bond().bond())
                .integer(TAG_GRUDGE, row.bond().grudge())
                .longValue(TAG_LAST, row.bond().lastTouch())
                .longValue(TAG_GRUDGE_LAST, row.bond().lastGrudge())
                .integer(TAG_SHARED, row.bond().shared())
                .tag())
            .tag();
    }

    public static RelationState load(Reader reader) {
        RelationState state = new RelationState();
        for (Reader one : reader.children(TAG_BONDS)) {
            Optional<UUID> subject = one.uuid(TAG_SUBJECT);
            Optional<UUID> target = one.uuid(TAG_TARGET);
            if (subject.isEmpty() || target.isEmpty()) {
                continue;
            }
            int bond = one.integer(TAG_BOND).orElse(0);
            int grudge = one.integer(TAG_GRUDGE).orElse(0);
            long lastGrudge = one.longValue(TAG_GRUDGE_LAST).orElse(0L);
            long last = one.longValue(TAG_LAST).orElse(0L);
            int shared = one.integer(TAG_SHARED).orElse(0);
            state.book.computeIfAbsent(subject.get(), id -> new HashMap<>())
                .put(target.get(), new Bond(target.get(), bond, grudge, last, lastGrudge, shared));
        }
        return state;
    }
}
