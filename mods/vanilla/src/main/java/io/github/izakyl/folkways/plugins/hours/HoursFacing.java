package io.github.izakyl.folkways.plugins.hours;

import io.github.izakyl.folkways.core.api.colony.ColonyView;
import io.github.izakyl.folkways.front.api.Facing;
import io.github.izakyl.folkways.front.api.panel.Board;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * PLA: WP-5 T4 — the hours page: what the timetable is doing right now and when it will change.
 *
 * <p>The knobs themselves are the base's own settings row; this page only reads the state back, so
 * the player sees the effect without opening a second screen.
 */
final class HoursFacing implements Facing {

    private final HoursState state;

    HoursFacing(HoursState state) {
        this.state = state;
    }

    @Override
    public Optional<Board> board(ResourceLocation page, ColonyView colony) {
        if (!HoursContent.PAGE.id().equals(page)) {
            return Optional.empty();
        }
        Component preset = Component.translatable(
            "folkways.page.hours.preset." + state.preset().name().toLowerCase(Locale.ROOT));
        Component detail = state.enabled()
            ? Component.translatable("folkways.page.hours.status", preset)
                .copy()
                .append(" ")
                .append(Component.translatable("folkways.page.hours.next",
                    Math.max(0L, state.nextChange()) / 1000L))
            : Component.translatable("folkways.page.hours.off");
        return Optional.of(Board.of(List.of(
            Board.Row.of(new ItemStack(Items.CLOCK),
                Component.translatable("folkways.page.hours"), detail))));
    }
}