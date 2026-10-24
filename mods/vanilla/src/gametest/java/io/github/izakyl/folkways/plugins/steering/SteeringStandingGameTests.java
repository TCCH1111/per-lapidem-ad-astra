package io.github.izakyl.folkways.plugins.steering;

import io.github.izakyl.folkways.FolkwaysMod;
import io.github.izakyl.folkways.core.api.work.Standing;
import io.github.izakyl.folkways.core.api.work.Standings;
import io.github.izakyl.folkways.plugins.FolkwaysVanillaMod;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * PLA: WP-4 T4b — the bridge from a colony's steering state to the engine's Standing, and the
 * registry the engine reads it from. The registry is reset in each test so one never leaks into
 * another.
 */
@EventBusSubscriber(modid = FolkwaysVanillaMod.ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SteeringStandingGameTests {

    private static final String TEMPLATE = "empty";
    private static final BlockPos INSIDE = new BlockPos(0, 0, 0);
    private static final BlockPos OUTSIDE = new BlockPos(9, 0, 9);
    private static final UUID RESIDENT = UUID.randomUUID();

    private SteeringStandingGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(SteeringStandingGameTests.class);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aForbidZoneBecomesAStandingThatRefusesItsCells(GameTestHelper helper) {
        SteeringState state = new SteeringState();
        state.put(ZoneRule.of("no entry", ZoneRule.Kind.FORBID, List.of(), 0, Set.of(INSIDE)));
        Standing standing = SteeringContent.standingOf(state);
        helper.assertTrue(!standing.permits(RESIDENT, INSIDE), "the forbid zone refuses its cell");
        helper.assertTrue(standing.permits(RESIDENT, OUTSIDE), "and allows a cell outside it");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void theRegistryHandsTheEngineTheColonysStanding(GameTestHelper helper) {
        SteeringState state = new SteeringState();
        state.put(ZoneRule.of("no entry", ZoneRule.Kind.FORBID, List.of(), 0, Set.of(INSIDE)));
        UUID colony = UUID.randomUUID();
        try {
            Standings.provide(id -> id.equals(colony) ? SteeringContent.standingOf(state) : Standing.ALL);
            helper.assertTrue(!Standings.of(colony).permits(RESIDENT, INSIDE),
                "the registry returns this colony's zone standing");
            helper.assertTrue(Standings.of(UUID.randomUUID()).permits(RESIDENT, INSIDE),
                "and another colony is unaffected");
        } finally {
            Standings.reset();
        }
        helper.assertTrue(Standings.of(colony).permits(RESIDENT, INSIDE),
            "reset puts the engine back to allowing every cell");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void anAbsentProviderAllowsEveryCell(GameTestHelper helper) {
        Standings.reset();
        Standing none = Standings.of(UUID.randomUUID());
        helper.assertTrue(none.permits(RESIDENT, INSIDE) && none.permits(RESIDENT, OUTSIDE),
            "with no plugin installed every cell may be stood on");
        helper.succeed();
    }
}