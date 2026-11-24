package name.ulbricht.locale.explorer.datetime;

import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;

/// The `java.time` type a value is formatted as.
///
/// Localized formats with a time zone name fail for types without a zone; showing that is part
/// of the point.
public enum TemporalKind {

    ZONED_DATE_TIME("ZonedDateTime", "zonedDateTime"),
    OFFSET_DATE_TIME("OffsetDateTime", "zonedDateTime.toOffsetDateTime()"),
    LOCAL_DATE_TIME("LocalDateTime", "zonedDateTime.toLocalDateTime()"),
    LOCAL_DATE("LocalDate", "zonedDateTime.toLocalDate()"),
    LOCAL_TIME("LocalTime", "zonedDateTime.toLocalTime()"),
    INSTANT("Instant", "zonedDateTime.toInstant()");

    private final String typeName;
    private final String expression;

    TemporalKind(String typeName, String expression) {
        this.typeName = typeName;
        this.expression = expression;
    }

    /// Returns the simple name of the Java type.
    ///
    /// @return for example `LocalDateTime`
    public String typeName() {
        return typeName;
    }

    /// Returns the Java expression that converts a `zonedDateTime` variable to this type.
    ///
    /// @return the expression
    public String expression() {
        return expression;
    }

    /// Converts a zoned date-time to this type.
    ///
    /// @param dateTime the date-time
    /// @return the converted value
    public TemporalAccessor from(ZonedDateTime dateTime) {
        return switch (this) {
            case ZONED_DATE_TIME -> dateTime;
            case OFFSET_DATE_TIME -> dateTime.toOffsetDateTime();
            case LOCAL_DATE_TIME -> dateTime.toLocalDateTime();
            case LOCAL_DATE -> dateTime.toLocalDate();
            case LOCAL_TIME -> dateTime.toLocalTime();
            case INSTANT -> dateTime.toInstant();
        };
    }
}
