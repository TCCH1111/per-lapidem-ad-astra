package io.github.izakyl.folkways.plugins.health;

import io.github.izakyl.folkways.FolkwaysMod;
import io.github.izakyl.folkways.base.persist.Reader;
import io.github.izakyl.folkways.core.api.colony.Colonies;
import io.github.izakyl.folkways.core.api.colony.Colony;
import io.github.izakyl.folkways.core.api.resident.body.Body;
import io.github.izakyl.folkways.plugins.person.PersonBody;
import io.github.izakyl.folkways.plugins.FolkwaysVanillaMod;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * PLA: M2 R0 — the medical book: wounds exist, blood runs, and nothing changes for a colony without
 * wounds.
 */
@EventBusSubscriber(modid = FolkwaysVanillaMod.ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HealthStateGameTests {

    private static final String TEMPLATE = "empty";

    private HealthStateGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(HealthStateGameTests.class);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aWoundSurvivesASave(GameTestHelper helper) {
        HealthState state = new HealthState();
        UUID who = UUID.randomUUID();
        state.wound(who, new HealthState.Wound("left arm", "cut", 3, "wolf", 120L, false));

        HealthState back = HealthState.load(Reader.of(state.save()));
        HealthState.Wound one = back.wounds(who).get(0);
        helper.assertTrue(one.part().equals("left arm") && one.kind().equals("cut"),
            "the wound kept its place and kind: " + one);
        helper.assertTrue(one.severity() == 3 && one.source().equals("wolf") && one.since() == 120L,
            "and its severity, source and time: " + one);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void anOpenWoundBleedsAndATreatedOneDoesNot(GameTestHelper helper) {
        HealthState state = new HealthState();
        UUID who = UUID.randomUUID();
        state.wound(who, new HealthState.Wound("torso", "gash", 4, "axe", 0L, false));

        double before = state.blood(who);
        double after = state.bleed(who, 100L);
        helper.assertTrue(after < before, "an open wound costs blood: " + before + " -> " + after);

        helper.assertTrue(state.treat(who, "torso"), "the wound can be treated");
        double treated = state.bleed(who, 100L);
        helper.assertTrue(Math.abs(treated - after) < 1e-9, "a treated wound stops bleeding");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void bloodStopsAtZeroAndNoWoundChangesNothing(GameTestHelper helper) {
        HealthState state = new HealthState();
        UUID who = UUID.randomUUID();
        UUID quiet = UUID.randomUUID();
        state.wound(who, new HealthState.Wound("head", "blunt", 5, "fall", 0L, false));

        double floor = state.bleed(who, 100_000L);
        helper.assertTrue(floor == 0.0, "blood floors at zero, got " + floor);
        helper.assertTrue(state.blood(quiet) == HealthState.MAX_BLOOD, "an unwounded resident is untouched");
        helper.assertTrue(!state.hurt(quiet), "and is not hurt");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void severityDecidesWhoStopsWork(GameTestHelper helper) {
        // Owner's rule C: light injuries work on, severe ones are not given planned labour.
        HealthState book = new HealthState();
        UUID light = UUID.randomUUID();
        UUID heavy = UUID.randomUUID();
        book.wound(light, new HealthState.Wound("finger", "cut", 1, "knife", 0L, false));
        book.wound(heavy, new HealthState.Wound("torso", "gash", 4, "axe", 0L, false));

        helper.assertTrue(!book.severe(light), "a cut finger is not severe");
        helper.assertTrue(book.severe(heavy), "a deep gash is");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void bloodAloneCanMakeSomebodySevere(GameTestHelper helper) {
        HealthState book = new HealthState();
        UUID drained = UUID.randomUUID();
        book.wound(drained, new HealthState.Wound("arm", "cut", 2, "fall", 0L, false));
        book.bleed(drained, 100_000L); // 流血到接近零

        helper.assertTrue(book.blood(drained) < HealthState.MAX_BLOOD * HealthState.SEVERE_BLOOD_FRACTION,
            "the blood is low enough to count");
        helper.assertTrue(book.severe(drained), "so the resident is severe even with a small wound");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void anUnwoundedResidentIsNeverSevere(GameTestHelper helper) {
        HealthState book = new HealthState();
        UUID quiet = UUID.randomUUID();
        helper.assertTrue(!book.severe(quiet), "no wound, full blood, no restriction");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void theCardSpeaksOnlyForTheBadlyHurt(GameTestHelper helper) {
        // PLA: M2 R0-4 — the card line exists to prove the state is read, and to stop at what is true.
        helper.assertTrue(HealthFacing.linesFor(false).isEmpty(), "a lightly hurt resident says nothing");
        helper.assertTrue(HealthFacing.linesFor(true).size() == 2, "a badly hurt one says what and what to do");
        for (Injury one : Injury.values()) {
            String key = one.translationKey();
            String shown = net.minecraft.network.chat.Component.translatable(key).getString();
            helper.assertTrue(!shown.isBlank() && !shown.equals(key),
                "the language table has no words for " + key + ", got: " + shown);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void infectionIsAPureFunctionOfTheRoll(GameTestHelper helper) {
        // PLA: M2 R1-1 — a bad day turns a wound, a good one does not, and the same roll always agrees.
        HealthState book = new HealthState();
        UUID who = UUID.randomUUID();
        book.wound(who, new HealthState.Wound("arm", "cut", 2, "wolf", 0L, false));

        helper.assertTrue(book.maybeInfect(who, "arm", 0.01), "a low roll infects");
        helper.assertTrue(book.infection(who, "arm") > 0, "and the part remembers it");
        helper.assertTrue(!book.maybeInfect(who, "arm", 0.01), "but it does not turn twice");

        HealthState other = new HealthState();
        UUID clean = UUID.randomUUID();
        other.wound(clean, new HealthState.Wound("leg", "cut", 2, "wolf", 0L, false));
        helper.assertTrue(!other.maybeInfect(clean, "leg", 0.99), "a high roll leaves it alone");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void infectionDeepensByTheDay(GameTestHelper helper) {
        HealthState book = new HealthState();
        UUID who = UUID.randomUUID();
        book.wound(who, new HealthState.Wound("arm", "cut", 2, "wolf", 0L, false));
        book.maybeInfect(who, "arm", 0.0);

        int after = book.infection(who, "arm");
        book.progressInfection(who, 3);
        helper.assertTrue(book.infection(who, "arm") > after, "three days deepen it");
        book.progressInfection(who, 1000);
        helper.assertTrue(book.infection(who, "arm") == 100, "and it never passes a hundred");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void deepInfectionCountsAsSevere(GameTestHelper helper) {
        HealthState book = new HealthState();
        UUID who = UUID.randomUUID();
        book.wound(who, new HealthState.Wound("arm", "cut", 1, "wolf", 0L, false));
        book.maybeInfect(who, "arm", 0.0);
        helper.assertTrue(!book.severe(who), "a fresh infection is not yet severe");

        book.progressInfection(who, HealthState.NECROTIC_AT / HealthState.INFECTION_GAIN_PER_DAY);
        helper.assertTrue(book.necrotic(who), "past the threshold the flesh is dying");
        helper.assertTrue(book.severe(who), "and the resident stops being given work");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void infectionSurvivesASave(GameTestHelper helper) {
        HealthState book = new HealthState();
        UUID who = UUID.randomUUID();
        book.wound(who, new HealthState.Wound("arm", "cut", 2, "wolf", 0L, false));
        book.maybeInfect(who, "arm", 0.0);
        book.progressInfection(who, 2);

        HealthState back = HealthState.load(Reader.of(book.save()));
        helper.assertTrue(back.infection(who, "arm") == book.infection(who, "arm"),
            "the infection came back, got " + back.infection(who, "arm"));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aDeathLeavesATrace(GameTestHelper helper) {
        // PLA: M2 R1-3 groundwork — PRD-009 asks that a death have weight; it starts with a record.
        HealthState book = new HealthState();
        UUID who = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        book.record(new HealthState.Death(who, "Tori", "infected wound", 1200L, false));
        book.record(new HealthState.Death(other, "Rotunda", "bleeding", 2400L, false));

        helper.assertTrue(book.deathCount() == 2, "both deaths are kept");
        HealthState.Death latest = book.recentDeaths(1).get(0);
        helper.assertTrue(latest.name().equals("Rotunda"), "recent deaths come newest first");
        helper.assertTrue(latest.cause().equals("bleeding") && latest.at() == 2400L,
            "with cause and time, got " + latest);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aDeathSurvivesASave(GameTestHelper helper) {
        HealthState book = new HealthState();
        UUID who = UUID.randomUUID();
        book.record(new HealthState.Death(who, "Tori", "infected wound", 1200L, false));

        HealthState back = HealthState.load(Reader.of(book.save()));
        helper.assertTrue(back.deathCount() == 1, "the death came back");
        HealthState.Death one = back.recentDeaths(1).get(0);
        helper.assertTrue(one.name().equals("Tori") && one.cause().equals("infected wound")
            && one.at() == 1200L && one.who().equals(who), "with everything it carried, got " + one);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aNamelessDeathStillSaysSomething(GameTestHelper helper) {
        HealthState book = new HealthState();
        book.record(new HealthState.Death(UUID.randomUUID(), "  ", "", 0L, false));
        HealthState.Death one = book.recentDeaths(1).get(0);
        helper.assertTrue(one.name().equals("someone") && one.cause().equals("unknown"),
            "an unnamed death reads as somebody unknown, got " + one);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void feverRisesWithTheInfection(GameTestHelper helper) {
        // PLA: M2 R1-2 groundwork — fever is derived from infection, not a second number to keep.
        HealthState book = new HealthState();
        UUID who = UUID.randomUUID();
        book.wound(who, new HealthState.Wound("arm", "cut", 2, "wolf", 0L, false));
        helper.assertTrue(book.fever(who) == 0, "no infection, no fever");

        book.maybeInfect(who, "arm", 0.0);
        while (book.infection(who, "arm") < HealthState.FEVER_AT) {
            book.progressInfection(who, 1);
        }
        helper.assertTrue(book.fever(who) == 1, "past the warning mark it is a fever");

        while (book.infection(who, "arm") < HealthState.HIGH_FEVER_AT) {
            book.progressInfection(who, 1);
        }
        helper.assertTrue(book.fever(who) == 2, "past the danger mark it is a high fever");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void anUnburiedDeathIsAVisibleState(GameTestHelper helper) {
        // PLA: PRD-009 G2 — the colony must be able to say a death has not been dealt with.
        HealthState book = new HealthState();
        UUID who = UUID.randomUUID();
        book.record(new HealthState.Death(who, "Tori", "infected wound", 1200L, false));
        helper.assertTrue(book.unburiedCount() == 1, "one body waiting");

        helper.assertTrue(book.bury(who), "burying the newest death of that resident works");
        helper.assertTrue(book.unburiedCount() == 0, "and nothing waits any more");
        helper.assertTrue(!book.bury(who), "burying twice does nothing the second time");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void burialSurvivesASave(GameTestHelper helper) {
        HealthState book = new HealthState();
        UUID who = UUID.randomUUID();
        book.record(new HealthState.Death(who, "Tori", "bleeding", 100L, false));
        book.bury(who);

        HealthState back = HealthState.load(Reader.of(book.save()));
        helper.assertTrue(back.unburiedCount() == 0, "the burial came back");
        helper.assertTrue(back.recentDeaths(1).get(0).buried(), "marked as dealt with");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void eitherEndKills(GameTestHelper helper) {
        // PLA: M2 R1-2 — the owner's rule: no blood left, or an infection run to its end.
        HealthState book = new HealthState();
        UUID healthy = UUID.randomUUID();
        UUID bled = UUID.randomUUID();
        UUID infected = UUID.randomUUID();
        book.wound(healthy, new HealthState.Wound("arm", "cut", 1, "wolf", 0L, false));
        book.wound(bled, new HealthState.Wound("arm", "cut", 3, "wolf", 0L, false));
        book.wound(infected, new HealthState.Wound("leg", "gash", 2, "axe", 0L, false));

        helper.assertTrue(book.causeOfDeath(healthy).isEmpty(), "a light wound kills nobody");
        book.bleed(bled, 1_000_000L);
        helper.assertTrue(book.causeOfDeath(bled).orElse("").equals("bled out"), "no blood, one cause");

        book.maybeInfect(infected, "leg", 0.0);
        book.progressInfection(infected, 1000);
        helper.assertTrue(book.infection(infected, "leg") == 100, "the infection ran to its end");
        helper.assertTrue(book.causeOfDeath(infected).orElse("").equals("infection"),
            "the other end, the other cause");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void aDrainedResidentDiesAndIsWrittenDown(GameTestHelper helper) {
        var level = helper.getLevel();
        Colony colony = Colonies.mint(level.getServer());
        try {
            HealthState book = colony.service(HealthContent.ID, HealthState.class)
                .orElseThrow(() -> new AssertionError("no health book"));
            Body body = PersonBody.RESIDENT.get().create(level);
            body.mob().moveTo(helper.absolutePos(new net.minecraft.core.BlockPos(2, 1, 2)), 0.0F, 0.0F);
            level.addFreshEntity(body.mob());
            helper.assertTrue(level.getEntity(body.resident().id()) != null,
                "the body is really in the world before it dies");
            UUID who = body.resident().id();
            book.wound(who, new HealthState.Wound("torso", "gash", 3, "axe", 0L, false));
            book.bleed(who, 1_000_000L);

            int died = HealthContent.sweep(colony, level.getServer(), 100L);
            helper.assertTrue(died == 1, "the sweep found the dead, got " + died);
            helper.assertTrue(book.deathCount() == 1, "and wrote them down");
            HealthState.Death one = book.recentDeaths(1).get(0);
            helper.assertTrue(one.cause().equals("bled out"), "with the cause, got " + one);
            helper.assertTrue(one.who().equals(who), "and the right resident");
            // kill() marks the body dead at once; the level removes it on a later tick, so the honest
            // assertion here is that it is no longer alive rather than that it has vanished.
            net.minecraft.world.entity.Entity left = level.getEntity(who);
            helper.assertTrue(left == null || !left.isAlive(), "the body is no longer alive");
            helper.assertTrue(HealthContent.sweep(colony, level.getServer(), 120L) == 0,
                "a second sweep finds nothing more");
        } finally {
            colony.raze();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void theDeathNoticeHasWords(GameTestHelper helper) {
        // PLA: M2 R1-3 — a death the player never sees is a resident who simply vanished.
        String bled = HealthContent.causeKey("bled out");
        String infected = HealthContent.causeKey("infection");
        String unknown = HealthContent.causeKey("something else");
        helper.assertTrue(bled.equals("folkways.health.cause.bled_out"), "bled out maps to its key");
        helper.assertTrue(infected.equals("folkways.health.cause.infection"), "infection too");
        helper.assertTrue(unknown.equals("folkways.health.cause.unknown"), "and anything else is unknown");

        for (String key : List.of(bled, infected, unknown)) {
            String shown = net.minecraft.network.chat.Component.translatable(key).getString();
            helper.assertTrue(!shown.isBlank() && !shown.equals(key),
                "the language table has no words for " + key + ", got: " + shown);
        }
        String notice = net.minecraft.network.chat.Component.translatable("folkways.health.died", "Tori",
            net.minecraft.network.chat.Component.translatable(bled)).getString();
        helper.assertTrue(notice.contains("Tori"), "the notice names the resident, got " + notice);
        helper.assertTrue(!notice.contains("folkways.health.died"), "and is not the raw key, got " + notice);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void theUnburiedCountIsSaidOutLoud(GameTestHelper helper) {
        // PLA: M2 R1 presentation — a body nobody is reminded about is a body that stays on the ground.
        helper.assertTrue(HealthContent.unburiedNotice(0).getString().isEmpty(),
            "nothing waiting says nothing");
        helper.assertTrue(HealthContent.unburiedNotice(-1).getString().isEmpty(),
            "and neither does nonsense");

        String one = HealthContent.unburiedNotice(1).getString();
        helper.assertTrue(one.contains("1"), "one body is counted, got " + one);
        helper.assertTrue(!one.contains("folkways.health.unburied"), "in words, not a raw key, got " + one);

        String three = HealthContent.unburiedNotice(3).getString();
        helper.assertTrue(three.contains("3"), "and so is three, got " + three);
        helper.succeed();
    }
}
