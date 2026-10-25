package io.github.izakyl.folkways.plugins.hours;

import io.github.izakyl.folkways.FolkwaysMod;
import io.github.izakyl.folkways.core.api.work.Standings;
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
 * PLA: WP-5 T1 — the timetable is arithmetic, and a colony with it turned off works as before.
 */
@EventBusSubscriber(modid = FolkwaysVanillaMod.ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HoursGameTests {

    private static final String TEMPLATE = "empty";

    private HoursGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(HoursGameTests.class);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void theDayShiftWorksByDayAndRestsByNight(GameTestHelper helper) {
        HoursPreset day = HoursPreset.DAY_SHIFT;
        helper.assertTrue(day.working(0), "sunrise is work");
        helper.assertTrue(day.working(11999), "the last tick of the morning is work");
        helper.assertTrue(!day.working(12000), "dusk starts the break");
        helper.assertTrue(!day.working(13799), "the break runs to the end of its segment");
        helper.assertTrue(!day.working(13800), "night is rest");
        helper.assertTrue(!day.working(23999), "and it stays rest until sunrise");
        helper.assertTrue(day.working(24000), "the next day begins with work");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void theFreeAndSiegePresetsNeverRest(GameTestHelper helper) {
        for (long hour : new long[] {0L, 6000L, 12000L, 18000L, 23000L}) {
            helper.assertTrue(HoursPreset.FREE.working(hour), "free works at " + hour);
            helper.assertTrue(HoursPreset.SIEGE.working(hour), "siege works at " + hour);
        }
        helper.assertTrue(HoursPreset.FREE.nextChange(0L) == 0L, "free never changes");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void theClockSaysWhenItWillChange(GameTestHelper helper) {
        helper.assertTrue(HoursPreset.DAY_SHIFT.nextChange(6000L) == 6000L, "noon is six hours from dusk");
        helper.assertTrue(HoursPreset.DAY_SHIFT.nextChange(12000L) == 12000L, "dusk is twelve hours from sunrise");
        helper.assertTrue(HoursPreset.NIGHT_SHIFT.nextChange(6000L) == 7800L, "the night shift starts at 13800");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aColonyWithHoursOffKeepsWorking(GameTestHelper helper) {
        HoursState state = new HoursState();
        state.clock(13800L);
        helper.assertTrue(!state.enabled(), "hours start off");
        helper.assertTrue(state.working(), "with hours off nothing is refused, whatever the clock says");
        state.preset(HoursPreset.DAY_SHIFT);
        state.enabled(true);
        helper.assertTrue(!state.working(), "turning the day shift on at dusk stops the work");
        state.preset(HoursPreset.SIEGE);
        helper.assertTrue(state.working(), "and siege overrides the clock");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aTimetableSurvivesASaveAndLoad(GameTestHelper helper) {
        HoursState before = new HoursState();
        before.enabled(true);
        before.preset(HoursPreset.NIGHT_SHIFT);
        before.clock(15000L);
        HoursState after = HoursState.load(io.github.izakyl.folkways.base.persist.Reader.of(before.save()));
        helper.assertTrue(after.enabled(), "the switch survived");
        helper.assertTrue(after.preset() == HoursPreset.NIGHT_SHIFT, "the preset survived");
        helper.assertTrue(after.working(), "the clock survived with the timetable");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void anEmptyRegistryRefusesNothing(GameTestHelper helper) {
        try {
            Standings.reset();
            UUID colony = UUID.randomUUID();
            UUID resident = UUID.randomUUID();
            helper.assertTrue(Standings.of(colony).permits(resident, BlockPos.ZERO),
                "no provider means every cell may be stood on");
        } finally {
            Standings.reset();
        }
        helper.succeed();
    }
}