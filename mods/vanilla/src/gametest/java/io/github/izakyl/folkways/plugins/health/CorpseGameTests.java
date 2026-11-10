package io.github.izakyl.folkways.plugins.health;

import io.github.izakyl.folkways.FolkwaysMod;
import io.github.izakyl.folkways.core.api.colony.Colonies;
import io.github.izakyl.folkways.core.api.colony.Colony;
import io.github.izakyl.folkways.core.api.resident.body.Body;
import io.github.izakyl.folkways.plugins.FolkwaysVanillaMod;
import io.github.izakyl.folkways.plugins.person.PersonBody;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * PLA: M2 R1-3 — a body stays where its owner fell, and burying it marks the record and leaves a grave.
 */
@EventBusSubscriber(modid = FolkwaysVanillaMod.ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CorpseGameTests {

    private static final String TEMPLATE = "empty";

    private CorpseGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(CorpseGameTests.class);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void aDeathLeavesABodyBehind(GameTestHelper helper) {
        var level = helper.getLevel();
        Colony colony = Colonies.mint(level.getServer());
        try {
            HealthState book = colony.service(HealthContent.ID, HealthState.class).orElseThrow();
            Body body = PersonBody.RESIDENT.get().create(level);
            body.mob().moveTo(helper.absolutePos(new BlockPos(3, 1, 3)), 0.0F, 0.0F);
            level.addFreshEntity(body.mob());
            UUID who = body.resident().id();
            book.wound(who, new HealthState.Wound("torso", "gash", 3, "axe", 0L, false));
            book.bleed(who, 1_000_000L);

            helper.assertTrue(HealthContent.sweep(colony, level.getServer(), 100L) == 1,
                "one resident died");

            BlockPos at = Corpses.bodyOf(level, body.mob().blockPosition(), who)
                .orElseThrow(() -> new AssertionError("no body was left near " + body.mob().blockPosition()));
            CorpseBlockEntity corpse = (CorpseBlockEntity) level.getBlockEntity(at);
            helper.assertTrue(corpse != null && corpse.who().equals(who), "it knows who it was");
            helper.assertTrue(corpse.cause().equals("bled out"), "and what took them, got " + corpse.cause());
            helper.assertTrue(book.unburiedCount() == 1, "and the colony knows a body waits");
        } finally {
            colony.raze();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void buryingLeavesAGraveAndMarksTheRecord(GameTestHelper helper) {
        var level = helper.getLevel();
        Colony colony = Colonies.mint(level.getServer());
        try {
            HealthState book = colony.service(HealthContent.ID, HealthState.class).orElseThrow();
            Body body = PersonBody.RESIDENT.get().create(level);
            body.mob().moveTo(helper.absolutePos(new BlockPos(5, 1, 5)), 0.0F, 0.0F);
            level.addFreshEntity(body.mob());
            UUID who = body.resident().id();
            book.wound(who, new HealthState.Wound("leg", "cut", 3, "fall", 0L, false));
            book.bleed(who, 1_000_000L);
            HealthContent.sweep(colony, level.getServer(), 200L);

            BlockPos at = Corpses.bodyOf(level, body.mob().blockPosition(), who)
                .orElseThrow(() -> new AssertionError("no body to bury"));
            helper.assertTrue(Corpses.bury(level, at), "the body can be dealt with");
            helper.assertTrue(level.getBlockState(at).is(Corpses.GRAVE.get()), "a grave stands there");
            helper.assertTrue(book.unburiedCount() == 0, "and the colony's record is settled");
            helper.assertTrue(!Corpses.bury(level, at), "burying an empty grave does nothing");
        } finally {
            colony.raze();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aBodyCanBePutDownByHand(GameTestHelper helper) {
        // Isolates the block itself from where a dying mob happens to stand.
        var level = helper.getLevel();
        Colony colony = Colonies.mint(level.getServer());
        try {
            BlockPos where = helper.absolutePos(new BlockPos(1, 3, 1));
            helper.assertTrue(Corpses.place(level, where, new HealthState.Death(UUID.randomUUID(),
                "Tori", "fall", 5L, false), colony.id()), "a body can be placed on open ground");
            helper.assertTrue(Corpses.isCorpse(level.getBlockState(where)), "and it is a body");
        } finally {
            colony.raze();
        }
        helper.succeed();
    }
}
