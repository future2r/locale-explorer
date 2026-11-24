package name.ulbricht.locale.explorer.time;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/// How a local date-time maps onto a time zone.
///
/// @param kind     whether the local date-time is normal, in a gap or in an overlap
/// @param resolved the date-time as resolved by [ZonedDateTime#of(LocalDateTime, ZoneId)]
public record LocalTimeResolution(Kind kind, ZonedDateTime resolved) {

    /// The relation of a local date-time to the transitions of a zone.
    public enum Kind {
        /// Exactly one valid offset.
        NORMAL,
        /// No valid offset; the local time is skipped, typically when daylight saving time starts.
        GAP,
        /// Two valid offsets; the local time occurs twice, typically when daylight saving time ends.
        OVERLAP
    }

    /// Classifies a local date-time in a zone.
    ///
    /// @param dateTime the local date-time
    /// @param zone     the zone
    /// @return the classification and the resolved date-time
    public static LocalTimeResolution of(LocalDateTime dateTime, ZoneId zone) {
        var validOffsets = zone.getRules().getValidOffsets(dateTime);
        var kind = switch (validOffsets.size()) {
            case 0 -> Kind.GAP;
            case 1 -> Kind.NORMAL;
            default -> Kind.OVERLAP;
        };
        return new LocalTimeResolution(kind, ZonedDateTime.of(dateTime, zone));
    }

    /// Returns the offset in effect for the resolved date-time.
    ///
    /// @return the offset
    public ZoneOffset offset() {
        return resolved.getOffset();
    }
}
