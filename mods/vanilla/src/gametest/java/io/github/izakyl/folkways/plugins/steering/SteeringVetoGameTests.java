package io.github.izakyl.folkways.plugins.steering;

import io.github.izakyl.folkways.FolkwaysMod;
import io.github.izakyl.folkways.core.api.work.Veto;
import io.github.izakyl.folkways.core.api.work.Vetoes;
import io.github.izakyl.folkways.plugins.FolkwaysVanillaMod;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
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
public final class SteeringVetoGameTests {

    private static final String TEMPLATE = "empty";
    private static final BlockPos INSIDE = new BlockPos(0, 0, 0);
    private static final BlockPos OUTSIDE = new BlockPos(9, 0, 9);
    private static final UUID RESIDENT = UUID.randomUUID();

    private SteeringVetoGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(SteeringVetoGameTests.class);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aForbidZoneBecomesAStandingThatRefusesItsCells(GameTestHelper helper) {
        SteeringState state = new SteeringState();
        state.put(ZoneRule.of("no entry", ZoneRule.Kind.FORBID, List.of(), 0, Set.of(INSIDE)));
        Veto standing = SteeringContent.standingOf(state);
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
            Vetoes.provide(SteeringContent.ID,
                id -> id.equals(colony) ? SteeringContent.standingOf(state) : Veto.ALL,
                SteeringRefusal.FORBIDDEN, io.github.izakyl.folkways.core.engine.plan.Unassigned.Reason.ZONE_FORBIDDEN);
            helper.assertTrue(!Vetoes.of(colony).permits(RESIDENT, INSIDE),
                "the registry returns this colony's zone standing");
            helper.assertTrue(Vetoes.of(UUID.randomUUID()).permits(RESIDENT, INSIDE),
                "and another colony is unaffected");
        } finally {
            Vetoes.reset();
        }
        helper.assertTrue(Vetoes.of(colony).permits(RESIDENT, INSIDE),
            "reset puts the engine back to allowing every cell");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void anAbsentProviderAllowsEveryCell(GameTestHelper helper) {
        Vetoes.reset();
        Veto none = Vetoes.of(UUID.randomUUID());
        helper.assertTrue(none.permits(RESIDENT, INSIDE) && none.permits(RESIDENT, OUTSIDE),
            "with no plugin installed every cell may be stood on");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void anAllowOnlyZoneNowRefusesTheOthersToo(GameTestHelper helper) {
        // PLA: M2 D20 — before this, subject-scoped zones were diagnosis-only.
        SteeringState state = new SteeringState();
        ResourceLocation farmer = ResourceLocation.fromNamespaceAndPath("folkways", "farmer");
        ResourceLocation miner = ResourceLocation.fromNamespaceAndPath("folkways", "miner");
        state.put(ZoneRule.of("farmers only", ZoneRule.Kind.ALLOW_ONLY, List.of(farmer), 0, Set.of(INSIDE)));

        Veto named = SteeringContent.standingOf(state, id -> id.equals(RESIDENT) ? farmer : miner);
        helper.assertTrue(named.permits(RESIDENT, INSIDE), "the farmer may enter");

        UUID minerId = UUID.randomUUID();
        Veto other = SteeringContent.standingOf(state, id -> id.equals(minerId) ? miner : farmer);
        helper.assertTrue(!other.permits(minerId, INSIDE), "the miner may not");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aConfinementNowKeepsItsPrisonerIn(GameTestHelper helper) {
        SteeringState state = new SteeringState();
        ResourceLocation prisoner = ResourceLocation.fromNamespaceAndPath("folkways", "prisoner");
        ResourceLocation farmer = ResourceLocation.fromNamespaceAndPath("folkways", "farmer");
        state.put(ZoneRule.of("cell", ZoneRule.Kind.CONFINE, List.of(prisoner), 0, Set.of(INSIDE)));

        Veto named = SteeringContent.standingOf(state, id -> id.equals(RESIDENT) ? prisoner : farmer);
        helper.assertTrue(named.permits(RESIDENT, INSIDE), "the prisoner may stay in the cell");
        helper.assertTrue(!named.permits(RESIDENT, OUTSIDE), "but not leave it");
        helper.assertTrue(named.permits(UUID.randomUUID(), OUTSIDE), "and a farmer outside is unaffected");
        helper.succeed();
    }
}
