package io.github.izakyl.folkways.plugins.relate;

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
 * PLA: M2 R1-4 — the card line appears only when there is company to report, and its words exist in
 * every shipped language.
 */
@EventBusSubscriber(modid = FolkwaysVanillaMod.ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RelationFacingGameTests {

    private static final String TEMPLATE = "empty";

    private RelationFacingGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(RelationFacingGameTests.class);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void theLineOnlyAppearsWithCompany(GameTestHelper helper) {
        helper.assertTrue(RelateFacing.linesFor(false, false).isEmpty(),
            "a resident working alone is not interesting");
        List<Line> together = RelateFacing.linesFor(true, false);
        helper.assertTrue(together.size() == 2, "with a friend beside them the card says what and why");

        Line first = RelateFacing.linesFor(false, true).get(0);
        String rivalKey = Relate.RIVAL.translationKey();
        helper.assertTrue(first instanceof Line.Said said && said.notice().key().equals(rivalKey),
            "a rival beside them is reported as such, got " + first);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void theLineHasWordsInTheLanguageTable(GameTestHelper helper) {
        for (Relate one : Relate.values()) {
            String key = one.translationKey();
            String shown = Component.translatable(key).getString();
            helper.assertTrue(!shown.isBlank() && !shown.equals(key),
                "the language table has no words for " + key + ", got: " + shown);
        }
        helper.succeed();
    }
}
