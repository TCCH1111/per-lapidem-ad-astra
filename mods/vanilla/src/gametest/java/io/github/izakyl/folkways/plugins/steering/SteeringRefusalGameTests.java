package io.github.izakyl.folkways.plugins.steering;

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
 * PLA: WP-4 T5 — every steering refusal names its own key, and its lasting flag tells the diagnosis
 * layer whether waiting will help.
 */
@EventBusSubscriber(modid = FolkwaysVanillaMod.ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SteeringRefusalGameTests {

    private static final String TEMPLATE = "empty";

    private SteeringRefusalGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(SteeringRefusalGameTests.class);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void everyRefusalNamesItsOwnKey(GameTestHelper helper) {
        for (SteeringRefusal refusal : SteeringRefusal.values()) {
            String key = refusal.translationKey();
            helper.assertTrue(key.startsWith("folkways.refusal.steering."),
                "unexpected key for " + refusal + ": " + key);
            helper.assertTrue(key.substring("folkways.refusal.steering.".length()).equals(refusal.name().toLowerCase(java.util.Locale.ROOT)),
                "the key must follow the enum name: " + key);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aVerdictMapsToTheMatchingRefusal(GameTestHelper helper) {
        helper.assertTrue(SteeringRefusal.of(ZoneRule.Verdict.FORBIDDEN) == SteeringRefusal.FORBIDDEN, "forbidden maps");
        helper.assertTrue(SteeringRefusal.of(ZoneRule.Verdict.ONLY_FOR_OTHERS) == SteeringRefusal.ONLY_FOR_OTHERS, "only-for-others maps");
        helper.assertTrue(SteeringRefusal.of(ZoneRule.Verdict.QUOTA_FULL) == SteeringRefusal.QUOTA_FULL, "quota maps");
        try {
            SteeringRefusal.of(ZoneRule.Verdict.ALLOWED);
            helper.fail("allowed is not a refusal");
        } catch (IllegalArgumentException expected) {
            helper.succeed();
        }
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void onlyWaitingHelpDecidesWhatLasts(GameTestHelper helper) {
        helper.assertTrue(SteeringRefusal.FORBIDDEN.lasting(), "a forbid zone lasts until the player redraws it");
        helper.assertTrue(SteeringRefusal.ONLY_FOR_OTHERS.lasting(), "so does an allow-only zone");
        helper.assertTrue(!SteeringRefusal.QUOTA_FULL.lasting(), "a full quota clears as residents leave");
        helper.succeed();
    }
}