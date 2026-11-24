package name.ulbricht.locale.explorer.model;

/// The categories in the navigation list on the left.
public enum Category {

    LOCALE("locale"),
    DATE_TIME("dateTime"),
    CALENDAR("calendar"),
    TIME_ZONES("timeZones"),
    NUMBERS("numbers"),
    CURRENCY("currency"),
    LISTS_TEXT("listsText"),
    SORTING("sorting"),
    SYSTEM("system");

    private final String id;

    Category(String id) {
        this.id = id;
    }

    /// Returns the stable identifier, used for preferences and resource names.
    ///
    /// @return the identifier
    public String id() {
        return id;
    }

    /// Returns the key of the translated name.
    ///
    /// @return the message key
    public String messageKey() {
        return "category." + id;
    }

    /// Finds a category by its identifier, falling back to [#LOCALE].
    ///
    /// @param id the identifier, may be `null`
    /// @return the category
    public static Category fromId(String id) {
        for (var category : values()) {
            if (category.id.equals(id)) {
                return category;
            }
        }
        return LOCALE;
    }
}
