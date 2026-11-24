package name.ulbricht.locale.explorer.model;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import name.ulbricht.locale.explorer.i18n.UiLanguage;
import name.ulbricht.locale.explorer.time.LocalTimeResolution;

/// The state of the application, shared by all views.
///
/// Views are rebuilt when the UI language changes, so everything that must survive that lives
/// here rather than in the controllers.
public final class AppState {

    /// Smallest selectable font size in pixels.
    public static final int MIN_FONT_SIZE = 10;
    /// Largest selectable font size in pixels.
    public static final int MAX_FONT_SIZE = 24;

    private final ObjectProperty<UiLanguage> uiLanguage = new SimpleObjectProperty<>(this, "uiLanguage");
    private final ObjectProperty<Theme> theme = new SimpleObjectProperty<>(this, "theme");
    private final IntegerProperty fontSize = new SimpleIntegerProperty(this, "fontSize");
    private final ObjectProperty<Locale> mainLocale = new SimpleObjectProperty<>(this, "mainLocale");
    private final ObservableList<Locale> compareLocales = FXCollections.observableArrayList();
    private final ObjectProperty<ZoneId> zone = new SimpleObjectProperty<>(this, "zone");
    private final ObjectProperty<LocalDateTime> dateTime = new SimpleObjectProperty<>(this, "dateTime");
    private final BooleanProperty live = new SimpleBooleanProperty(this, "live");
    private final ObjectProperty<Category> category = new SimpleObjectProperty<>(this, "category");

    /// Creates the state with the given initial values.
    ///
    /// @param settings the initial values
    public AppState(Settings settings) {
        uiLanguage.set(settings.uiLanguage());
        theme.set(settings.theme());
        fontSize.set(Math.clamp(settings.fontSize(), MIN_FONT_SIZE, MAX_FONT_SIZE));
        mainLocale.set(settings.mainLocale());
        compareLocales.setAll(settings.compareLocales());
        zone.set(settings.zone());
        dateTime.set(settings.dateTime());
        category.set(settings.category());
    }

    /// Captures the current values.
    ///
    /// @return the settings
    public Settings toSettings() {
        return new Settings(getUiLanguage(), getTheme(), getFontSize(), getMainLocale(),
                List.copyOf(compareLocales), getZone(), getDateTime(), getCategory());
    }

    /// Returns the main locale followed by the compare locales, without duplicates.
    ///
    /// These are the columns of the result tables.
    ///
    /// @return the locales in column order
    public List<Locale> columnLocales() {
        return Stream.concat(Stream.of(getMainLocale()), compareLocales.stream()).distinct().toList();
    }

    /// Resolves the local date-time in the selected zone.
    ///
    /// @return the resolution, including gap and overlap information
    public LocalTimeResolution resolution() {
        return LocalTimeResolution.of(getDateTime(), getZone());
    }

    public ObjectProperty<UiLanguage> uiLanguageProperty() {
        return uiLanguage;
    }

    public UiLanguage getUiLanguage() {
        return uiLanguage.get();
    }

    public void setUiLanguage(UiLanguage value) {
        uiLanguage.set(value);
    }

    public ObjectProperty<Theme> themeProperty() {
        return theme;
    }

    public Theme getTheme() {
        return theme.get();
    }

    public void setTheme(Theme value) {
        theme.set(value);
    }

    public IntegerProperty fontSizeProperty() {
        return fontSize;
    }

    public int getFontSize() {
        return fontSize.get();
    }

    public void setFontSize(int value) {
        fontSize.set(Math.clamp(value, MIN_FONT_SIZE, MAX_FONT_SIZE));
    }

    public ObjectProperty<Locale> mainLocaleProperty() {
        return mainLocale;
    }

    public Locale getMainLocale() {
        return mainLocale.get();
    }

    public void setMainLocale(Locale value) {
        mainLocale.set(value);
    }

    public ObservableList<Locale> getCompareLocales() {
        return compareLocales;
    }

    public ObjectProperty<ZoneId> zoneProperty() {
        return zone;
    }

    public ZoneId getZone() {
        return zone.get();
    }

    public void setZone(ZoneId value) {
        zone.set(value);
    }

    public ObjectProperty<LocalDateTime> dateTimeProperty() {
        return dateTime;
    }

    public LocalDateTime getDateTime() {
        return dateTime.get();
    }

    public void setDateTime(LocalDateTime value) {
        dateTime.set(value);
    }

    public BooleanProperty liveProperty() {
        return live;
    }

    public boolean isLive() {
        return live.get();
    }

    public void setLive(boolean value) {
        live.set(value);
    }

    public ObjectProperty<Category> categoryProperty() {
        return category;
    }

    public Category getCategory() {
        return category.get();
    }

    public void setCategory(Category value) {
        category.set(value);
    }
}
