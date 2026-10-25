package io.github.izakyl.folkways.core.api.work;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;

/**
 * PLA: WP-4 T4b, composed for WP-5 — where plugins hand the engine the standings they keep for a
 * colony.
 *
 * <p>Every owner keeps one provider and the engine asks all of them, allowing a cell only when all
 * agree. Zones and hours therefore stack without either one knowing about the other: the schedule
 * has one question to ask and one answer to honour. No provider means {@link Standing#ALL}, so an
 * engine with no plugin installed behaves exactly as it did before this existed.
 */
public final class Standings {

    private static final Map<ResourceLocation, Function<UUID, Standing>> PROVIDERS = new ConcurrentHashMap<>();

    private Standings() {
    }

    public static void provide(ResourceLocation owner, Function<UUID, Standing> next) {
        if (owner == null) {
            return;
        }
        if (next == null) {
            PROVIDERS.remove(owner);
        } else {
            PROVIDERS.put(owner, next);
        }
    }

    public static Standing of(UUID colony) {
        if (colony == null || PROVIDERS.isEmpty()) {
            return Standing.ALL;
        }
        return (resident, cell) -> {
            for (Function<UUID, Standing> provider : PROVIDERS.values()) {
                if (!provider.apply(colony).permits(resident, cell)) {
                    return false;
                }
            }
            return true;
        };
    }

    /** For tests: puts the engine back to allowing every cell. */
    public static void reset() {
        PROVIDERS.clear();
    }
}