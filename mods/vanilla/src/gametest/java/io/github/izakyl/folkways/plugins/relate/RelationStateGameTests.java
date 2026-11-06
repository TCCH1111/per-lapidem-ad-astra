package io.github.izakyl.folkways.plugins.relate;

import io.github.izakyl.folkways.FolkwaysMod;
import io.github.izakyl.folkways.base.persist.Reader;
import io.github.izakyl.folkways.plugins.FolkwaysVanillaMod;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * PLA: M2 R1 — the book of who knows whom: how fast a bond grows, what happens at thirty entries,
 * what survives a save, and what is forgotten when someone leaves.
 */
@EventBusSubscriber(modid = FolkwaysVanillaMod.ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RelationStateGameTests {

    private static final String TEMPLATE = "empty";

    private RelationStateGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(RelationStateGameTests.class);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aBondGrowsOncePerHalfHour(GameTestHelper helper) {
        RelationState book = new RelationState();
        UUID one = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        helper.assertTrue(book.touch(one, other, 0L), "the first meeting counts");
        helper.assertTrue(!book.touch(one, other, 100L), "a second meeting in the same half hour does not");
        helper.assertTrue(book.bond(one, other) == 1, "so the bond is one");
        helper.assertTrue(book.touch(one, other, RelationState.TOUCH_TICKS), "half an hour later it counts again");
        helper.assertTrue(book.bond(one, other) == 2, "and the bond is two");

        helper.assertTrue(!book.touch(one, one, 0L), "nobody gets closer to themselves");
        helper.assertTrue(!book.touch(null, other, 0L), "and nobody to nobody");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void theHeadHoldsThirtyPeople(GameTestHelper helper) {
        RelationState book = new RelationState();
        UUID subject = UUID.randomUUID();

        // Build different strengths: the first target is touched once, the last one many times.
        for (int i = 0; i < RelationState.CAP; i++) {
            UUID target = UUID.randomUUID();
            for (int strength = 0; strength <= i; strength++) {
                book.touch(subject, target, strength * RelationState.TOUCH_TICKS);
            }
        }
        helper.assertTrue(book.size(subject) == RelationState.CAP, "thirty people fit");

        UUID newcomer = UUID.randomUUID();
        book.touch(subject, newcomer, 100_000L);
        helper.assertTrue(book.size(subject) == RelationState.CAP, "and the head does not grow past thirty");
        helper.assertTrue(book.bond(subject, newcomer) == 1, "the newcomer is remembered");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void theBookSurvivesASave(GameTestHelper helper) {
        RelationState book = new RelationState();
        UUID one = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        book.touch(one, other, 0L);
        book.touch(one, other, RelationState.TOUCH_TICKS);
        book.share(one, other, 2 * RelationState.TOUCH_TICKS);

        RelationState back = RelationState.load(Reader.of(book.save()));
        helper.assertTrue(back.bond(one, other) == 2, "the bond came back");
        helper.assertTrue(back.known(one).size() == 1, "and so did the single entry");
        helper.assertTrue(back.known(one).getFirst().shared() == 1, "with its shared count");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void leavingIsForgotten(GameTestHelper helper) {
        RelationState book = new RelationState();
        UUID stays = UUID.randomUUID();
        UUID leaves = UUID.randomUUID();
        book.touch(stays, leaves, 0L);
        book.touch(leaves, stays, 0L);
        helper.assertTrue(book.bond(stays, leaves) == 1, "they know each other first");

        Set<UUID> present = new HashSet<>();
        present.add(stays);
        book.prune(present);

        helper.assertTrue(book.size(stays) == 0, "the one who left is no longer in the book");
        helper.assertTrue(book.size(leaves) == 0, "and their own book is gone too");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void theBandsMeanWhatThePrdSays(GameTestHelper helper) {
        RelationState book = new RelationState();
        UUID one = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        for (int i = 0; i < RelationState.ACQUAINTED; i++) {
            book.touch(one, other, i * RelationState.TOUCH_TICKS);
        }
        helper.assertTrue(book.acquainted(one, other), "ten touches make an acquaintance");
        helper.assertTrue(!book.friends(one, other), "but not a friend");

        for (int i = RelationState.ACQUAINTED; i < RelationState.FRIEND; i++) {
            book.touch(one, other, i * RelationState.TOUCH_TICKS);
        }
        helper.assertTrue(book.friends(one, other), "forty make a friend");
        helper.assertTrue(!book.mutualFriends(one, other), "one side is not both sides");

        for (int i = 0; i < RelationState.FRIEND; i++) {
            book.touch(other, one, i * RelationState.TOUCH_TICKS);
        }
        helper.assertTrue(book.mutualFriends(one, other), "and both sides make it mutual");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void frictionIsItsOwnAxis(GameTestHelper helper) {
        RelationState book = new RelationState();
        UUID one = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        // A friendship and a grudge can live in the same head: the PRD insists on it.
        book.touch(one, other, 0L);
        helper.assertTrue(book.friction(one, other, RelationState.TOUCH_TICKS), "a bad moment counts");
        helper.assertTrue(!book.friction(one, other, RelationState.TOUCH_TICKS + 1),
            "but not twice in one half hour");
        helper.assertTrue(book.bond(one, other) == 1,
            "the friendship is untouched, got " + book.bond(one, other));
        helper.assertTrue(book.grudge(one, other) == 1,
            "and the grudge is its own number, got " + book.grudge(one, other));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void resentmentNeedsForty(GameTestHelper helper) {
        RelationState book = new RelationState();
        UUID one = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        for (int i = 0; i < RelationState.RESENTMENT; i++) {
            book.friction(one, other, i * RelationState.TOUCH_TICKS);
        }
        helper.assertTrue(book.resents(one, other), "forty bad moments make a resentment");
        helper.assertTrue(!book.mutualResentment(one, other), "one side is not both sides");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aGrudgeSurvivesASave(GameTestHelper helper) {
        RelationState book = new RelationState();
        UUID one = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        book.friction(one, other, 0L);
        book.friction(one, other, RelationState.TOUCH_TICKS);
        RelationState back = RelationState.load(Reader.of(book.save()));
        helper.assertTrue(back.grudge(one, other) == 2,
            "the grudge came back, got " + back.grudge(one, other));
        helper.assertTrue(back.bond(one, other) == 0, "and it did not turn into a friendship");
        helper.succeed();
    }
}
