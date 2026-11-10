package io.github.izakyl.folkways.plugins.threat;

import io.github.izakyl.folkways.FolkwaysMod;
import io.github.izakyl.folkways.base.persist.Reader;
import io.github.izakyl.folkways.plugins.FolkwaysVanillaMod;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * PLA: M2 T0 — a danger is known before it arrives, and it stops at the gates rather than pretending
 * to be resolved.
 */
@EventBusSubscriber(modid = FolkwaysVanillaMod.ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ThreatStateGameTests {

    private static final String TEMPLATE = "empty";
    private static final long DAY = 24_000L;

    private ThreatStateGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(ThreatStateGameTests.class);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aThreatIsKnownDaysBeforeItArrives(GameTestHelper helper) {
        ThreatState book = new ThreatState();
        ThreatState.Threat one = book.foretell("raiders", 30, 0L, 3);

        helper.assertTrue(one.stage() == ThreatState.Stage.FORETOLD, "it starts as a rumour");
        helper.assertTrue(one.arrivesAt() == 3 * DAY, "arriving three days out, got " + one.arrivesAt());
        helper.assertTrue(book.advance(2 * DAY).isEmpty(), "two days later it has not arrived");
        helper.assertTrue(book.pending().isPresent(), "but it is still on its way");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void arrivingStopsAtTheGates(GameTestHelper helper) {
        // The resolution rule is not agreed, so the state machine must not invent one.
        ThreatState book = new ThreatState();
        book.foretell("raiders", 30, 0L, 1);

        List<ThreatState.Threat> arrived = book.advance(DAY);
        helper.assertTrue(arrived.size() == 1, "it arrives exactly once, got " + arrived.size());
        helper.assertTrue(arrived.get(0).stage() == ThreatState.Stage.AT_THE_GATES,
            "and waits at the gates, got " + arrived.get(0).stage());
        helper.assertTrue(book.advance(2 * DAY).isEmpty(), "and does not arrive again");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aThreatSurvivesASave(GameTestHelper helper) {
        ThreatState book = new ThreatState();
        book.foretell("raiders", 30, 100L, 2);
        book.advance(100L + 2 * DAY);

        ThreatState back = ThreatState.load(Reader.of(book.save()));
        helper.assertTrue(back.all().size() == 1, "the threat came back");
        ThreatState.Threat one = back.all().get(0);
        helper.assertTrue(one.stage() == ThreatState.Stage.AT_THE_GATES, "with its stage, got " + one.stage());
        helper.assertTrue(one.strength() == 30 && one.arrivesAt() == 100L + 2 * DAY,
            "and its numbers, got " + one);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void anEmptyBookDoesNothing(GameTestHelper helper) {
        ThreatState book = new ThreatState();
        helper.assertTrue(book.pending().isEmpty(), "nothing is on its way");
        helper.assertTrue(book.advance(DAY).isEmpty(), "and nothing arrives");
        helper.succeed();
    }
}
