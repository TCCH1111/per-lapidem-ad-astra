package io.github.izakyl.folkways.plugins.explain;

import io.github.izakyl.folkways.core.engine.plan.Unassigned;
import io.github.izakyl.folkways.plugins.hours.HoursContent;
import io.github.izakyl.folkways.plugins.steering.SteeringContent;
import io.github.izakyl.folkways.front.api.panel.Board;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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

    /**
     * PLA: WP-6 W3 — the row a page shows for work nobody took: the reason's own words, and a way to
     * the page that fixes it when there is one. At most one act, so a board row stays within budget.
     */
    public static Board.Row row(Unassigned nobody) {
        Line line = of(nobody.why());
        List<Board.Act> acts = line.page()
            .map(page -> List.<Board.Act>of(new Board.Act.Open(page)))
            .orElse(List.of());
        return new Board.Row(new ItemStack(Items.PAPER),
            Component.translatable(line.titleKey()),
            Component.translatable(line.adviceKey()),
            acts);
    }

    public static Line unknown() {
        return new Line("folkways.explain.unknown.title", "folkways.explain.unknown.advice",
            Optional.empty());
    }
}