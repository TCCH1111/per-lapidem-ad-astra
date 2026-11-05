package io.github.izakyl.folkways.plugins.hours;

import io.github.izakyl.folkways.FolkwaysMod;
import io.github.izakyl.folkways.core.api.terms.RefusalKind;
import io.github.izakyl.folkways.core.api.work.Veto;
import io.github.izakyl.folkways.core.api.work.Vetoes;
import io.github.izakyl.folkways.core.engine.plan.Unassigned;
import io.github.izakyl.folkways.plugins.FolkwaysVanillaMod;
import java.util.Optional;
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
 * PLA: WP-5 D8 (approved L3) — a refusal now says who refused, so the schedule records the right
 * reason instead of blaming a zone for the clock.
 */
@EventBusSubscriber(modid = FolkwaysVanillaMod.ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VetoesReasonGameTests {

    private static final String TEMPLATE = "empty";
    private static final UUID RESIDENT = UUID.randomUUID();
    private static final BlockPos CELL = new BlockPos(0, 0, 0);

    private VetoesReasonGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(VetoesReasonGameTests.class);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void theScheduleReportsTheReasonTheOwnerSpokeWith(GameTestHelper helper) {
        try {
            Vetoes.reset();
            UUID colony = UUID.randomUUID();
            Vetoes.provide(HoursContent.ID, id -> (resident, cell) -> Optional.of(HoursRefusal.RESTING),
                HoursRefusal.RESTING, Unassigned.Reason.HOURS_RESTING);
            var refused = Vetoes.of(colony).check(RESIDENT, CELL).orElseThrow();
            helper.assertTrue(refused == HoursRefusal.RESTING, "the refusal is the timetable's own");
            helper.assertTrue(Vetoes.reasonOf(refused) == Unassigned.Reason.HOURS_RESTING,
                "and it is reported as hours, not as a zone");
        } finally {
            Vetoes.reset();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void twoProvidersStackAndTheFirstRefusalWins(GameTestHelper helper) {
        try {
            Vetoes.reset();
            UUID colony = UUID.randomUUID();
            Vetoes.provide(io.github.izakyl.folkways.plugins.steering.SteeringContent.ID,
                id -> (resident, cell) -> Optional.<RefusalKind>empty(),
                io.github.izakyl.folkways.plugins.steering.SteeringRefusal.FORBIDDEN,
                Unassigned.Reason.ZONE_FORBIDDEN);
            Vetoes.provide(HoursContent.ID, id -> (resident, cell) -> Optional.of(HoursRefusal.RESTING),
                HoursRefusal.RESTING, Unassigned.Reason.HOURS_RESTING);
            Veto both = Vetoes.of(colony);
            helper.assertTrue(!both.permits(RESIDENT, CELL), "either provider may refuse");
            helper.assertTrue(Vetoes.reasonOf(both.check(RESIDENT, CELL).orElseThrow())
                == Unassigned.Reason.HOURS_RESTING, "the refusing owner is the one reported");
        } finally {
            Vetoes.reset();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void anUnregisteredRefusalFallsBackToTheZoneReason(GameTestHelper helper) {
        try {
            Vetoes.reset();
            helper.assertTrue(Vetoes.reasonOf(HoursRefusal.RESTING) == Unassigned.Reason.ZONE_FORBIDDEN,
                "with nothing registered the vocabulary still resolves to something");
        } finally {
            Vetoes.reset();
        }
        helper.succeed();
    }
}