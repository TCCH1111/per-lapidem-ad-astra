package io.github.izakyl.folkways.plugins.explain;

import io.github.izakyl.folkways.FolkwaysMod;
import io.github.izakyl.folkways.core.engine.plan.Unassigned;
import io.github.izakyl.folkways.front.api.panel.Board;
import io.github.izakyl.folkways.plugins.FolkwaysVanillaMod;
import io.github.izakyl.folkways.plugins.hours.HoursContent;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * PLA: WP-6 W3 — work nobody took becomes exactly one row, with the reason's words and, where the
 * player can fix it, one act that opens the page that fixes it.
 */
@EventBusSubscriber(modid = FolkwaysVanillaMod.ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ExplanationRowGameTests {

    private static final String TEMPLATE = "empty";
    private static final UUID NODE = UUID.randomUUID();

    private ExplanationRowGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(ExplanationRowGameTests.class);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aRefusalBecomesOneRowWithItsWords(GameTestHelper helper) {
        Explanation.Line expected = Explanation.of(Unassigned.Reason.NO_TOOL);
        Board.Row row = Explanation.row(new Unassigned(NODE, Unassigned.Reason.NO_TOOL));
        helper.assertTrue(row.title().getString().equals(
            net.minecraft.network.chat.Component.translatable(expected.titleKey()).getString()),
            "the row shows the reason's title");
        helper.assertTrue(row.detail().getString().equals(
            net.minecraft.network.chat.Component.translatable(expected.adviceKey()).getString()),
            "and its advice as the detail");
        helper.assertTrue(row.acts().isEmpty(), "a missing tool has nothing to open");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void theFixableRefusalCarriesOneAct(GameTestHelper helper) {
        Board.Row row = Explanation.row(new Unassigned(NODE, Unassigned.Reason.HOURS_RESTING));
        helper.assertTrue(row.acts().size() == 1, "one act, got " + row.acts().size());
        boolean opensHours = row.acts().getFirst() instanceof Board.Act.Open open
            && open.page().equals(HoursContent.ID);
        helper.assertTrue(opensHours, "the act opens the hours page");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aPageOfRefusalsStaysWithinTheActBudget(GameTestHelper helper) {
        Board.Row[] rows = new Board.Row[] {
            Explanation.row(new Unassigned(NODE, Unassigned.Reason.ZONE_FORBIDDEN)),
            Explanation.row(new Unassigned(NODE, Unassigned.Reason.NO_WAY)),
            Explanation.row(new Unassigned(NODE, Unassigned.Reason.PACK_FULL))
        };
        Board board = Board.of(java.util.List.of(rows));
        for (Board.Row row : board.rows()) {
            helper.assertTrue(row.acts().size() <= Board.Row.MAX_ACTS,
                "a row may offer at most " + Board.Row.MAX_ACTS + " acts");
        }
        helper.succeed();
    }
}