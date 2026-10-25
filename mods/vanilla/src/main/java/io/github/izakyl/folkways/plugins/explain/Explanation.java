package io.github.izakyl.folkways.plugins.explain;

import io.github.izakyl.folkways.core.engine.plan.Unassigned;
import io.github.izakyl.folkways.plugins.hours.HoursContent;
import io.github.izakyl.folkways.plugins.steering.SteeringContent;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * PLA: WP-6 W1 — one place that turns the schedule's reason into words and a next step.
 *
 * <p>Every page and any later command must go through here, so the same refusal cannot be described
 * two ways. Pure: it reads the reason and nothing else, which is what lets all ten classes be tested
 * without a level, a colony or a resident.
 */
public final class Explanation {

    /** What the player reads, and where they can go to fix it. */
    public record Line(String titleKey, String adviceKey, Optional<ResourceLocation> page) {
    }

    private Explanation() {
    }

    public static Line of(Unassigned.Reason reason) {
        if (reason == null) {
            return unknown();
        }
        String key = "folkways.explain." + reason.name().toLowerCase(Locale.ROOT);
        Optional<ResourceLocation> page = switch (reason) {
            case ZONE_FORBIDDEN -> Optional.of(SteeringContent.ID);
            case HOURS_RESTING -> Optional.of(HoursContent.ID);
            default -> Optional.empty();
        };
        return new Line(key + ".title", key + ".advice", page);
    }

    public static Line unknown() {
        return new Line("folkways.explain.unknown.title", "folkways.explain.unknown.advice",
            Optional.empty());
    }
}