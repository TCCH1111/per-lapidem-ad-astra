package io.github.izakyl.folkways.plugins.hours;

import io.github.izakyl.folkways.base.persist.Reader;
import io.github.izakyl.folkways.base.persist.Writer;
import net.minecraft.nbt.CompoundTag;

/**
 * PLA: WP-5 T1 — one colony's timetable and the clock it reads.
 *
 * <p>Off by default and off unless the player turns it on, which is the whole point: a colony that
 * never opens the page behaves exactly as it did before this plugin existed. The clock is only ever
 * written by the colony's own tick, never read from the level by the schedule, so the veto stays a
 * pure read of this object.
 */
public final class HoursState {

    private static final String TAG_ENABLED = "enabled";
    private static final String TAG_PRESET = "preset";
    private static final String TAG_HOUR = "hour";

    private boolean enabled;
    private HoursPreset preset = HoursPreset.FREE;
    private long dayTime;

    public boolean enabled() {
        return enabled;
    }

    public HoursPreset preset() {
        return preset;
    }

    public void enabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void preset(HoursPreset preset) {
        this.preset = preset == null ? HoursPreset.FREE : preset;
    }

    public void clock(long dayTime) {
        this.dayTime = dayTime;
    }

    /** Whether the colony works at the clock it last saw. */
    public boolean working() {
        return !enabled || preset.working(dayTime);
    }

    public long nextChange() {
        return enabled ? preset.nextChange(dayTime) : 0L;
    }

    public CompoundTag save() {
        return Writer.of()
            .flag(TAG_ENABLED, enabled)
            .string(TAG_PRESET, preset.name())
            .longValue(TAG_HOUR, dayTime)
            .tag();
    }

    public static HoursState load(Reader reader) {
        HoursState state = new HoursState();
        state.enabled = reader.flag(TAG_ENABLED).orElse(false);
        state.preset = HoursPreset.named(reader.string(TAG_PRESET).orElse(HoursPreset.FREE.name()));
        state.dayTime = reader.longValue(TAG_HOUR).orElse(0L);
        return state;
    }
}