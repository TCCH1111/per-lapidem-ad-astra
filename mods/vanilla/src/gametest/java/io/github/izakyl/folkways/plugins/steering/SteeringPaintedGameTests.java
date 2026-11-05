package io.github.izakyl.folkways.plugins.steering;

import io.github.izakyl.folkways.FolkwaysMod;
import io.github.izakyl.folkways.plugins.FolkwaysVanillaMod;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * PLA: WP-4 T6b — the painted volume becomes the one forbid zone, and dismissing it ignores that
 * exact painting until the player draws a different one.
 */
@EventBusSubscriber(modid = FolkwaysVanillaMod.ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SteeringPaintedGameTests {

    private static final String TEMPLATE = "empty";
    private static final BlockPos INSIDE = new BlockPos(0, 0, 0);
    private static final BlockPos OTHER = new BlockPos(4, 0, 4);

    private SteeringPaintedGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(SteeringPaintedGameTests.class);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aPaintedVolumeBecomesAForbidZone(GameTestHelper helper) {
        SteeringState state = new SteeringState();
        state.adoptPainted(Set.of(INSIDE));
        helper.assertTrue(state.zones().size() == 1, "one painted volume makes one zone, got " + state.zones().size());
        ZoneRule zone = state.zones().getFirst();
        helper.assertTrue(zone.kind() == ZoneRule.Kind.FORBID, "the painted zone forbids");
        helper.assertTrue(state.forbids(INSIDE), "the painted cell is forbidden");
        helper.assertTrue(!state.forbids(OTHER), "a cell outside the painting is not");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void dismissingIgnoresThatPaintingUntilItChanges(GameTestHelper helper) {
        SteeringState state = new SteeringState();
        state.adoptPainted(Set.of(INSIDE));
        helper.assertTrue(state.dismissPainted(Set.of(INSIDE)), "dismiss wants the painted zone");
        helper.assertTrue(state.zones().isEmpty(), "the zone is gone");
        state.adoptPainted(Set.of(INSIDE));
        helper.assertTrue(state.zones().isEmpty(), "the same painting is ignored while dismissed");
        state.adoptPainted(Set.of(OTHER));
        helper.assertTrue(state.zones().size() == 1, "a different painting is adopted again");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void anEmptyPaintingChangesNothing(GameTestHelper helper) {
        SteeringState state = new SteeringState();
        state.adoptPainted(Set.of());
        helper.assertTrue(state.zones().isEmpty(), "nothing painted, nothing to adopt");
        helper.assertTrue(!state.dirty(), "and nothing to write");
        helper.succeed();
    }
}