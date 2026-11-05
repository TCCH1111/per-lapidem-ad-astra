package io.github.izakyl.folkways.core.api.resident.body;

import io.github.izakyl.folkways.core.api.work.Balk;
import io.github.izakyl.folkways.core.api.work.Doing;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.entity.Entity;

/**
 * PLA: read-only view over the package-private {@link BodyState}, for the diagnosis layer
 * (docs/plans/PLAN-002-wp1-engine-readable-surface.md, T3).
 *
 * <p>Why a facade instead of making {@code BodyState} public: its members include mutators
 * ({@code bindTo}, {@code nowDoing}, {@code balked}, {@code credit}). Widening the class would hand
 * every caller the ability to write state. This facade lives in the same package, so it can read the
 * package-private class and the package-private attachment, and it hands back only immutable values.
 *
 * <p>Nothing here may trigger planning, touch the level, or mark anything dirty: it is a pure read.
 */
public final class BodyViews {

    private BodyViews() {
    }

    /** The view for any entity, or empty when the entity cannot hold attachments. */
    public static Optional<BodyView> of(Entity entity) {
        return entity == null ? Optional.empty() : Optional.of(view(entity.getData(FolkwaysAttachments.BODY_STATE)));
    }

    /** The view for a body the colony already knows about. */
    public static BodyView of(Body body) {
        return view(body.mob().getData(FolkwaysAttachments.BODY_STATE));
    }

    private static BodyView view(BodyState state) {
        return new BodyView(state.colony(), state.doing(), state.balk(), state.pack().getContainerSize());
    }

    /**
     * An immutable picture of a body's state.
     *
     * @param colony the colony the body belongs to, if any
     * @param doing  what it is doing right now, if anything
     * @param balk   a standing refusal, if any
     * @param packSlots how many cells its pack offers right now
     */
    public record BodyView(Optional<UUID> colony, Optional<Doing> doing, Optional<Balk> balk, int packSlots) {

        public boolean inColony() {
            return colony.isPresent();
        }

        public boolean idle() {
            return doing.isEmpty();
        }

        public boolean balking() {
            return balk.isPresent();
        }
    }
}