package io.github.izakyl.folkways.core.api.work;

import java.util.UUID;
import net.minecraft.core.BlockPos;

/**
 * PLA: WP-4 T4 (L3, approved 2026-10-24) — whether a resident may stand on a cell at all.
 *
 * <p>The schedule asks this for every hand and every piece of work before it lays a route, so a zone
 * that refuses a cell keeps that resident off it and the refusal is reported rather than silently
 * dropped. {@link #ALL} is the default and keeps the engine exactly as it was when no plugin offers
 * a standing.
 */
public interface Standing {

    boolean permits(UUID resident, BlockPos cell);

    Standing ALL = (resident, cell) -> true;
}