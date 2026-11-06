package io.github.izakyl.folkways.plugins.relate;

import io.github.izakyl.folkways.FolkwaysMod;
import io.github.izakyl.folkways.plugins.FolkwaysVanillaMod;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * PLA: M2 R1/R2 — who warms to whom, and who rubs whom the wrong way, in one sampling pass.
 */
@EventBusSubscriber(modid = FolkwaysVanillaMod.ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RelationContentGameTests {

    private static final String TEMPLATE = "empty";

    private RelationContentGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(RelationContentGameTests.class);
    }

    private static RelationContent.Spot spot(UUID who, double x, boolean working) {
        return new RelationContent.Spot(who, x, 64.0, 0.0, working);
    }

    private static java.util.function.Predicate<UUID> never() {
        return who -> false;
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void workingSideBySideMakesFriends(GameTestHelper helper) {
        RelationState book = new RelationState();
        UUID one = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        RelationContent.Sample result = RelationContent.sample(
            List.of(spot(one, 0.0, true), spot(other, 3.0, true)), book, 0L, never());

        helper.assertTrue(result.companions() == 1, "one pair warmed, got " + result.companions());
        helper.assertTrue(book.bond(one, other) == 1 && book.bond(other, one) == 1,
            "and both sides remember it");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void standingApartOrIdleCountsForNothing(GameTestHelper helper) {
        RelationState book = new RelationState();
        UUID one = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID third = UUID.randomUUID();

        helper.assertTrue(RelationContent.sample(
            List.of(spot(one, 0.0, true), spot(other, 10.0, true)), book, 0L, never()).companions() == 0,
            "ten blocks apart is not working together");
        helper.assertTrue(RelationContent.sample(
            List.of(spot(one, 0.0, true), spot(third, 1.0, false)), book, 0L, never()).companions() == 0,
            "and standing next to someone idle counts for nothing");
        helper.assertTrue(book.bond(one, other) == 0 && book.bond(one, third) == 0,
            "so no bonds were made");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void samplingTwiceInHalfAnHourCountsOnce(GameTestHelper helper) {
        RelationState book = new RelationState();
        UUID one = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        List<RelationContent.Spot> near = List.of(spot(one, 0.0, true), spot(other, 2.0, true));

        helper.assertTrue(RelationContent.sample(near, book, 0L, never()).companions() == 1,
            "the first sample counts");
        helper.assertTrue(RelationContent.sample(near, book, 100L, never()).companions() == 0,
            "the next sample does not");
        helper.assertTrue(book.bond(one, other) == 1, "so the bond stays at one");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aResidentDoesNotBondWithThemselves(GameTestHelper helper) {
        RelationState book = new RelationState();
        UUID one = UUID.randomUUID();
        helper.assertTrue(RelationContent.sample(
            List.of(spot(one, 0.0, true), spot(one, 1.0, true)), book, 0L, never()).companions() == 0,
            "the same resident twice is not a pair");
        helper.assertTrue(book.size(one) == 0, "and nothing was recorded");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aHardDayRubsOffOnWhoeverIsBesideIt(GameTestHelper helper) {
        RelationState book = new RelationState();
        UUID tried = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        // The PRD's rule: being in a bad way makes people short with those beside them (R2-A).
        RelationContent.Sample result = RelationContent.sample(
            List.of(spot(tried, 0.0, true), spot(other, 3.0, true)), book, 0L,
            who -> who.equals(tried));

        helper.assertTrue(result.frictions() == 1, "the pair rubbed, got " + result.frictions());
        helper.assertTrue(book.grudge(tried, other) == 1 && book.grudge(other, tried) == 1,
            "and both remember the friction");
        helper.assertTrue(book.bond(tried, other) == 1, "while the friendship still counted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aGoodDayMakesNoFriction(GameTestHelper helper) {
        RelationState book = new RelationState();
        UUID one = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        RelationContent.Sample result = RelationContent.sample(
            List.of(spot(one, 0.0, true), spot(other, 3.0, true)), book, 0L, never());

        helper.assertTrue(result.frictions() == 0, "nobody rubbed anybody, got " + result.frictions());
        helper.assertTrue(book.grudge(one, other) == 0, "so no grudge was written");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void frictionNeedsProximityToo(GameTestHelper helper) {
        RelationState book = new RelationState();
        UUID tried = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        RelationContent.Sample result = RelationContent.sample(
            List.of(spot(tried, 0.0, true), spot(other, 10.0, true)), book, 0L, who -> true);

        helper.assertTrue(result.frictions() == 0, "ten blocks apart is not rubbing anybody");
        helper.assertTrue(book.grudge(tried, other) == 0, "so no grudge was written");
        helper.succeed();
    }
}
