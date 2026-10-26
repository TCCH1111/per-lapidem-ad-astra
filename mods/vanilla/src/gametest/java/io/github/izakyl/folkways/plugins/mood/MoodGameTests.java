package io.github.izakyl.folkways.plugins.mood;

import io.github.izakyl.folkways.FolkwaysMod;
import io.github.izakyl.folkways.plugins.FolkwaysVanillaMod;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * PLA: M2 N1/N2 — the mood bands hold their edges, and every word a band shows exists in the
 * shipped language files.
 */
@EventBusSubscriber(modid = FolkwaysVanillaMod.ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MoodGameTests {

    private static final String TEMPLATE = "empty";
    private static final int FULL = 20;
    private static final int RESTED = 100;

    private MoodGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(MoodGameTests.class);
    }

    private static Mood.Needs needs(int food, int alertness, boolean refused, boolean waiting) {
        return new Mood.Needs(food, FULL, false, alertness, RESTED, false, refused, waiting);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aFedRestedResidentWithWorkIsContent(GameTestHelper helper) {
        helper.assertTrue(Mood.of(needs(FULL, RESTED, false, true)) == Mood.CONTENT,
            "full, rested and with work waiting is content");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aNeedTheBaseCallsUrgentIsDesperate(GameTestHelper helper) {
        Mood.Needs starving = new Mood.Needs(2, FULL, true, RESTED, RESTED, false, false, true);
        Mood.Needs awake = new Mood.Needs(FULL, FULL, false, 1, RESTED, true, false, true);
        helper.assertTrue(Mood.of(starving) == Mood.DESPERATE, "urgent hunger outranks everything");
        helper.assertTrue(Mood.of(awake) == Mood.DESPERATE, "as does urgent weariness");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aquarterLowOrARefusalIsStrain(GameTestHelper helper) {
        helper.assertTrue(Mood.of(needs(5, RESTED, false, true)) == Mood.STRESSED,
            "a belly down to a quarter is strain");
        helper.assertTrue(Mood.of(needs(FULL, 25, false, true)) == Mood.STRESSED,
            "and so is rest down to a quarter");
        helper.assertTrue(Mood.of(needs(FULL, RESTED, true, true)) == Mood.STRESSED,
            "being refused work is strain even when fed and rested");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void gettingLowOrNoWorkAtAllIsUnease(GameTestHelper helper) {
        helper.assertTrue(Mood.of(needs(10, RESTED, false, true)) == Mood.WORRIED,
            "half a belly is unease, not strain");
        helper.assertTrue(Mood.of(needs(FULL, 50, false, true)) == Mood.WORRIED,
            "half rested likewise");
        helper.assertTrue(Mood.of(needs(FULL, RESTED, false, false)) == Mood.WORRIED,
            "and a resident with no work waiting is uneasy");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void everyBandHasWordsThatResolve(GameTestHelper helper) {
        for (Mood mood : Mood.values()) {
            for (String key : new String[] {mood.titleKey(), mood.adviceKey()}) {
                String shown = Component.translatable(key).getString();
                helper.assertTrue(!shown.isBlank() && !shown.equals(key),
                    "the language table has no words for " + key + ", got: " + shown);
            }
        }
        helper.assertTrue(Mood.of(null) == Mood.CONTENT, "no snapshot reads as content");
        helper.succeed();
    }
}