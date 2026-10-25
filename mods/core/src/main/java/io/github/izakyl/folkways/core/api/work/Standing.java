package io.github.izakyl.folkways.core.api.work;

import io.github.izakyl.folkways.core.api.terms.RefusalKind;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;

/**
 * PLA: WP-4 T4 / WP-5 D8 (L3, approved) — whether a resident may stand on a cell, and if not, why.
 *
 * <p>The refusal is what the player is told: it is the plugin's own {@link RefusalKind}, so a zone
 * says a zone forbids and a timetable says the colony is off its hours. The engine turns it into an
 * {@code Unassigned.Reason} through the registry that installed the provider, which keeps the
 * schedule's cached reasons as plain enum values.
 */
public interface Standing {

    /** Empty when the resident may stand there, otherwise the words that explain why not. */
    Optional<RefusalKind> check(UUID resident, BlockPos cell);

    default boolean permits(UUID resident, BlockPos cell) {
        return check(resident, cell).isEmpty();
    }

    Standing ALL = (resident, cell) -> Optional.empty();
}