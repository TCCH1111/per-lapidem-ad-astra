package io.github.izakyl.folkways.plugins.persistence;

import io.github.izakyl.folkways.FolkwaysMod;
import io.github.izakyl.folkways.base.persist.Reader;
import io.github.izakyl.folkways.core.api.colony.Colonies;
import io.github.izakyl.folkways.core.api.colony.Colony;
import io.github.izakyl.folkways.plugins.FolkwaysVanillaMod;
import io.github.izakyl.folkways.plugins.hours.HoursContent;
import io.github.izakyl.folkways.plugins.hours.HoursPreset;
import io.github.izakyl.folkways.plugins.hours.HoursState;
import io.github.izakyl.folkways.plugins.steering.SteeringContent;
import io.github.izakyl.folkways.plugins.steering.SteeringState;
import io.github.izakyl.folkways.plugins.steering.ZoneRule;
import java.util.List;
import java.util.Optional;
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
 * PLA: WP-8 — what a plugin keeps lives on the colony the server hands out, not on a copy: a colony
 * read back from the server by its id still carries our two datastores.
 *
 * <p>This is as far as a game test can go: it proves the data is held by the level-backed colony
 * registry rather than by our own object, and the process-restart half stays a manual pre-release
 * check (D11).
 */
@EventBusSubscriber(modid = FolkwaysVanillaMod.ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ColonyLevelBackedGameTests {

    private static final String TEMPLATE = "empty";

    private ColonyLevelBackedGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(ColonyLevelBackedGameTests.class);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aColonyReadBackFromTheServerStillCarriesOurData(GameTestHelper helper) {
        Colony minted = Colonies.mint(helper.getLevel().getServer());
        SteeringState zones = new SteeringState();
        zones.put(ZoneRule.of("no entry", ZoneRule.Kind.FORBID, List.of(), 0, Set.of(new BlockPos(2, 2, 2))));
        minted.keep(SteeringContent.ID, zones.save());
        HoursState hours = new HoursState();
        hours.enabled(true);
        hours.preset(HoursPreset.NIGHT_SHIFT);
        hours.clock(15000L);
        minted.keep(HoursContent.ID, hours.save());

        Optional<Colony> readBack = Colonies.of(helper.getLevel().getServer(), minted.id());
        helper.assertTrue(readBack.isPresent(), "the server hands the colony back by its id");
        Colony again = readBack.orElseThrow();
        helper.assertTrue(SteeringState.load(Reader.of(again.kept(SteeringContent.ID))).forbids(new BlockPos(2, 2, 2)),
            "the zone came back with the colony");
        HoursState back = HoursState.load(Reader.of(again.kept(HoursContent.ID)));
        helper.assertTrue(back.enabled() && back.preset() == HoursPreset.NIGHT_SHIFT,
            "and so did the timetable");
        helper.succeed();
    }
}