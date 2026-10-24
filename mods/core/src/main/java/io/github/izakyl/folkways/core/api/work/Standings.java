package io.github.izakyl.folkways.core.api.work;

import java.util.UUID;
import java.util.function.Function;

/**
 * PLA: WP-4 T4b — where a plugin hands the engine the standing it keeps for a colony.
 *
 * <p>The engine never learns what a zone is: it asks this registry once when it builds a colony's
 * labor and reads whatever standing comes back. With no plugin installed the provider is
 * {@link Standing#ALL}, so the engine behaves exactly as it did before this existed.
 */
public final class Standings {

    private static final Function<UUID, Standing> NONE = colony -> Standing.ALL;

    private static volatile Function<UUID, Standing> provider = NONE;

    private Standings() {
    }

    public static void provide(Function<UUID, Standing> next) {
        provider = next == null ? NONE : next;
    }

    public static Standing of(UUID colony) {
        return colony == null ? Standing.ALL : provider.apply(colony);
    }

    /** For tests: puts the engine back to allowing every cell. */
    public static void reset() {
        provider = NONE;
    }
}