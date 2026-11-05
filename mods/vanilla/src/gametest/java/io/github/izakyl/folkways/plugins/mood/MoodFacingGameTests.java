package io.github.izakyl.folkways.plugins.mood;

import io.github.izakyl.folkways.FolkwaysMod;
import io.github.izakyl.folkways.front.api.notice.Line;
import io.github.izakyl.folkways.plugins.FolkwaysVanillaMod;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * PLA: M2 N6 — the resident card speaks only when there is something to say, and whatever it says
 * resolves to words at runtime.
 */
@EventBusSubscriber(modid = FolkwaysVanillaMod.ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MoodFacingGameTests {

    private static final String TEMPLATE = "empty";

    private MoodFacingGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(MoodFacingGameTests.class);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void onlyTroubleReachesTheCard(GameTestHelper helper) {
        helper.assertTrue(MoodFacing.linesFor(Mood.CONTENT).isEmpty(),
            "a resident getting on with it says nothing");
        helper.assertTrue(MoodFacing.linesFor(null).isEmpty(),
            "and nothing is said about nobody");
        for (Mood mood : List.of(Mood.WORRIED, Mood.STRESSED, Mood.DESPERATE)) {
            List<Line> lines = MoodFacing.linesFor(mood);
            helper.assertTrue(lines.size() == 1, mood + " puts exactly one line on the card");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void theCardLinesResolveAtRuntime(GameTestHelper helper) {
        for (Mood mood : Mood.values()) {
            String key = mood.translationKey();
            String shown = Component.translatable(key).getString();
            helper.assertTrue(!shown.isBlank() && !shown.equals(key),
                "the language table has no words for " + key + ", got: " + shown);
        }
        helper.succeed();
    }
}
