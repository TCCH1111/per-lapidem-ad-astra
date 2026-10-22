package io.github.izakyl.folkways.core.api.resident.body;

import io.github.izakyl.folkways.FolkwaysMod;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * PLA: proves the read-only view (PLAN-002 T3) is present for a fresh body and changes nothing.
 */
@EventBusSubscriber(modid = FolkwaysMod.CORE_ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BodyViewsGameTests {

    private static final String TEMPLATE = "empty";

    private BodyViewsGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(BodyViewsGameTests.class);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aFreshBodyHasNoColonyNoDoingAndRoomInItsPack(GameTestHelper helper) {
        var cow = helper.spawn(EntityType.COW, new BlockPos(1, 1, 1));
        var view = BodyViews.of(cow).orElseThrow();
        helper.assertTrue(view.colony().isEmpty(), "a fresh body belongs to no colony");
        helper.assertTrue(view.doing().isEmpty(), "a fresh body is doing nothing");
        helper.assertTrue(view.balk().isEmpty(), "a fresh body is balking at nothing");
        helper.assertTrue(view.packSlots() > 0, "a fresh body's pack offers cells, got " + view.packSlots());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void readingTwiceChangesNothing(GameTestHelper helper) {
        var cow = helper.spawn(EntityType.COW, new BlockPos(1, 1, 1));
        var first = BodyViews.of(cow).orElseThrow();
        var second = BodyViews.of(cow).orElseThrow();
        helper.assertTrue(first.colony().isEmpty() && second.colony().isEmpty(), "no colony appeared");
        helper.assertTrue(first.packSlots() == second.packSlots(),
            "the pack size is stable across reads: " + first.packSlots() + " vs " + second.packSlots());
        helper.assertTrue(first.doing().isEmpty() && second.doing().isEmpty(), "no work appeared");
        helper.succeed();
    }
}