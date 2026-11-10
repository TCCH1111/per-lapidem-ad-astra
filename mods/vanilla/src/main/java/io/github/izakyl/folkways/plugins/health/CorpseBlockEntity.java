package io.github.izakyl.folkways.plugins.health;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * PLA: M2 R1-3 — who this body was, of what, when, and which colony must mark them dealt with.
 */
public final class CorpseBlockEntity extends BlockEntity {

    private String name = "someone";
    private String cause = "unknown";
    private long at;
    private UUID who;
    private UUID colony;

    public CorpseBlockEntity(BlockPos pos, BlockState state) {
        super(Corpses.CORPSE_BE.get(), pos, state);
    }

    void about(HealthState.Death death, UUID colonyId) {
        name = death.name();
        cause = death.cause();
        at = death.at();
        who = death.who();
        colony = colonyId;
        setChanged();
    }

    public String name() {
        return name;
    }

    public String cause() {
        return cause;
    }

    public long at() {
        return at;
    }

    public UUID who() {
        return who;
    }

    public UUID colony() {
        return colony;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("name", name);
        tag.putString("cause", cause);
        tag.putLong("at", at);
        if (who != null) {
            tag.putUUID("who", who);
        }
        if (colony != null) {
            tag.putUUID("colony", colony);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        name = tag.getString("name");
        cause = tag.getString("cause");
        at = tag.getLong("at");
        who = tag.hasUUID("who") ? tag.getUUID("who") : null;
        colony = tag.hasUUID("colony") ? tag.getUUID("colony") : null;
    }
}
