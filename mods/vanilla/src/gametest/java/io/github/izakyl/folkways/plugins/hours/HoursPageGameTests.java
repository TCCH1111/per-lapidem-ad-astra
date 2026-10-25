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
 * PLA: WP-5 T4 — the page stores its pick as the item that stands for it, and every word the page
 * shows exists in the language table.
 */
@EventBusSubscriber(modid = FolkwaysVanillaMod.ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HoursPageGameTests {

    private static final String TEMPLATE = "empty";

    private static final String[] PAGE_KEYS = {
        "folkways.page.hours",
        "folkways.page.hours.status",
        "folkways.page.hours.next",
        "folkways.page.hours.off",
        "folkways.page.hours.preset.free",
        "folkways.page.hours.preset.day_shift",
        "folkways.page.hours.preset.night_shift",
        "folkways.page.hours.preset.siege",
        "folkways.settings.hours.enabled",
        "folkways.settings.hours.preset"
    };

    private HoursPageGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(HoursPageGameTests.class);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void thePickIsTheItemThatStandsForIt(GameTestHelper helper) {
        helper.assertTrue(HoursPreset.named("clock") == HoursPreset.FREE, "the clock means free");
        helper.assertTrue(HoursPreset.named("daylight_detector") == HoursPreset.DAY_SHIFT, "the detector means day");
        helper.assertTrue(HoursPreset.named("lantern") == HoursPreset.NIGHT_SHIFT, "the lantern means night");
        helper.assertTrue(HoursPreset.named("iron_sword") == HoursPreset.SIEGE, "the sword means siege");
        helper.assertTrue(HoursPreset.named("something_else") == HoursPreset.FREE, "anything else falls back to free");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void everyWordThePageShowsResolves(GameTestHelper helper) {
        for (String key : PAGE_KEYS) {
            String shown = Component.translatable(key).getString();
            helper.assertTrue(!shown.isBlank() && !shown.equals(key),
                "the language table has no words for " + key + ", got: " + shown);
        }
        helper.succeed();
    }
}