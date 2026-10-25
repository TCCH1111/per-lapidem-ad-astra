package io.github.izakyl.folkways.core.engine.plan;

import io.github.izakyl.folkways.FolkwaysMod;
import io.github.izakyl.folkways.core.api.terms.ItemSpec;
import io.github.izakyl.folkways.core.api.terms.RefusalKind;
import io.github.izakyl.folkways.core.api.terms.Stash;
import io.github.izakyl.folkways.core.api.terms.Stand;
import io.github.izakyl.folkways.core.api.terms.WorldPos;
import io.github.izakyl.folkways.core.api.work.Delivery;
import io.github.izakyl.folkways.core.api.work.Grown;
import io.github.izakyl.folkways.core.api.work.Node;
import io.github.izakyl.folkways.core.api.work.Stances;
import io.github.izakyl.folkways.core.api.work.Standings;
import io.github.izakyl.folkways.core.api.work.WorkSite;
import io.github.izakyl.folkways.core.engine.plan.haul.Haul;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * PLA: WP-6 W5 — the schedule really does refuse work whose stand a veto keeps the resident off, and
 * it reports the reason that veto speaks with. This is the end-to-end proof the unit tests could not
 * give: a real planner, a real goal, and the veto wired the way the colony wires it.
 */
@EventBusSubscriber(modid = FolkwaysMod.CORE_ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StandingVetoGameTests {

    private static final String TEMPLATE = "empty";
    private static final ResourceLocation OWNER = ResourceLocation.fromNamespaceAndPath("folkways_test", "veto");
    private static final ItemSpec BREAD = ItemSpec.of(ResourceLocation.withDefaultNamespace("bread"));

    /** A refusal of our own, so this test needs nothing from another module. */
    private enum TestRefusal implements RefusalKind.Named {
        KEPT_OUT
    }

    private StandingVetoGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(StandingVetoGameTests.class);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aVetoedStandLeavesTheWorkUnassignedWithItsReason(GameTestHelper helper) {
        PlanningFixture fixture = new PlanningFixture(helper);
        List<Stash> stashes = new ArrayList<>();
        Map<Stash, Integer> room = new LinkedHashMap<>();
        List<Stock.Holding> holdings = new ArrayList<>();
        Stands.Builder stands = Stands.building();
        for (int at = 0; at < 3; at++) {
            WorldPos pos = fixture.source.at(fixture.source.cell().offset(0, 0, 2 * at));
            Stash stash = new Stash(pos);
            stashes.add(stash);
            holdings.add(new Stock.Holding(stash, new ItemStack(Items.BREAD, 4)));
            room.put(stash, 26);
            stands.container(stash, Set.of(new Stand(pos)));
        }
        stands.container(new Stash(fixture.target), Set.of(new Stand(fixture.target)));
        room.put(new Stash(fixture.target), 27);
        Node delivery = Delivery.to(UUID.randomUUID(), Haul.DOMAIN, new WorkSite.AtBlock(fixture.target),
            Stances.at(fixture.target), BREAD, 8, count -> { });
        Known known = new Known(new Situation(fixture.ways, new Stock(holdings, room), List.of(), stands.done()),
            fixture.crew.hands());

        try {
            Standings.reset();
            Standings.provide(OWNER, colony -> (resident, cell) -> Optional.of(TestRefusal.KEPT_OUT),
                TestRefusal.KEPT_OUT, Unassigned.Reason.ZONE_FORBIDDEN);

            Solving planner = new Solving(new Claims(), new Cooldowns());
            planner.standing(Standings.of(UUID.randomUUID()));
            Planned grown = planner.handle(known, List.of(new Message.Submitted(OWNER, Grown.of(delivery))), 0);

            boolean refused = grown.laid().unassigned().stream()
                .anyMatch(nobody -> nobody.why() == Unassigned.Reason.ZONE_FORBIDDEN);
            helper.assertTrue(refused, "the vetoed stand leaves the work unassigned as a zone refusal, got "
                + grown.laid().unassigned());
            helper.assertTrue(grown.laid().unassigned().stream()
                    .noneMatch(nobody -> nobody.why() == Unassigned.Reason.NO_WAY),
                "and it is not blamed on the way");
            planner.closed();
        } finally {
            Standings.reset();
        }
        helper.succeed();
    }
}
