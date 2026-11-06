package io.github.izakyl.folkways.plugins.person.living;

import io.github.izakyl.folkways.FolkwaysMod;
import io.github.izakyl.folkways.core.api.colony.Colonies;
import io.github.izakyl.folkways.core.api.colony.Colony;
import io.github.izakyl.folkways.core.api.colony.ColonyView;
import io.github.izakyl.folkways.core.api.resident.Resident;
import io.github.izakyl.folkways.core.api.resident.body.Body;
import io.github.izakyl.folkways.core.api.terms.WorldPos;
import io.github.izakyl.folkways.core.api.work.Node;
import io.github.izakyl.folkways.core.api.work.Placement;
import io.github.izakyl.folkways.core.api.work.ToolNeed;
import io.github.izakyl.folkways.core.api.work.Urge;
import io.github.izakyl.folkways.core.api.work.Worker;
import io.github.izakyl.folkways.core.engine.colony.ColonyData;
import io.github.izakyl.folkways.core.engine.colony.ColonySnapshot;
import io.github.izakyl.folkways.effect.Effects;
import io.github.izakyl.folkways.plugins.FolkwaysVanillaMod;
import io.github.izakyl.folkways.plugins.health.HealthContent;
import io.github.izakyl.folkways.plugins.health.HealthState;
import io.github.izakyl.folkways.plugins.person.PersonBody;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * PLA: M2 R0-3/R1 — the end-to-end question left open twice: does a badly hurt resident actually stop
 * being offered planned work inside a real colony?
 *
 * <p>The chain is the real one: a colony is minted, its health book is reached through the service the
 * plugin shares, a wound is written into it, and the urge list the labour system asks for is inspected.
 * Nothing here fakes the book or the presence.
 */
@EventBusSubscriber(modid = FolkwaysVanillaMod.ID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(FolkwaysMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HealthWorkStopGameTests {

    private static final String TEMPLATE = "empty";

    private HealthWorkStopGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(HealthWorkStopGameTests.class);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void aBadlyHurtResidentIsNotOfferedPlannedWork(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Colony colony = Colonies.mint(level.getServer());
        try {
            HealthState book = colony.service(HealthContent.ID, HealthState.class)
                .orElseThrow(() -> new AssertionError("the colony has no health book"));
            LivingPresence presence = colony.service(LivingContent.ID, LivingPresence.class)
                .orElseThrow(() -> new AssertionError("the colony has no living presence"));
            ColonyData data = ColonyData.find(level.getServer(), colony.id()).orElseThrow();
            ColonyView view = ColonySnapshot.of(data, level);

            Body body = PersonBody.RESIDENT.get().create(level);
            body.mob().moveTo(helper.absolutePos(new net.minecraft.core.BlockPos(2, 1, 2)), 0.0F, 0.0F);
            UUID who = body.resident().id();
            Worker worker = workerFor(body);

            // A day with no wound: nothing is added.
            List<Urge> healthy = presence.urges(worker, view);
            helper.assertTrue(healthy.stream().noneMatch(Urge::reservation),
                "an untouched resident is not reserved against work, got " + healthy);

            // A deep wound: the contract's reservation appears.
            book.wound(who, new HealthState.Wound("torso", "gash", 4, "axe", 0L, false));
            List<Urge> hurt = presence.urges(worker, view);
            helper.assertTrue(hurt.stream().anyMatch(Urge::reservation),
                "a badly hurt resident is reserved against planned work, got " + hurt);
            helper.assertTrue(hurt.stream().anyMatch(one -> one.id().equals(HealthContent.RESTING)),
                "and the reservation carries the health plugin's id, got " + hurt);
            body.mob().discard();
        } finally {
            colony.raze();
        }
        helper.succeed();
    }

    /** The narrowest worker the urge list actually reads. */
    private static Worker workerFor(Body body) {
        return new Worker() {
            private final Container pack = new SimpleContainer(1);

            @Override
            public Resident resident() {
                return body.resident();
            }

            @Override
            public Mob body() {
                return body.mob();
            }

            @Override
            public Container pack() {
                return pack;
            }

            @Override
            public List<Store> within() {
                return List.of();
            }

            @Override
            public ItemStack held(ToolNeed need) {
                return ItemStack.EMPTY;
            }

            @Override
            public void spill(ItemStack stack) {
            }

            @Override
            public Effects effects() {
                return Effects.NONE;
            }

            @Override
            public List<Node> route() {
                return List.of();
            }

            @Override
            public Placement placement() {
                return Placement.NOWHERE;
            }
        };
    }
}
