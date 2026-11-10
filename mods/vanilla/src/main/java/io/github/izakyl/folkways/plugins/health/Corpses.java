package io.github.izakyl.folkways.plugins.health;

import io.github.izakyl.folkways.FolkwaysMod;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * PLA: M2 R1-3 — a body stays where its owner fell, and a grave replaces it once somebody deals with it.
 *
 * <p>Made of a block rather than an entity on purpose: the colony's blocks are what the server can be
 * tested against without a client, and a body that a player cannot see is worse than a body that is
 * plainly a block. Art is a placeholder until there is art.
 */
public final class Corpses {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FolkwaysMod.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> TYPES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, FolkwaysMod.MOD_ID);

    public static final DeferredBlock<CorpseBlock> CORPSE = BLOCKS.register("corpse",
        () -> new CorpseBlock(BlockBehaviour.Properties.of().strength(0.4F).noOcclusion()));

    public static final DeferredBlock<Block> GRAVE = BLOCKS.register("grave",
        () -> new Block(BlockBehaviour.Properties.of().strength(1.0F)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CorpseBlockEntity>> CORPSE_BE =
        TYPES.register("corpse", () -> BlockEntityType.Builder.of(CorpseBlockEntity::new, CORPSE.get()).build(null));

    private Corpses() {
    }

    public static void install(IEventBus modBus) {
        BLOCKS.register(modBus);
        TYPES.register(modBus);
    }

    /**
     * Leaves a body where somebody died, if the ground allows it.
     *
     * @return whether a corpse was placed
     */
    public static boolean place(ServerLevel level, BlockPos at, HealthState.Death death, UUID colony) {
        BlockPos target = at;
        if (!level.getBlockState(target).canBeReplaced()) {
            target = target.above();
        }
        if (!level.getBlockState(target).canBeReplaced()) {
            return false;
        }
        if (!level.setBlock(target, CORPSE.get().defaultBlockState(), Block.UPDATE_ALL)) {
            return false;
        }
        if (level.getBlockEntity(target) instanceof CorpseBlockEntity corpse) {
            corpse.about(death, colony);
            return true;
        }
        return false;
    }

    /**
     * Deals with a body: the grave takes its place and the colony's record is marked.
     *
     * @return whether there was a body to bury
     */
    public static boolean bury(ServerLevel level, BlockPos at) {
        if (!(level.getBlockEntity(at) instanceof CorpseBlockEntity corpse)) {
            return false;
        }
        UUID colony = corpse.colony();
        UUID who = corpse.who();
        level.setBlock(at, GRAVE.get().defaultBlockState(), Block.UPDATE_ALL);
        if (colony != null) {
            net.minecraft.server.MinecraftServer server = level.getServer();
            io.github.izakyl.folkways.core.api.colony.Colonies.of(server, colony).ifPresent(found ->
                found.service(HealthContent.ID, HealthState.class).ifPresent(state -> state.bury(who)));
        }
        return true;
    }

    /** The body belonging to this resident near a point, if one was left there. */
    public static java.util.Optional<BlockPos> bodyOf(ServerLevel level, BlockPos around, UUID who) {
        for (BlockPos probe : BlockPos.betweenClosed(around.offset(-2, -1, -2), around.offset(2, 2, 2))) {
            if (level.getBlockEntity(probe) instanceof CorpseBlockEntity corpse
                && who.equals(corpse.who())) {
                return java.util.Optional.of(probe.immutable());
            }
        }
        return java.util.Optional.empty();
    }

    /** Whether a block state is a body waiting to be dealt with. */
    public static boolean isCorpse(BlockState state) {
        return state.is(CORPSE.get());
    }
}
