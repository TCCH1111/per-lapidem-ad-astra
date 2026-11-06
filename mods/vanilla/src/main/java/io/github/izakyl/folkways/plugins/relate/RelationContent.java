package io.github.izakyl.folkways.plugins.relate;

import io.github.izakyl.folkways.base.persist.Reader;
import io.github.izakyl.folkways.core.api.colony.Colony;
import io.github.izakyl.folkways.core.api.resident.Resident;
import io.github.izakyl.folkways.core.api.resident.body.Bodies;
import io.github.izakyl.folkways.core.api.work.Doings;
import io.github.izakyl.folkways.front.api.Enrollment;
import io.github.izakyl.folkways.front.api.Registering;
import io.github.izakyl.folkways.core.api.Declaring;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * PLA: M2 R1-2 — who has been working beside whom lately.
 *
 * <p>The colony is sampled rather than hooked: every {@link #SAMPLE_TICKS} ticks it looks at who is
 * working and where, and hands pairs that are close enough to the book, which owns the arithmetic and
 * the cooldown. Sampling keeps the engine untouched, and it is honest about what it measures: presence
 * beside someone, not a conversation.
 */
public final class RelationContent {

    /** Five seconds: often enough to notice a shared shift, rare enough to be free. */
    public static final int SAMPLE_TICKS = 100;

    /** How close two working residents must be, in blocks, to count as working together. */
    public static final double NEARBY = 8.0;

    /** Persisted with the colony the same way the other plugins do it. */
    public static final int PERSIST_TICKS = 200;

    /** Once a game day: forget anyone who is no longer here. */
    public static final int PRUNE_TICKS = 24_000;

    /** colony → who → who they were working beside at the last sample. */
    private static final Map<UUID, Map<UUID, Set<UUID>>> BESIDE = new ConcurrentHashMap<>();

    private RelationContent() {
    }

    /** Where one resident is and whether they are busy, taken out of the world so the rule is testable. */
    public record Spot(UUID who, double x, double y, double z, boolean working) {
    }

    /**
     * Counts every pair of working residents standing close enough to each other.
     *
     * @return how many pairs the book accepted this sample, which the tests and the diagnostics read
     */
    public static int sample(List<Spot> spots, RelationState book, long now) {
        int counted = 0;
        double reach = NEARBY * NEARBY;
        for (int i = 0; i < spots.size(); i++) {
            Spot one = spots.get(i);
            if (!one.working()) {
                continue;
            }
            for (int j = i + 1; j < spots.size(); j++) {
                Spot other = spots.get(j);
                if (!other.working() || one.who().equals(other.who())) {
                    continue;
                }
                double dx = one.x() - other.x();
                double dy = one.y() - other.y();
                double dz = one.z() - other.z();
                if (dx * dx + dy * dy + dz * dz > reach) {
                    continue;
                }
                boolean forwards = book.touch(one.who(), other.who(), now);
                boolean backwards = book.touch(other.who(), one.who(), now);
                if (forwards || backwards) {
                    counted++;
                }
            }
        }
        return counted;
    }

    /** Reads the colony's residents out of every level they may stand in. */
    public static List<Spot> spotsOf(Colony colony, MinecraftServer server) {
        List<Spot> spots = new ArrayList<>();
        List<Resident> residents = colony.residents();
        for (ServerLevel level : server.getAllLevels()) {
            for (Resident resident : residents) {
                Entity entity = level.getEntity(resident.id());
                if (entity == null) {
                    continue;
                }
                Bodies.of(entity).ifPresent(body -> {
                    Vec3 at = body.mob().position();
                    boolean working = body.doing()
                        .map(doing -> !Doings.IDLE.equals(doing.what()))
                        .orElse(false);
                    spots.add(new Spot(body.id(), at.x, at.y, at.z, working));
                });
            }
        }
        return List.copyOf(spots);
    }

    /**
     * True when a mutual friend was working beside this resident at the last sample.
     *
     * <p>Read from the snapshot rather than the world: a resident card can be opened at any moment and
     * must not walk every level to answer one question.
     */
    public static boolean amongFriends(Colony colony, UUID who) {
        if (colony == null || who == null) {
            return false;
        }
        Map<UUID, Set<UUID>> near = BESIDE.get(colony.id());
        if (near == null) {
            return false;
        }
        Set<UUID> around = near.get(who);
        if (around == null || around.isEmpty()) {
            return false;
        }
        RelationState book = colony.service(RelationState.ID, RelationState.class).orElse(null);
        return book != null && around.stream().anyMatch(other -> book.mutualFriends(who, other));
    }

    /** Who was beside whom in one sample, both directions. */
    static Map<UUID, Set<UUID>> beside(List<Spot> spots) {
        Map<UUID, Set<UUID>> out = new HashMap<>();
        double reach = NEARBY * NEARBY;
        for (int i = 0; i < spots.size(); i++) {
            Spot one = spots.get(i);
            if (!one.working()) {
                continue;
            }
            for (int j = i + 1; j < spots.size(); j++) {
                Spot other = spots.get(j);
                if (!other.working() || one.who().equals(other.who())) {
                    continue;
                }
                double dx = one.x() - other.x();
                double dy = one.y() - other.y();
                double dz = one.z() - other.z();
                if (dx * dx + dy * dy + dz * dz > reach) {
                    continue;
                }
                out.computeIfAbsent(one.who(), id -> new HashSet<>()).add(other.who());
                out.computeIfAbsent(other.who(), id -> new HashSet<>()).add(one.who());
            }
        }
        return out;
    }

    public static void enroll(Registering registering) {
        registering.enrollment(RelationState.ID, new Enrollment(List.of()));
    }

    public static void declare(Declaring declaring) {
        declaring.colony(RelationState.ID, scope -> {
            Colony colony = scope.colony();
            RelationState book = RelationState.load(Reader.of(colony.kept(RelationState.ID)));
            scope.share(book);
            scope.closed(closing -> BESIDE.remove(colony.id()));
            scope.tick(server -> {
                long now = server.getTickCount();
                if (now % SAMPLE_TICKS == 0) {
                    List<Spot> spots = spotsOf(colony, server);
                    sample(spots, book, now);
                    BESIDE.put(colony.id(), beside(spots));
                }
                if (now % PRUNE_TICKS == 0) {
                    Set<UUID> present = new HashSet<>();
                    for (Resident resident : colony.residents()) {
                        present.add(resident.id());
                    }
                    book.prune(present);
                }
                if (now % PERSIST_TICKS == 0) {
                    colony.keep(RelationState.ID, book.save());
                }
            });
        });
    }
}
