package io.github.izakyl.folkways.plugins.steering;

import io.github.izakyl.folkways.base.persist.Reader;
import io.github.izakyl.folkways.base.persist.Writer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * PLA: WP-4 T2 — every steering zone of one colony and the answers it gives.
 *
 * <p>Kept through {@code colony.keep} under {@link SteeringContent#ID}. Reads are pure; the only
 * mutation paths are {@link #put} and {@link #remove}, which mark the state dirty so the next tick
 * writes it once instead of on every change.
 */
public final class SteeringState {

    private static final String TAG_ZONES = "zones";
    private static final String TAG_ID = "id";
    private static final String TAG_NAME = "name";
    private static final String TAG_KIND = "kind";
    private static final String TAG_SUBJECTS = "subjects";
    private static final String TAG_QUOTA = "quota";
    private static final String TAG_CELLS = "cells";
    private static final String TAG_AT = "at";

    /** PLA: WP-4 T6b — the one zone the painted delegation turns into. */
    public static final UUID PAINTED = UUID.fromString("00000000-0000-0000-0000-0000000005ee");

    private final Map<UUID, ZoneRule> zones = new LinkedHashMap<>();
    private int dismissedSignature = Integer.MIN_VALUE;
    private boolean dirty;

    public List<ZoneRule> zones() {
        return List.copyOf(zones.values());
    }

    public Optional<ZoneRule> zone(UUID id) {
        return Optional.ofNullable(zones.get(id));
    }

    public void put(ZoneRule rule) {
        zones.put(rule.id(), rule);
        dirty = true;
    }

    public boolean remove(UUID id) {
        boolean removed = zones.remove(id) != null;
        dirty |= removed;
        return removed;
    }

    /** The first rule that refuses this cell for this subject, with its reason. */
    public Optional<ZoneRule.Verdict> refusal(BlockPos cell, ResourceLocation subject, int insideCount) {
        for (ZoneRule rule : zones.values()) {
            ZoneRule.Verdict verdict = rule.verdict(cell, subject, insideCount);
            if (verdict != ZoneRule.Verdict.ALLOWED) {
                return Optional.of(verdict);
            }
        }
        return Optional.empty();
    }

    /**
     * PLA: WP-4 T4b — whether any forbid zone covers this cell. Only FORBID with an empty subject
     * list is consulted here: a resident's trade is not known at this point, so ALLOW_ONLY and LIMIT
     * stay with the diagnosis layer until they can name the subject.
     */
    public boolean forbids(BlockPos cell) {
        for (ZoneRule rule : zones.values()) {
            if (rule.kind() == ZoneRule.Kind.FORBID && rule.subjects().isEmpty() && rule.covers(cell)) {
                return true;
            }
        }
        return false;
    }

    /**
     * PLA: WP-4 T6b — adopts the cells the player painted for {@link SteeringContent#ZONE} as one
     * forbid zone, unless that very painting was dismissed. A changed painting is adopted again.
     */
    public void adoptPainted(java.util.Set<BlockPos> painted) {
        if (painted.isEmpty()) {
            return;
        }
        int signature = painted.hashCode();
        if (signature == dismissedSignature) {
            return;
        }
        dismissedSignature = Integer.MIN_VALUE;
        ZoneRule current = zones.get(PAINTED);
        String name = current == null ? "steering zone" : current.name();
        zones.put(PAINTED, new ZoneRule(PAINTED, name, ZoneRule.Kind.FORBID, List.of(), 0, painted));
        dirty = true;
    }

    /** Drops the painted zone and ignores this exact painting until the player draws a different one. */
    public boolean dismissPainted(java.util.Set<BlockPos> painted) {
        boolean removed = zones.remove(PAINTED) != null;
        dismissedSignature = painted.isEmpty() ? Integer.MIN_VALUE : painted.hashCode();
        dirty |= removed;
        return removed;
    }

    public boolean dirty() {
        return dirty;
    }

    public CompoundTag save() {
        dirty = false;
        return Writer.of().children(TAG_ZONES, zones.values(), SteeringState::saveZone).tag();
    }

    private static CompoundTag saveZone(ZoneRule rule) {
        return Writer.of()
            .uuid(TAG_ID, rule.id())
            .string(TAG_NAME, rule.name())
            .string(TAG_KIND, rule.kind().name())
            .ids(TAG_SUBJECTS, rule.subjects())
            .integer(TAG_QUOTA, rule.quota())
            .children(TAG_CELLS, rule.cells(), cell -> Writer.of().blockPos(TAG_AT, cell).tag())
            .tag();
    }

    public static SteeringState load(Reader reader) {
        SteeringState state = new SteeringState();
        for (Reader child : reader.children(TAG_ZONES)) {
            zoneOf(child).ifPresent(rule -> state.zones.put(rule.id(), rule));
        }
        state.dirty = false;
        return state;
    }

    private static Optional<ZoneRule> zoneOf(Reader reader) {
        Optional<UUID> id = reader.uuid(TAG_ID);
        Optional<String> kind = reader.string(TAG_KIND);
        if (id.isEmpty() || kind.isEmpty()) {
            return Optional.empty();
        }
        List<BlockPos> cells = reader.children(TAG_CELLS).stream()
            .flatMap(cell -> cell.blockPos(TAG_AT).stream())
            .toList();
        return Optional.of(new ZoneRule(
            id.get(),
            reader.string(TAG_NAME).orElse("zone"),
            ZoneRule.Kind.valueOf(kind.get()),
            reader.ids(TAG_SUBJECTS),
            reader.integer(TAG_QUOTA).orElse(0),
            Set.copyOf(cells)));
    }
}
