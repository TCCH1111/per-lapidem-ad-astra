package io.github.izakyl.folkways.plugins.steering;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * PLA: WP-4 T2 — one steering zone: cells whose use is forbidden, reserved for some subjects, or
 * limited to a number of them at once.
 *
 * <p>{@link #verdict} is a pure function of this rule. It never reads the world and never looks at
 * the clock, so the planner and the diagnosis layer can share it without either growing a side
 * effect.
 */
public record ZoneRule(UUID id, String name, Kind kind, List<ResourceLocation> subjects, int quota,
                       Set<BlockPos> cells) {

    public enum Kind {
        /** Nobody enters; with a non-empty subject list only those subjects are kept out. */
        FORBID,
        /** Only the listed subjects may enter. */
        ALLOW_ONLY,
        /** At most {@code quota} subjects inside at once; {@code quota <= 0} means no limit. */
        LIMIT,
        /**
         * The listed subjects are kept in: any cell this zone does not cover is forbidden to them, while
         * everybody else is unaffected outside it. Inside, only the listed subjects may enter.
         *
         * <p>With an empty subject list this kind does nothing, because a confinement that names nobody
         * would quietly forbid the whole world to nobody in particular.
         */
        CONFINE
    }

    public enum Verdict {
        ALLOWED,
        FORBIDDEN,
        ONLY_FOR_OTHERS,
        QUOTA_FULL
    }

    public ZoneRule {
        subjects = List.copyOf(subjects);
        cells = Set.copyOf(cells);
    }

    public static ZoneRule of(String name, Kind kind, List<ResourceLocation> subjects, int quota,
                              Set<BlockPos> cells) {
        return new ZoneRule(UUID.randomUUID(), name, kind, subjects, quota, cells);
    }

    public boolean covers(BlockPos cell) {
        return cells.contains(cell);
    }

    /**
     * Whether {@code subject} may use {@code cell} under this rule.
     *
     * @param subject     the vocation or resident kind asking for the cell
     * @param insideCount how many of that subject are already inside this zone
     */
    public Verdict verdict(BlockPos cell, ResourceLocation subject, int insideCount) {
        boolean inside = covers(cell);
        if (!inside) {
            // A confinement keeps its subjects in rather than keeping others out, so outside the zone it
            // is the listed subjects that are refused, and only they.
            if (kind == Kind.CONFINE && !subjects.isEmpty() && subjects.contains(subject)) {
                return Verdict.FORBIDDEN;
            }
            return Verdict.ALLOWED;
        }
        return switch (kind) {
            case FORBID -> subjects.isEmpty() || subjects.contains(subject)
                ? Verdict.FORBIDDEN
                : Verdict.ALLOWED;
            case ALLOW_ONLY -> subjects.contains(subject) ? Verdict.ALLOWED : Verdict.ONLY_FOR_OTHERS;
            case LIMIT -> quota > 0 && insideCount >= quota ? Verdict.QUOTA_FULL : Verdict.ALLOWED;
            case CONFINE -> subjects.isEmpty() || subjects.contains(subject)
                ? Verdict.ALLOWED
                : Verdict.ONLY_FOR_OTHERS;
        };
    }
}