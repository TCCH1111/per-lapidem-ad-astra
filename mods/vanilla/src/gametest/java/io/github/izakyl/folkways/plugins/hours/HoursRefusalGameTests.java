package io.github.izakyl.folkways.plugins.hours;

import io.github.izakyl.folkways.FolkwaysMod;
import io.github.izakyl.folkways.plugins.FolkwaysVanillaMod;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * PLA: WP-5 T5 — the words the timetable uses when it stops the work, and the flag that tells the
 * diagnosis that waiting will help.
 */
@EventBusSubscriber(modid = FolkwaysVanillaMod.ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HoursRefusalGameTests {

    private static final String TEMPLATE = "empty";

    private HoursRefusalGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(HoursRefusalGameTests.class);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void theRestingRefusalNamesItsOwnKey(GameTestHelper helper) {
        String key = HoursRefusal.RESTING.translationKey();
        helper.assertTrue(key.equals("folkways.refusal.hours.resting"), "unexpected key: " + key);
        helper.assertTrue(!HoursRefusal.RESTING.lasting(),
            "the clock comes round by itself, so waiting answers it");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void theWordResolvesAtRuntime(GameTestHelper helper) {
        String key = HoursRefusal.RESTING.translationKey();
        String shown = Component.translatable(key).getString();
        helper.assertTrue(!shown.isBlank() && !shown.equals(key),
            "the language table has no words for " + key + ", got: " + shown);
        helper.succeed();
    }
}