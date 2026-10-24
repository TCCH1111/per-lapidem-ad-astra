package io.github.izakyl.folkways.core.engine.labor;

import io.github.izakyl.folkways.FolkwaysMod;
import io.github.izakyl.folkways.core.api.terms.RefusalKind;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * PLA: proves the read-only backoff view (PLAN-002 T5) reports the cooldown and never consumes it.
 */
@EventBusSubscriber(modid = FolkwaysMod.CORE_ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LaborViewsGameTests {

    private static final String TEMPLATE = "empty";
    private static final ResourceLocation URGE = ResourceLocation.fromNamespaceAndPath("folkways", "test_urge");
    private static final RefusalKind KIND = () -> "folkways.refusal.test";

    private LaborViewsGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(LaborViewsGameTests.class);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aRefusedUrgeCoolsForItsBackoff(GameTestHelper helper) {
        Urges urges = new Urges();
        urges.refused(URGE, KIND, 1000L);
        helper.assertTrue(urges.coolingLeft(URGE, 1000L) == 100L,
            "a fresh refusal backs off for BACKOFF ticks, got " + urges.coolingLeft(URGE, 1000L));
        helper.assertTrue(urges.coolingLeft(URGE, 1050L) == 50L,
            "half the backoff is left at half time, got " + urges.coolingLeft(URGE, 1050L));
        helper.assertTrue(urges.coolingLeft(URGE, 1100L) == 0L,
            "the backoff is over at its end, got " + urges.coolingLeft(URGE, 1100L));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void readingTheBackoffDoesNotClearIt(GameTestHelper helper) {
        Urges urges = new Urges();
        urges.refused(URGE, KIND, 1000L);
        long first = urges.coolingLeft(URGE, 1000L);
        long second = urges.coolingLeft(URGE, 1001L);
        helper.assertTrue(first == 100L && second == 99L,
            "each read reports the remaining time it is asked about: " + first + " / " + second);
        helper.assertTrue(urges.coolingLeft(URGE, 1002L) == 98L, "reads never consume the cooldown");
        helper.succeed();
    }
}