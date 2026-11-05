package io.github.izakyl.folkways.plugins.hours;

/**
 * PLA: WP-5 T1 — the three segments of a colony's day, as pure arithmetic on {@code dayTime}.
 *
 * <p>Minecraft's day is 24000 ticks: 0 is sunrise, 12000 is dusk, 18000 is midnight. Keeping this a
 * pure function means the whole timetable can be tested without a level, and the page can read the
 * same numbers the schedule does.
 */
public enum HoursPreset {

    /** No timetable at all: work whenever the colony wants (the default). */
    FREE(new Segment(0, 24000)),

    /** Up with the sun, a break at dusk, asleep before midnight. */
    DAY_SHIFT(new Segment(0, 12000), new Segment(12000, 13800), new Segment(13800, 24000)),

    /** For mines and workshops where the day is the night: work 13800–24000, but keep the break. */
    NIGHT_SHIFT(new Segment(13800, 24000), new Segment(0, 12000), new Segment(12000, 13800)),

    /** Emergencies: everyone works the whole day, breaks included. */
    SIEGE(new Segment(0, 24000));

    private final Segment[] segments;

    HoursPreset(Segment... segments) {
        this.segments = segments;
    }

    public record Segment(long from, long to) {
    }

    /** Whether this preset leaves the residents to work at {@code dayTime}. */
    public boolean working(long dayTime) {
        long now = Math.floorMod(dayTime, 24000L);
        return switch (this) {
            case FREE, SIEGE -> true;
            case DAY_SHIFT -> now < 12000;
            case NIGHT_SHIFT -> now >= 13800;
        };
    }

    /** The next dayTime at which {@link #working} changes, or 0 when it never does. */
    public long nextChange(long dayTime) {
        long now = Math.floorMod(dayTime, 24000L);
        return switch (this) {
            case FREE, SIEGE -> 0L;
            case DAY_SHIFT -> now < 12000 ? 12000L - now : 24000L - now;
            case NIGHT_SHIFT -> now < 13800 ? 13800L - now : 24000L - now + 13800L;
        };
    }

    public static HoursPreset named(String name) {
        for (HoursPreset preset : values()) {
            if (preset.name().equalsIgnoreCase(name)) {
                return preset;
            }
        }
        // The page stores the pick as the item that stands for it, so the path is what comes back.
        return switch (name == null ? "" : name.toLowerCase(java.util.Locale.ROOT)) {
            case "daylight_detector", "day_shift" -> DAY_SHIFT;
            case "lantern", "night_shift" -> NIGHT_SHIFT;
            case "iron_sword", "siege" -> SIEGE;
            default -> FREE;
        };
    }
}