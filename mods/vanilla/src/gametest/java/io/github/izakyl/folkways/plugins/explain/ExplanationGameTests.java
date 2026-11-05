package io.github.izakyl.folkways.plugins.explain;

import io.github.izakyl.folkways.FolkwaysMod;
import io.github.izakyl.folkways.core.engine.plan.Unassigned;
import io.github.izakyl.folkways.plugins.FolkwaysVanillaMod;
import io.github.izakyl.folkways.plugins.hours.HoursContent;
import io.github.izakyl.folkways.plugins.steering.SteeringContent;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * PLA: WP-6 W1/W2 — every reason the schedule can give has words of its own, the two fixable ones
 * point at the page that fixes them, and every word resolves at runtime.
 */
@EventBusSubscriber(modid = FolkwaysVanillaMod.ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ExplanationGameTests {

    private static final String TEMPLATE = "empty";

    private ExplanationGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(ExplanationGameTests.class);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void everyReasonHasItsOwnWords(GameTestHelper helper) {
        Set<String> titles = new HashSet<>();
        for (Unassigned.Reason reason : Unassigned.Reason.values()) {
            Explanation.Line line = Explanation.of(reason);
            helper.assertTrue(!line.titleKey().isBlank() && !line.adviceKey().isBlank(),
                "reason " + reason + " has no words");
            helper.assertTrue(titles.add(line.titleKey()),
                "two reasons share a title: " + line.titleKey());
        }
        helper.assertTrue(titles.size() == Unassigned.Reason.values().length,
            "every reason needs a title of its own, got " + titles.size());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void theFixableOnesPointAtTheirPage(GameTestHelper helper) {
        helper.assertTrue(Explanation.of(Unassigned.Reason.ZONE_FORBIDDEN).page().orElseThrow()
            .equals(SteeringContent.ID), "a zone refusal opens the steering page");
        helper.assertTrue(Explanation.of(Unassigned.Reason.HOURS_RESTING).page().orElseThrow()
            .equals(HoursContent.ID), "an hours refusal opens the hours page");
        helper.assertTrue(Explanation.of(Unassigned.Reason.NO_TOOL).page().isEmpty(),
            "a missing tool has no page to open");
        helper.assertTrue(Explanation.of(null).titleKey().equals("folkways.explain.unknown.title"),
            "no reason falls back to the unknown words");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void everyWordResolvesAtRuntime(GameTestHelper helper) {
        for (Unassigned.Reason reason : Unassigned.Reason.values()) {
            Explanation.Line line = Explanation.of(reason);
            for (String key : new String[] {line.titleKey(), line.adviceKey()}) {
                String shown = Component.translatable(key).getString();
                helper.assertTrue(!shown.isBlank() && !shown.equals(key),
                    "no words for " + key + ", got: " + shown);
            }
        }
        Explanation.Line none = Explanation.unknown();
        for (String key : new String[] {none.titleKey(), none.adviceKey()}) {
            String shown = Component.translatable(key).getString();
            helper.assertTrue(!shown.isBlank() && !shown.equals(key), "no words for " + key);
        }
        helper.succeed();
    }
}