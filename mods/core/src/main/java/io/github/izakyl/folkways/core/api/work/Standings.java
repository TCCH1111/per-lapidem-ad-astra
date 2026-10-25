package io.github.izakyl.folkways.core.api.work;

import io.github.izakyl.folkways.core.api.terms.RefusalKind;
import io.github.izakyl.folkways.core.engine.plan.Unassigned;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * PLA: WP-4 T4b / WP-5 D8 — where plugins hand the engine the standings they keep for a colony.
 *
 * <p>Every owner keeps one provider and names the reason its refusals are reported as. The engine
 * asks every provider and takes the first refusal, so zones and hours stack without either knowing
 * the other, and the schedule still records a plain {@code Unassigned.Reason} it can cache by
 * ordinal. {@link #reasonOf} turns whatever refusal came back into that reason.
 */
public final class Standings {

    private static final Map<ResourceLocation, Function<UUID, Standing>> PROVIDERS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Unassigned.Reason> REASONS = new ConcurrentHashMap<>();

    private Standings() {
    }

    public static void provide(ResourceLocation owner, Function<UUID, Standing> provider,
                               RefusalKind refusedAs, Unassigned.Reason reason) {
        if (owner == null) {
            return;
        }
        if (provider == null) {
            PROVIDERS.remove(owner);
            return;
        }
        PROVIDERS.put(owner, provider);
        if (refusedAs != null && reason != null) {
            REASONS.put(refusedAs.getClass(), reason);
        }
    }

    public static Standing of(UUID colony) {
        if (colony == null || PROVIDERS.isEmpty()) {
            return Standing.ALL;
        }
        return (resident, cell) -> {
            for (Function<UUID, Standing> provider : PROVIDERS.values()) {
                Optional<RefusalKind> why = provider.apply(colony).check(resident, cell);
                if (why.isPresent()) {
                    return why;
                }
            }
            return Optional.empty();
        };
    }

    /** The reason a refusal of this kind is reported as; a zone refusal stands in for the unknown. */
    public static Unassigned.Reason reasonOf(RefusalKind why) {
        return REASONS.getOrDefault(why.getClass(), Unassigned.Reason.ZONE_FORBIDDEN);
    }

    /** PLA: WP-8 — lets go of one owner's provider, so a plugin can unload without leaving a hand behind. */
    public static void remove(ResourceLocation owner) {
        if (owner != null) {
            PROVIDERS.remove(owner);
        }
    }

    public static void reset() {
        PROVIDERS.clear();
        REASONS.clear();
    }
}