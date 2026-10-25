package io.github.izakyl.folkways.plugins.hours;

import io.github.izakyl.folkways.FolkwaysMod;
import io.github.izakyl.folkways.core.api.work.Standing;
import io.github.izakyl.folkways.core.api.work.Standings;
import io.github.izakyl.folkways.core.engine.plan.Unassigned;
import io.github.izakyl.folkways.plugins.FolkwaysVanillaMod;
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
 * PLA: WP-8 — two colonies must not share one timetable, and a plugin that unloads must not leave a
 * veto behind in the engine's registry.
 */
@EventBusSubscriber(modid = FolkwaysVanillaMod.ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class UnloadSafetyGameTests {

    private static final String TEMPLATE = "empty";
    private static final UUID RESIDENT = UUID.randomUUID();
    private static final BlockPos CELL = new BlockPos(0, 0, 0);

    private UnloadSafetyGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(UnloadSafetyGameTests.class);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void twoColoniesKeepTheirOwnTimetables(GameTestHelper helper) {
        HoursState resting = new HoursState();
        resting.preset(HoursPreset.DAY_SHIFT);
        resting.clock(14000L);
        resting.enabled(true);
        HoursState working = new HoursState();
        working.clock(14000L);

        helper.assertTrue(HoursContent.standingOf(resting).check(RESIDENT, CELL).isPresent(),
            "a colony off its hours refuses the cell");
        helper.assertTrue(HoursContent.standingOf(working).check(RESIDENT, CELL).isEmpty(),
            "a colony with no hours allows the very same cell");
        helper.assertTrue(HoursContent.standingOf(null).check(RESIDENT, CELL).isEmpty(),
            "a colony with no timetable at all allows it too");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aProviderCanBeLetGo(GameTestHelper helper) {
        try {
            Standings.reset();
            UUID colony = UUID.randomUUID();
            Standings.provide(HoursContent.ID, id -> HoursContent.standingOf(null),
                HoursRefusal.RESTING, Unassigned.Reason.HOURS_RESTING);
            helper.assertTrue(Standings.of(colony).check(RESIDENT, CELL).isEmpty(),
                "with a provider that allows everything, every cell is allowed");
            Standings.remove(HoursContent.ID);
            helper.assertTrue(Standings.of(colony).permits(RESIDENT, CELL),
                "after the plugin lets go, the engine allows every cell again");
            Standings.remove(HoursContent.ID);
            helper.assertTrue(Standings.of(colony).permits(RESIDENT, CELL),
                "letting go twice is harmless");
        } finally {
            Standings.reset();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aRefusingProviderStopsRefusingWhenItIsLetGo(GameTestHelper helper) {
        try {
            Standings.reset();
            UUID colony = UUID.randomUUID();
            Standings.provide(HoursContent.ID, id -> (resident, cell) -> java.util.Optional.of(HoursRefusal.RESTING),
                HoursRefusal.RESTING, Unassigned.Reason.HOURS_RESTING);
            helper.assertTrue(!Standings.of(colony).permits(RESIDENT, CELL), "the provider refuses first");
            Standings.remove(HoursContent.ID);
            helper.assertTrue(Standings.of(colony).permits(RESIDENT, CELL),
                "and stops refusing once it is gone");
        } finally {
            Standings.reset();
        }
        helper.succeed();
    }
}