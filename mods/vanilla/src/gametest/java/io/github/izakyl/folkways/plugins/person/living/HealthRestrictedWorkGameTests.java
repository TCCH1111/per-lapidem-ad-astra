package io.github.izakyl.folkways.plugins.person.living;

import io.github.izakyl.folkways.FolkwaysMod;
import io.github.izakyl.folkways.core.api.work.Urge;
import io.github.izakyl.folkways.plugins.FolkwaysVanillaMod;
import io.github.izakyl.folkways.plugins.health.HealthContent;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * PLA: M2 R0-3/R0-5 — being too hurt contributes one thing and it is a reservation, which the labour
 * contract defines as excluding planned work while leaving personal needs to the runner.
 */
@EventBusSubscriber(modid = FolkwaysVanillaMod.ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HealthRestrictedWorkGameTests {

    private static final String TEMPLATE = "empty";

    private HealthRestrictedWorkGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(HealthRestrictedWorkGameTests.class);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aHurtBodyAsksForNothingExtraWhenItIsOnlyLightlyHurt(GameTestHelper helper) {
        helper.assertTrue(LivingPresence.restriction(false).isEmpty(),
            "a cut finger adds nothing to what the body wants");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aHurtBodyReservesItselfAgainstPlannedWork(GameTestHelper helper) {
        List<Urge> offered = LivingPresence.restriction(true);
        helper.assertTrue(offered.size() == 1, "being badly hurt adds exactly one thing");
        Urge one = offered.get(0);
        helper.assertTrue(one.reservation(), "and that thing is a reservation, not an errand");
        helper.assertTrue(one.id().equals(HealthContent.RESTING),
            "carrying the health plugin's own id, got " + one.id());
        helper.succeed();
    }
}
