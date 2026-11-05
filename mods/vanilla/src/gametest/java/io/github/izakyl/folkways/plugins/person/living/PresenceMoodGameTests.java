package io.github.izakyl.folkways.plugins.person.living;

import io.github.izakyl.folkways.FolkwaysMod;
import io.github.izakyl.folkways.plugins.FolkwaysVanillaMod;
import io.github.izakyl.folkways.plugins.mood.Mood;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * PLA: M2 N3 — the read-out a colony asks for is the mood the needs describe, and asking about
 * nothing at all is answered rather than thrown.
 */
@EventBusSubscriber(modid = FolkwaysVanillaMod.ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PresenceMoodGameTests {

    private static final String TEMPLATE = "empty";

    private PresenceMoodGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(PresenceMoodGameTests.class);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void theReadOutFollowsTheNeeds(GameTestHelper helper) {
        Mood.Needs full = new Mood.Needs(20, 20, false, 1200, 1200, false, false, true);
        Mood.Needs hungry = new Mood.Needs(4, 20, true, 1200, 1200, false, false, true);
        helper.assertTrue(PresenceViews.moodOf(full) == Mood.CONTENT, "fed and rested reads content");
        helper.assertTrue(PresenceViews.moodOf(hungry) == Mood.DESPERATE, "urgent hunger reads desperate");
        helper.assertTrue(PresenceViews.moodOf(null) == Mood.CONTENT, "no snapshot at all reads content");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void askingAboutNothingIsAnswered(GameTestHelper helper) {
        helper.assertTrue(PresenceViews.mood(null, UUID.randomUUID()).isEmpty(),
            "no colony means no mood to read");
        helper.assertTrue(PresenceViews.mood(null, null).isEmpty(),
            "nor is there one for no resident");
        helper.succeed();
    }
}