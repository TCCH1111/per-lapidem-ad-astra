package io.github.izakyl.folkways.plugins.explain;

import io.github.izakyl.folkways.FolkwaysMod;
import io.github.izakyl.folkways.plugins.FolkwaysVanillaMod;
import io.github.izakyl.folkways.plugins.mood.Mood;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * PLA: M2 N5 — a resident up during the colony's rest is explained when they have a need, and left
 * unexplained when they do not: a content resident needs no excuse.
 */
@EventBusSubscriber(modid = FolkwaysVanillaMod.ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ExplanationNeedsGameTests {

    private static final String TEMPLATE = "empty";

    private ExplanationNeedsGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(ExplanationNeedsGameTests.class);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aResidentWithANeedIsExplained(GameTestHelper helper) {
        helper.assertTrue(Explanation.upDuringRest(Mood.STRESSED).isPresent(),
            "a strained resident up during rest is explained");
        helper.assertTrue(Explanation.upDuringRest(Mood.DESPERATE).isPresent(),
            "and so is one who cannot carry on");
        helper.assertTrue(Explanation.upDuringRest(Mood.CONTENT).isEmpty(),
            "a content resident needs no excuse");
        helper.assertTrue(Explanation.upDuringRest(Mood.WORRIED).isEmpty(),
            "nor does a quietly uneasy one");
        helper.assertTrue(Explanation.upDuringRest(null).isEmpty(),
            "and nothing is said about nobody");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void itsWordsResolveAtRuntime(GameTestHelper helper) {
        Explanation.Line line = Explanation.upDuringRest(Mood.DESPERATE).orElseThrow();
        for (String key : new String[] {line.titleKey(), line.adviceKey()}) {
            String shown = Component.translatable(key).getString();
            helper.assertTrue(!shown.isBlank() && !shown.equals(key),
                "the language table has no words for " + key + ", got: " + shown);
        }
        helper.succeed();
    }
}