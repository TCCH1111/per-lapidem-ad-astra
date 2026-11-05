package io.github.izakyl.folkways.plugins.explain;

import io.github.izakyl.folkways.FolkwaysMod;
import io.github.izakyl.folkways.core.engine.plan.Unassigned;
import io.github.izakyl.folkways.plugins.FolkwaysVanillaMod;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * PLA: WP-6 W7 — a reason we have never seen the engine produce does not get shown to the player,
 * even though we have words for it. Telling a story that never happens is worse than silence.
 */
@EventBusSubscriber(modid = FolkwaysVanillaMod.ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ExplanationHonestyGameTests {

    private static final String TEMPLATE = "empty";
    private static final UUID NODE = UUID.randomUUID();

    private ExplanationHonestyGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(ExplanationHonestyGameTests.class);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void theProvenReasonsAreShown(GameTestHelper helper) {
        for (Unassigned.Reason reason : new Unassigned.Reason[] {
                Unassigned.Reason.ZONE_FORBIDDEN, Unassigned.Reason.HOURS_RESTING,
                Unassigned.Reason.NO_WAY, Unassigned.Reason.WAY_PENDING,
                Unassigned.Reason.SAME_WORKER_SPLIT}) {
            helper.assertTrue(Explanation.shown(reason), reason + " has evidence and should be shown");
            helper.assertTrue(Explanation.row(new Unassigned(NODE, reason)).isPresent(),
                reason + " should produce a row");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void theUnprovenReasonsAreSilent(GameTestHelper helper) {
        for (Unassigned.Reason reason : new Unassigned.Reason[] {
                Unassigned.Reason.NO_LICENCE, Unassigned.Reason.PACK_FULL, Unassigned.Reason.CLAIM_TAKEN,
                Unassigned.Reason.BEYOND_HORIZON}) {
            helper.assertTrue(!Explanation.shown(reason), reason + " has no evidence and should stay quiet");
            helper.assertTrue(Explanation.row(new Unassigned(NODE, reason)).isEmpty(),
                reason + " should not produce a row");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aNamelessReasonIsSilent(GameTestHelper helper) {
        helper.assertTrue(!Explanation.shown(null), "no reason, nothing to say");
        helper.succeed();
    }
}