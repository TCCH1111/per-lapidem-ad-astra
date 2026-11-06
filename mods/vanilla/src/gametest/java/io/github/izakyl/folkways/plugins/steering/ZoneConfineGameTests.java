package io.github.izakyl.folkways.plugins.steering;

import io.github.izakyl.folkways.FolkwaysMod;
import io.github.izakyl.folkways.plugins.FolkwaysVanillaMod;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * PLA: M3 captivity seam — a confinement keeps its subjects inside without touching anybody else.
 */
@EventBusSubscriber(modid = FolkwaysVanillaMod.ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ZoneConfineGameTests {

    private static final String TEMPLATE = "empty";
    private static final BlockPos INSIDE = new BlockPos(1, 1, 1);
    private static final BlockPos OUTSIDE = new BlockPos(9, 1, 9);
    private static final ResourceLocation PRISONER = ResourceLocation.fromNamespaceAndPath("folkways", "prisoner");
    private static final ResourceLocation FARMER = ResourceLocation.fromNamespaceAndPath("folkways", "farmer");

    private ZoneConfineGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(ZoneConfineGameTests.class);
    }

    private static ZoneRule cell() {
        return ZoneRule.of("cell", ZoneRule.Kind.CONFINE, List.of(PRISONER), 0, Set.of(INSIDE));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aPrisonerMayNotLeave(GameTestHelper helper) {
        ZoneRule rule = cell();
        helper.assertTrue(rule.verdict(INSIDE, PRISONER, 0) == ZoneRule.Verdict.ALLOWED,
            "inside the cell a prisoner is allowed");
        helper.assertTrue(rule.verdict(OUTSIDE, PRISONER, 0) == ZoneRule.Verdict.FORBIDDEN,
            "outside it they are refused");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void everybodyElseIsUntouched(GameTestHelper helper) {
        ZoneRule rule = cell();
        helper.assertTrue(rule.verdict(OUTSIDE, FARMER, 0) == ZoneRule.Verdict.ALLOWED,
            "a farmer outside is unaffected");
        helper.assertTrue(rule.verdict(INSIDE, FARMER, 0) == ZoneRule.Verdict.ONLY_FOR_OTHERS,
            "and may not wander into the cell");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aConfinementThatNamesNobodyDoesNothing(GameTestHelper helper) {
        ZoneRule nobody = ZoneRule.of("empty", ZoneRule.Kind.CONFINE, List.of(), 0, Set.of(INSIDE));
        helper.assertTrue(nobody.verdict(OUTSIDE, PRISONER, 0) == ZoneRule.Verdict.ALLOWED,
            "with no subjects listed, nobody is confined anywhere");
        helper.succeed();
    }
}
