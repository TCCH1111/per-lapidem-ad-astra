package io.github.izakyl.folkways.plugins.steering;

import io.github.izakyl.folkways.core.api.colony.ColonyView;
import io.github.izakyl.folkways.front.api.Facing;
import io.github.izakyl.folkways.front.api.notice.Attempt;
import io.github.izakyl.folkways.front.api.panel.Board;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * PLA: WP-4 T6 — the steering page: the zones this colony keeps, each with the one act that removes
 * it. Removal is a {@code Board.Act.Do} with our own key, so the board needs no new verb.
 */
final class SteeringFacing implements Facing {

    private static final ItemStack ZONE_ICON = new ItemStack(Items.BARRIER);

    private final SteeringState state;

    SteeringFacing(SteeringState state) {
        this.state = state;
    }

    @Override
    public Optional<Board> board(ResourceLocation page, ColonyView colony) {
        if (!SteeringContent.PAGE.id().equals(page)) {
            return Optional.empty();
        }
        List<Board.Row> rows = new ArrayList<>();
        rows.add(Board.Row.heading(Component.translatable("folkways.page.steering.zones")));
        for (ZoneRule zone : state.zones()) {
            rows.add(new Board.Row(
                ZONE_ICON,
                Component.literal(zone.name()),
                Component.translatable("folkways.page.steering.zone",
                    Component.translatable("folkways.zone.kind." + zone.kind().name().toLowerCase(Locale.ROOT)),
                    zone.cells().size()),
                List.of(new Board.Act.Do(SteeringContent.REMOVE + zone.id(),
                    "folkways.page.steering.remove"))));
        }
        if (rows.size() == 1) {
            rows.add(Board.Row.of(new ItemStack(Items.PAPER),
                Component.translatable("folkways.page.steering.empty"), Component.empty()));
        }
        return Optional.of(Board.of(rows));
    }

    @Override
    public Attempt act(ResourceLocation page, String actionKey, Optional<BlockPos> at, ColonyView colony) {
        if (!SteeringContent.PAGE.id().equals(page) || !actionKey.startsWith(SteeringContent.REMOVE)) {
            return Attempt.went();
        }
        String raw = actionKey.substring(SteeringContent.REMOVE.length());
        try {
            state.remove(UUID.fromString(raw));
        } catch (IllegalArgumentException malformed) {
            return Attempt.refused(SteeringRefusal.FORBIDDEN);
        }
        return Attempt.went();
    }
}