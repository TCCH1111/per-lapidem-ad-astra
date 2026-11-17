package io.github.izakyl.folkways.plugins.threat;

import io.github.izakyl.folkways.FolkwaysMod;
import io.github.izakyl.folkways.plugins.FolkwaysVanillaMod;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * PLA: M2 T1/T2 — the owner's A/B/A rule for resolving a threat, tested as pure arithmetic so a
 * crisis is reproducible without a world.
 */
@EventBusSubscriber(modid = FolkwaysVanillaMod.ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ThreatResolutionGameTests {

    private static final String TEMPLATE = "empty";

    private ThreatResolutionGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(ThreatResolutionGameTests.class);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aPreparedColonyHoldsWithNoLoss(GameTestHelper helper) {
        ThreatState.Resolution r = ThreatState.resolve(30, 30);
        helper.assertTrue(r.held(), "equal strength and readiness hold the line");
        helper.assertTrue(r.loss() == 0 && r.severity() == 0, "and cost nobody anything");
        helper.assertTrue(ThreatState.resolve(20, 30).held(), "more readiness than strength also holds");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void lossIsTheProportionalShortfall(GameTestHelper helper) {
        // OQ-TH2 = B: losses scale with the gap, and the same gap always gives the same answer.
        helper.assertTrue(ThreatState.resolve(30, 10).loss() == 20, "loss is strength minus readiness");
        helper.assertTrue(ThreatState.resolve(30, 25).loss() == 5, "a smaller gap costs less");
        helper.assertTrue(ThreatState.resolve(30, 10).equals(ThreatState.resolve(30, 10)),
            "and the outcome is deterministic — OQ-TH1 = A");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void evenTotalDefeatCapsBelowSevere(GameTestHelper helper) {
        // OQ-TH3 = A: a raid can hurt but never kills outright; "severe" starts at 4.
        ThreatState.Resolution worst = ThreatState.resolve(100, 0);
        helper.assertTrue(!worst.held(), "an unprepared colony does not hold");
        helper.assertTrue(worst.severity() > 0 && worst.severity() < 4,
            "the wound is real but stays below severe, got " + worst.severity());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void readinessCountsOnlyWhatCanBePointedTo(GameTestHelper helper) {
        helper.assertTrue(ThreatState.readiness(4, 4, 4, 3) == 4 + 2 + 2 + 3,
            "militia, weapons, barriers and warning days each contribute a countable share");
        helper.assertTrue(ThreatState.readiness(0, 0, 0, 0) == 0, "nothing prepared is zero readiness");
        helper.assertTrue(ThreatState.readiness(0, 0, 0, 99) == 5, "warning time is capped, not open-ended");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void settlingOnlyTouchesThoseAtTheGates(GameTestHelper helper) {
        ThreatState book = new ThreatState();
        book.foretell("raiders", 10, 0L, 1);
        book.foretell("raiders", 10, 0L, 5);
        book.advance(24_000L);
        helper.assertTrue(book.all().stream().filter(t -> t.stage() == ThreatState.Stage.AT_THE_GATES).count() == 1,
            "one has arrived");
        book.settle();
        helper.assertTrue(book.all().stream().filter(t -> t.stage() == ThreatState.Stage.RESOLVED).count() == 1,
            "and only the arrived one is resolved");
        helper.assertTrue(book.pending().isPresent(), "the second is still on its way");
        helper.succeed();
    }
}
