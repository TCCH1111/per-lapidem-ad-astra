package io.github.izakyl.folkways.plugins.steering;

import io.github.izakyl.folkways.FolkwaysMod;
import io.github.izakyl.folkways.base.persist.Reader;
import io.github.izakyl.folkways.plugins.FolkwaysVanillaMod;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
 * PLA: WP-4 T2 — the zone verdict is a pure function and the zones survive a save and load.
 */
@EventBusSubscriber(modid = FolkwaysVanillaMod.ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SteeringStateGameTests {

    private static final String TEMPLATE = "empty";
    private static final ResourceLocation FARMER = ResourceLocation.fromNamespaceAndPath("folkways", "farmer");
    private static final ResourceLocation MINER = ResourceLocation.fromNamespaceAndPath("folkways", "miner");
    private static final BlockPos INSIDE = new BlockPos(0, 0, 0);
    private static final BlockPos OUTSIDE = new BlockPos(9, 0, 9);

    private SteeringStateGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(SteeringStateGameTests.class);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aForbidZoneRefusesEverySubjectOnItsCells(GameTestHelper helper) {
        ZoneRule rule = ZoneRule.of("no entry", ZoneRule.Kind.FORBID, List.of(), 0, Set.of(INSIDE));
        helper.assertTrue(rule.verdict(INSIDE, FARMER, 0) == ZoneRule.Verdict.FORBIDDEN,
            "a forbid zone refuses a farmer standing on it");
        helper.assertTrue(rule.verdict(OUTSIDE, FARMER, 0) == ZoneRule.Verdict.ALLOWED,
            "and allows the very next cell outside it");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void anAllowOnlyZoneAdmitsItsSubjectsAndNoOneElse(GameTestHelper helper) {
        ZoneRule rule = ZoneRule.of("farmers only", ZoneRule.Kind.ALLOW_ONLY, List.of(FARMER), 0, Set.of(INSIDE));
        helper.assertTrue(rule.verdict(INSIDE, FARMER, 0) == ZoneRule.Verdict.ALLOWED,
            "the named subject may enter");
        helper.assertTrue(rule.verdict(INSIDE, MINER, 0) == ZoneRule.Verdict.ONLY_FOR_OTHERS,
            "any other subject is told the zone is for others");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aLimitZoneIsFullAtItsQuota(GameTestHelper helper) {
        ZoneRule rule = ZoneRule.of("two at a time", ZoneRule.Kind.LIMIT, List.of(), 2, Set.of(INSIDE));
        helper.assertTrue(rule.verdict(INSIDE, FARMER, 1) == ZoneRule.Verdict.ALLOWED,
            "one of two inside still has room");
        helper.assertTrue(rule.verdict(INSIDE, FARMER, 2) == ZoneRule.Verdict.QUOTA_FULL,
            "the second one fills the quota");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void zonesSurviveASaveAndLoad(GameTestHelper helper) {
        SteeringState before = new SteeringState();
        ZoneRule rule = new ZoneRule(UUID.randomUUID(), "no entry", ZoneRule.Kind.FORBID,
            List.of(FARMER), 3, Set.of(INSIDE, OUTSIDE));
        before.put(rule);
        SteeringState after = SteeringState.load(Reader.of(before.save()));

        helper.assertTrue(after.zones().size() == 1, "one zone came back, got " + after.zones().size());
        ZoneRule back = after.zones().getFirst();
        helper.assertTrue(back.id().equals(rule.id()), "the id survived");
        helper.assertTrue(back.name().equals(rule.name()), "the name survived");
        helper.assertTrue(back.kind() == rule.kind(), "the kind survived");
        helper.assertTrue(back.quota() == rule.quota(), "the quota survived");
        helper.assertTrue(back.subjects().equals(rule.subjects()), "the subjects survived");
        helper.assertTrue(back.cells().equals(rule.cells()), "the cells survived");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void anEmptyColonyKeepsNoZones(GameTestHelper helper) {
        SteeringState state = SteeringState.load(Reader.of(new net.minecraft.nbt.CompoundTag()));
        helper.assertTrue(state.zones().isEmpty(), "a fresh colony has no zones");
        helper.assertTrue(!state.dirty(), "and nothing to write");
        helper.succeed();
    }
}