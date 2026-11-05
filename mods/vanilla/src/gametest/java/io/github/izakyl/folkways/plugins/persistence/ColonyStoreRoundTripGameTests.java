package io.github.izakyl.folkways.plugins.persistence;

import io.github.izakyl.folkways.FolkwaysMod;
import io.github.izakyl.folkways.base.persist.Reader;
import io.github.izakyl.folkways.core.engine.colony.ColonyData;
import io.github.izakyl.folkways.plugins.FolkwaysVanillaMod;
import io.github.izakyl.folkways.plugins.hours.HoursContent;
import io.github.izakyl.folkways.plugins.hours.HoursPreset;
import io.github.izakyl.folkways.plugins.hours.HoursState;
import io.github.izakyl.folkways.plugins.steering.SteeringContent;
import io.github.izakyl.folkways.plugins.steering.SteeringState;
import io.github.izakyl.folkways.plugins.steering.ZoneRule;
import java.util.List;
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
 * PLA: WP-8 — our two datastores really do go through the colony's own kept bag: what a plugin writes
 * with keep is what it reads back from kept, not a copy kept somewhere else in memory.
 */
@EventBusSubscriber(modid = FolkwaysVanillaMod.ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ColonyStoreRoundTripGameTests {

    private static final String TEMPLATE = "empty";

    private ColonyStoreRoundTripGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(ColonyStoreRoundTripGameTests.class);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void theZonesGoThroughTheColonyBag(GameTestHelper helper) {
        ColonyData data = new ColonyData();
        SteeringState before = new SteeringState();
        before.put(ZoneRule.of("no entry", ZoneRule.Kind.FORBID, List.of(), 0,
            Set.of(new BlockPos(1, 2, 3), new BlockPos(4, 5, 6))));
        data.keeps().keep(SteeringContent.ID, before.save());

        SteeringState after = SteeringState.load(Reader.of(data.keeps().kept(SteeringContent.ID)));
        helper.assertTrue(after.zones().size() == 1, "one zone came back, got " + after.zones().size());
        helper.assertTrue(after.forbids(new BlockPos(1, 2, 3)), "and it still forbids its cells");
        helper.assertTrue(!after.forbids(new BlockPos(9, 9, 9)), "and nothing else");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void theTimetableGoesThroughTheColonyBag(GameTestHelper helper) {
        ColonyData data = new ColonyData();
        HoursState before = new HoursState();
        before.enabled(true);
        before.preset(HoursPreset.NIGHT_SHIFT);
        before.clock(15000L);
        data.keeps().keep(HoursContent.ID, before.save());

        HoursState after = HoursState.load(Reader.of(data.keeps().kept(HoursContent.ID)));
        helper.assertTrue(after.enabled(), "the switch came back on");
        helper.assertTrue(after.preset() == HoursPreset.NIGHT_SHIFT, "the shift came back");
        helper.assertTrue(after.working(), "and the clock with it");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void anUntouchedColonyReadsAsEmpty(GameTestHelper helper) {
        ColonyData data = new ColonyData();
        SteeringState zones = SteeringState.load(Reader.of(data.keeps().kept(SteeringContent.ID)));
        HoursState hours = HoursState.load(Reader.of(data.keeps().kept(HoursContent.ID)));
        helper.assertTrue(zones.zones().isEmpty(), "a colony nobody configured has no zones");
        helper.assertTrue(!hours.enabled(), "and keeps no hours");
        helper.assertTrue(hours.working(), "so every hour is a working hour");
        helper.succeed();
    }
}