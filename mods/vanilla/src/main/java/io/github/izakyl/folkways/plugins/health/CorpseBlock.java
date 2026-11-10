package io.github.izakyl.folkways.plugins.health;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * PLA: M2 R1-3 — the body on the ground: right-click deals with it and leaves a grave.
 */
public final class CorpseBlock extends BaseEntityBlock {

    /** The codec every entity-backed block needs; the body carries no state of its own in the block. */
    public static final com.mojang.serialization.MapCodec<CorpseBlock> CODEC =
        com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(instance ->
            instance.group(propertiesCodec()).apply(instance, CorpseBlock::new));

    public CorpseBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Rendered as a plain cube: no model of a person is attempted, and a shape nobody can misread
        // is better than a half-person.
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CorpseBlockEntity(pos, state);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            return Corpses.bury(server, pos) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }
}
