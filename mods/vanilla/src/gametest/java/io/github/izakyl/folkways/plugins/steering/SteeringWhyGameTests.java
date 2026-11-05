package io.github.izakyl.folkways.plugins.steering;

import io.github.izakyl.folkways.FolkwaysMod;
import io.github.izakyl.folkways.core.engine.plan.Unassigned;
import io.github.izakyl.folkways.plugins.FolkwaysVanillaMod;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * PLA: WP-4 T7 — the engine's ZONE_FORBIDDEN becomes exactly one refusal, and every word it shows
 * exists in the shipped language file, so a row cannot point at a key nobody wrote.
 */
@EventBusSubscriber(modid = FolkwaysVanillaMod.ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SteeringWhyGameTests {

    private static final String TEMPLATE = "empty";

    private SteeringWhyGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(SteeringWhyGameTests.class);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void theZoneReasonBecomesTheNamedRefusal(GameTestHelper helper) {
        helper.assertTrue(SteeringContent.why(Unassigned.Reason.ZONE_FORBIDDEN).orElseThrow()
            == SteeringRefusal.FORBIDDEN, "a zone refusal maps to the steering refusal");
        helper.assertTrue(SteeringContent.why(Unassigned.Reason.NO_WAY).isEmpty(),
            "another reason is not ours to explain");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void everyWordItShowsResolvesAtRuntime(GameTestHelper helper) {
        for (SteeringRefusal refusal : SteeringRefusal.values()) {
            String key = refusal.translationKey();
            String shown = net.minecraft.network.chat.Component.translatable(key).getString();
            helper.assertTrue(!shown.isBlank() && !shown.equals(key),
                "the language table has no words for " + key + ", got: " + shown);
        }
        helper.succeed();
    }

}