package name.ulbricht.locale.explorer.model;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import name.ulbricht.locale.explorer.i18n.UiLanguage;
import name.ulbricht.locale.explorer.text.Locales;

/// Loads and saves [Settings] with [java.util.prefs].
///
/// The date-time is not stored; every start uses the current time.
public final class PreferencesStore {

    private static final String UI_LANGUAGE = "uiLanguage";
    private static final String THEME = "theme";
    private static final String FONT_SIZE = "fontSize";
    private static final String MAIN_LOCALE = "mainLocale";
    private static final String COMPARE_LOCALES = "compareLocales";
    private static final String ZONE = "zone";
    private static final String CATEGORY = "category";

    private static final String WORLD_ZONES = "worldZones";
    private static final String HISTORY_PREFIX = "history.";
    private static final String HISTORY_SEPARATOR = "\u001F";

    /// Maximum number of entries kept per input field.
    public static final int MAX_HISTORY = 10;

    /// Longest entry kept in a history; longer entries are not remembered.
    public static final int MAX_HISTORY_ENTRY_LENGTH = 500;

    private static final int DEFAULT_FONT_SIZE = 13;
    private static final String DEFAULT_COMPARE_LOCALES = "en-US";

    private final Preferences node;

    /// Creates a store on the user preferences node of the application.
    public PreferencesStore() {
        this(Preferences.userRoot().node("name/ulbricht/locale-explorer"));
    }

    /// Creates a store on the given node.
    ///
    /// @param node the preferences node
    public PreferencesStore(Preferences node) {
        this.node = node;
    }

    /// Loads the settings, using defaults for missing or invalid values.
    ///
    /// @return the settings
    public Settings load() {
        var uiLanguage = UiLanguage.fromTag(node.get(UI_LANGUAGE, null))
                .orElseGet(() -> UiLanguage.fromLocale(Locale.getDefault(Locale.Category.DISPLAY)));
        var mainLocale = Locales.parseTags(node.get(MAIN_LOCALE, null)).stream()
                .findFirst()
                .orElseGet(() -> Locale.getDefault(Locale.Category.FORMAT));
        var compareLocales = Locales.parseTags(node.get(COMPARE_LOCALES, DEFAULT_COMPARE_LOCALES));
        var zone = parseZone(node.get(ZONE, null));
        return new Settings(
                uiLanguage,
                Theme.parse(node.get(THEME, null)),
                node.getInt(FONT_SIZE, DEFAULT_FONT_SIZE),
                mainLocale,
                compareLocales,
                zone,
                LocalDateTime.now(zone).truncatedTo(ChronoUnit.SECONDS),
                Category.fromId(node.get(CATEGORY, null)));
    }

    /// Saves the settings.
    ///
    /// @param settings the settings
    public void save(Settings settings) {
        node.put(UI_LANGUAGE, settings.uiLanguage().tag());
        node.put(THEME, settings.theme().name());
        node.putInt(FONT_SIZE, settings.fontSize());
        node.put(MAIN_LOCALE, settings.mainLocale().toLanguageTag());
        node.put(COMPARE_LOCALES, Locales.formatTags(settings.compareLocales()));
        node.put(ZONE, settings.zone().getId());
        node.put(CATEGORY, settings.category().id());
        flush();
    }

    /// Loads the zones of the world clock.
    ///
    /// Unknown zone IDs are skipped.
    ///
    /// @param defaults the zones to use if none are stored
    /// @return the zones in display order
    public List<ZoneId> loadWorldZones(List<ZoneId> defaults) {
        var stored = node.get(WORLD_ZONES, null);
        if (stored == null) {
            return defaults;
        }
        var zones = new ArrayList<ZoneId>();
        for (var id : stored.split(",")) {
            if (!id.isBlank()) {
                try {
                    zones.add(ZoneId.of(id.strip()));
                } catch (DateTimeException _) {
                    // skip zones that no longer exist
                }
            }
        }
        return List.copyOf(zones);
    }

    /// Saves the zones of the world clock.
    ///
    /// @param zones the zones in display order
    public void saveWorldZones(List<ZoneId> zones) {
        node.put(WORLD_ZONES, String.join(",", zones.stream().map(ZoneId::getId).toList()));
        flush();
    }

    /// Loads the recently used entries of an input field, most recent first.
    ///
    /// @param fieldId the identifier of the field
    /// @return the entries
    public List<String> loadHistory(String fieldId) {
        var stored = node.get(HISTORY_PREFIX + fieldId, "");
        return stored.isEmpty() ? List.of() : List.of(stored.split(HISTORY_SEPARATOR));
    }

    /// Adds an entry to the history of an input field.
    ///
    /// The entry moves to the front; at most [#MAX_HISTORY] entries are kept. Entries longer than
    /// [#MAX_HISTORY_ENTRY_LENGTH] or containing the internal separator are not remembered, so the
    /// stored value stays within [Preferences#MAX_VALUE_LENGTH].
    ///
    /// @param fieldId the identifier of the field
    /// @param entry   the entry
    /// @return the updated history, most recent first
    public List<String> addHistory(String fieldId, String entry) {
        if (entry == null || entry.isBlank() || entry.length() > MAX_HISTORY_ENTRY_LENGTH
                || entry.contains(HISTORY_SEPARATOR)) {
            return loadHistory(fieldId);
        }
        var history = new ArrayList<String>(loadHistory(fieldId));
        history.remove(entry);
        history.addFirst(entry);
        while (history.size() > MAX_HISTORY
                || String.join(HISTORY_SEPARATOR, history).length() > Preferences.MAX_VALUE_LENGTH) {
            history.removeLast();
        }
        node.put(HISTORY_PREFIX + fieldId, String.join(HISTORY_SEPARATOR, history));
        flush();
        return List.copyOf(history);
    }

    private static ZoneId parseZone(String id) {
        if (id != null) {
            try {
                return ZoneId.of(id);
            } catch (DateTimeException _) {
                // fall through to the system default
            }
        }
        return ZoneId.systemDefault();
    }

    private void flush() {
        try {
            node.flush();
        } catch (BackingStoreException e) {
            System.getLogger(PreferencesStore.class.getName())
                    .log(System.Logger.Level.WARNING, "Could not save preferences", e);
        }
    }
}
