package io.github.izakyl.folkways.plugins.health;

import io.github.izakyl.folkways.FolkwaysMod;
import io.github.izakyl.folkways.base.persist.Reader;
import io.github.izakyl.folkways.plugins.FolkwaysVanillaMod;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * PLA: M2 R0 — the medical book: wounds exist, blood runs, and nothing changes for a colony without
 * wounds.
 */
@EventBusSubscriber(modid = FolkwaysVanillaMod.ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HealthStateGameTests {

    private static final String TEMPLATE = "empty";

    private HealthStateGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(HealthStateGameTests.class);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aWoundSurvivesASave(GameTestHelper helper) {
        HealthState state = new HealthState();
        UUID who = UUID.randomUUID();
        state.wound(who, new HealthState.Wound("left arm", "cut", 3, "wolf", 120L, false));

        HealthState back = HealthState.load(Reader.of(state.save()));
        HealthState.Wound one = back.wounds(who).get(0);
        helper.assertTrue(one.part().equals("left arm") && one.kind().equals("cut"),
            "the wound kept its place and kind: " + one);
        helper.assertTrue(one.severity() == 3 && one.source().equals("wolf") && one.since() == 120L,
            "and its severity, source and time: " + one);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void anOpenWoundBleedsAndATreatedOneDoesNot(GameTestHelper helper) {
        HealthState state = new HealthState();
        UUID who = UUID.randomUUID();
        state.wound(who, new HealthState.Wound("torso", "gash", 4, "axe", 0L, false));

        double before = state.blood(who);
        double after = state.bleed(who, 100L);
        helper.assertTrue(after < before, "an open wound costs blood: " + before + " -> " + after);

        helper.assertTrue(state.treat(who, "torso"), "the wound can be treated");
        double treated = state.bleed(who, 100L);
        helper.assertTrue(Math.abs(treated - after) < 1e-9, "a treated wound stops bleeding");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void bloodStopsAtZeroAndNoWoundChangesNothing(GameTestHelper helper) {
        HealthState state = new HealthState();
        UUID who = UUID.randomUUID();
        UUID quiet = UUID.randomUUID();
        state.wound(who, new HealthState.Wound("head", "blunt", 5, "fall", 0L, false));

        double floor = state.bleed(who, 100_000L);
        helper.assertTrue(floor == 0.0, "blood floors at zero, got " + floor);
        helper.assertTrue(state.blood(quiet) == HealthState.MAX_BLOOD, "an unwounded resident is untouched");
        helper.assertTrue(!state.hurt(quiet), "and is not hurt");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void severityDecidesWhoStopsWork(GameTestHelper helper) {
        // Owner's rule C: light injuries work on, severe ones are not given planned labour.
        HealthState book = new HealthState();
        UUID light = UUID.randomUUID();
        UUID heavy = UUID.randomUUID();
        book.wound(light, new HealthState.Wound("finger", "cut", 1, "knife", 0L, false));
        book.wound(heavy, new HealthState.Wound("torso", "gash", 4, "axe", 0L, false));

        helper.assertTrue(!book.severe(light), "a cut finger is not severe");
        helper.assertTrue(book.severe(heavy), "a deep gash is");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void bloodAloneCanMakeSomebodySevere(GameTestHelper helper) {
        HealthState book = new HealthState();
        UUID drained = UUID.randomUUID();
        book.wound(drained, new HealthState.Wound("arm", "cut", 2, "fall", 0L, false));
        book.bleed(drained, 100_000L); // 流血到接近零

        helper.assertTrue(book.blood(drained) < HealthState.MAX_BLOOD * HealthState.SEVERE_BLOOD_FRACTION,
            "the blood is low enough to count");
        helper.assertTrue(book.severe(drained), "so the resident is severe even with a small wound");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void anUnwoundedResidentIsNeverSevere(GameTestHelper helper) {
        HealthState book = new HealthState();
        UUID quiet = UUID.randomUUID();
        helper.assertTrue(!book.severe(quiet), "no wound, full blood, no restriction");
        helper.succeed();
    }
}
