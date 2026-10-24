package io.github.izakyl.folkways.plugins.person.living;

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
 * PLA: proves the read-only hunger/bed views (PLAN-002 T2) map the package-private state without
 * touching it. The colony lookup itself is the same call LivingContent.exerted uses.
 */
@EventBusSubscriber(modid = FolkwaysVanillaMod.ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PresenceViewsGameTests {

    private static final String TEMPLATE = "empty";

    private PresenceViewsGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(PresenceViewsGameTests.class);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aFreshBellyReadsAsFull(GameTestHelper helper) {
        var view = PresenceViews.viewOf(new Hunger());
        helper.assertTrue(view.foodLevel() == Hunger.MAX_FOOD_LEVEL,
            "a fresh belly holds a full food level, got " + view.foodLevel());
        helper.assertTrue(view.maxFoodLevel() == Hunger.MAX_FOOD_LEVEL,
            "the view reports the same maximum as Hunger, got " + view.maxFoodLevel());
        helper.assertTrue(view.fullness() == 1.0F, "a fresh belly reads as full, got " + view.fullness());
        helper.assertTrue(!view.starving(), "a fresh belly is not starving");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void anUnclaimedBedReadsAsNone(GameTestHelper helper) {
        var view = PresenceViews.BedView.none();
        helper.assertTrue(!view.hasBed(), "an unclaimed bed reads as absent");
        helper.assertTrue(view.bed().isEmpty(), "and carries no cell");
        helper.succeed();
    }
}