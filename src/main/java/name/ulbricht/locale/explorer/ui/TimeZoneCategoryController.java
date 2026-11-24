package name.ulbricht.locale.explorer.ui;

import static name.ulbricht.locale.explorer.ui.Controls.showError;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Predicate;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.Subscription;

import name.ulbricht.locale.explorer.i18n.Messages;
import name.ulbricht.locale.explorer.model.AppState;
import name.ulbricht.locale.explorer.text.Fact;
import name.ulbricht.locale.explorer.text.LocaleProperty;
import name.ulbricht.locale.explorer.zone.TimeZones;

/// Controller of the "Time zones" category.
///
/// The zone at the top is preset with the toolbar zone and can be changed for this category
/// only.
public final class TimeZoneCategoryController implements CategoryController {

    private static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");
    private static final List<String> DEFAULT_WORLD_ZONES = List.of(
            "Europe/Berlin", "America/New_York", "America/St_Johns", "Asia/Kolkata", "Asia/Kathmandu",
            "Australia/Lord_Howe", "Pacific/Chatham", "Pacific/Kiritimati");

    private final CategoryContext context;
    private final AppState state;
    private final ResourceBundle resources;
    private final Locale uiLocale;
    private final List<Subscription> subscriptions = new ArrayList<>();
    private final List<ZoneId> worldZones;
    private final List<String> zoneIds = ZoneId.getAvailableZoneIds().stream().sorted().toList();

    private ZoneId zone;
    private int filterYear;
    private int zoneYear;
    private LocaleTable names;
    private LocaleTable world;
    private FactTable rules;
    private FactTable transitions;
    private FactTable resolution;
    private SuggestionBox localDateTimeSuggestions;
    private FilterTable<ZoneId> all;

    @FXML
    private ComboBox<String> zoneBox;
    @FXML
    private TableView<LocaleProperty> namesTable;
    @FXML
    private TableView<Fact> rulesTable;
    @FXML
    private TableView<Fact> transitionsTable;
    @FXML
    private ComboBox<String> addZoneBox;
    @FXML
    private TableView<LocaleProperty> worldTable;
    @FXML
    private ComboBox<String> localDateTimeBox;
    @FXML
    private Label gapError;
    @FXML
    private TableView<Fact> resolutionTable;
    @FXML
    private TextField filterField;
    @FXML
    private CheckBox unusualCheck;
    @FXML
    private CheckBox dstCheck;
    @FXML
    private CheckBox shortIdCheck;
    @FXML
    private Label countLabel;
    @FXML
    private TableView<ZoneId> allTable;
    @FXML
    private TableColumn<ZoneId, String> allIdColumn;
    @FXML
    private TableColumn<ZoneId, String> allOffsetColumn;
    @FXML
    private TableColumn<ZoneId, String> allNameColumn;
    @FXML
    private TableColumn<ZoneId, String> allDstColumn;
    @FXML
    private TableColumn<ZoneId, String> allShortIdColumn;

    /// Creates the controller.
    ///
    /// @param context the category context
    public TimeZoneCategoryController(CategoryContext context) {
        this.context = context;
        this.state = context.state();
        this.resources = context.resources();
        this.uiLocale = state.getUiLanguage().locale();
        this.worldZones = new ArrayList<>(context.store().loadWorldZones(
                DEFAULT_WORLD_ZONES.stream().map(ZoneId::of).toList()));
    }

    @Override
    public void dispose() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
    }

    @FXML
    private void initialize() {
        names = new LocaleTable(namesTable, resources.getString("zone.pattern"), context);
        world = new LocaleTable(worldTable, resources.getString("zone.world.zone"), context);
        var property = resources.getString("common.property");
        var value = resources.getString("common.value");
        rules = new FactTable(rulesTable, property, value, context);
        transitions = new FactTable(transitionsTable, resources.getString("zone.transition"), value, context);
        resolution = new FactTable(resolutionTable, property, value, context);

        zoneBox.getItems().setAll(zoneIds);
        zoneBox.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        ChoiceHandler.install(zoneBox, text -> {
            try {
                setZone(ZoneId.of(text == null ? "" : text.strip()));
                zoneBox.pseudoClassStateChanged(ERROR, false);
            } catch (DateTimeException _) {
                zoneBox.pseudoClassStateChanged(ERROR, true);
            }
        });
        addZoneBox.getItems().setAll(zoneIds);
        addZoneBox.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);

        localDateTimeBox.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        localDateTimeBox.getStyleClass().add("mono");
        localDateTimeSuggestions = SuggestionBox.install(localDateTimeBox, context, "zone.localDateTime",
                List.of(), _ -> updateResolution(localDateTimeSuggestions.isUserCommit()));

        initAllZones();

        // The toolbar zone also moves the instant, so even the same zone needs new instant values.
        subscriptions.add(state.zoneProperty().subscribe(toolbarZone -> {
            if (toolbarZone.equals(zone)) {
                updateInstantDependent();
            } else {
                setZone(toolbarZone);
            }
        }));
        subscriptions.add(state.mainLocaleProperty().subscribe((_, _) -> updateLocales()));
        subscriptions.add(state.getCompareLocales().subscribe(this::updateLocales));
        subscriptions.add(state.dateTimeProperty().subscribe((_, _) -> updateDateTimeDependent()));
        updateLocales();
    }

    private Instant instant() {
        return state.resolution().resolved().toInstant();
    }

    private void setZone(ZoneId newZone) {
        // setting the zone box calls back with the same zone
        if (newZone.equals(zone)) {
            return;
        }
        zone = newZone;
        updateZoneDependent();
    }

    /// Shows a zone chosen with a button or a double click; the same zone is rebuilt too.
    private void showZone(ZoneId chosenZone) {
        if (chosenZone.equals(zone)) {
            updateZoneDependent();
        } else {
            setZone(chosenZone);
        }
    }

    /// Rebuilds what depends on the zone and on the year of the toolbar instant: the names,
    /// the gap and overlap suggestions, and everything that depends on the instant.
    private void updateZoneDependent() {
        // also replaces an invalid text left in the zone box
        zoneBox.setValue(zone.getId());
        zoneBox.pseudoClassStateChanged(ERROR, false);
        zoneYear = state.getDateTime().getYear();
        names.setRows(TimeZones.nameRows(zone, zoneYear));
        updateInstantDependent();

        var suggestions = new ArrayList<Suggestion>();
        for (var example : TimeZones.examples(zone, instant())) {
            suggestions.add(new Suggestion(example.dateTime().toString(),
                    example.gap() ? "suggest.zone.gap" : "suggest.zone.overlap"));
        }
        var normal = state.getDateTime().truncatedTo(ChronoUnit.MINUTES).toString();
        suggestions.add(new Suggestion(normal, "suggest.zone.toolbar"));
        localDateTimeSuggestions.setSuggestions(suggestions);
        localDateTimeSuggestions.setValue(suggestions.getFirst().value());
    }

    private void updateDateTimeDependent() {
        if (zone != null && state.getDateTime().getYear() != zoneYear) {
            updateZoneDependent();
        } else {
            updateInstantDependent();
        }
    }

    private void updateInstantDependent() {
        if (zone == null) {
            return;
        }
        rules.setFacts(TimeZones.ruleFacts(zone, instant()));
        transitions.setFacts(TimeZones.transitionFacts(zone, instant(), 6));
        world.setRows(TimeZones.worldClockRows(worldZones, instant()));
        // offsets in "All zones" depend on the instant, the filters on the instant and the year
        allTable.refresh();
        if (state.getDateTime().getYear() != filterYear || unusualCheck.isSelected()) {
            updateFilter();
        }
    }

    private void updateLocales() {
        var locales = state.columnLocales();
        names.setLocales(locales);
        world.setLocales(locales);
    }

    @FXML
    private void onUseToolbarZone() {
        showZone(state.getZone());
    }

    // --- World clock -------------------------------------------------------------------------

    @FXML
    private void onAddWorldZone() {
        try {
            addWorldZone(ZoneId.of(addZoneBox.getValue() == null ? "" : addZoneBox.getValue().strip()));
            addZoneBox.pseudoClassStateChanged(ERROR, false);
        } catch (DateTimeException _) {
            addZoneBox.pseudoClassStateChanged(ERROR, true);
        }
    }

    @FXML
    private void onRemoveWorldZone() {
        // only a selected row: the focus alone is set on the first row whenever rows are set
        var selected = worldTable.getSelectionModel().getSelectedCells();
        var row = selected.isEmpty() ? -1 : selected.getFirst().getRow();
        if (row >= 0 && row < worldZones.size()) {
            worldZones.remove(row);
            context.store().saveWorldZones(worldZones);
            updateInstantDependent();
        }
    }

    private void addWorldZone(ZoneId zoneId) {
        if (!worldZones.contains(zoneId)) {
            worldZones.add(zoneId);
            context.store().saveWorldZones(worldZones);
            updateInstantDependent();
        }
    }

    // --- Gap and overlap ---------------------------------------------------------------------

    private void updateResolution(boolean fromUser) {
        var text = localDateTimeBox.getValue() == null ? "" : localDateTimeBox.getValue().strip();
        try {
            var dateTime = LocalDateTime.parse(text);
            resolution.setFacts(TimeZones.resolutionFacts(dateTime, zone));
            showError(gapError, null);
            if (fromUser) {
                localDateTimeSuggestions.remember(text);
            }
        } catch (DateTimeParseException e) {
            resolution.setFacts(List.of());
            showError(gapError, Messages.format(resources, "zone.gap.invalid", e.getMessage()));
        }
    }

    // --- All zones ---------------------------------------------------------------------------

    private void initAllZones() {
        all = new FilterTable<>(allTable, zoneIds.stream().map(ZoneId::of).toList(), filterField, countLabel, context);
        allTable.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        allIdColumn.setCellValueFactory(features -> new ReadOnlyStringWrapper(features.getValue().getId()));
        allOffsetColumn.setCellValueFactory(features -> new ReadOnlyStringWrapper(
                features.getValue().getRules().getOffset(instant()).getId()));
        allNameColumn.setCellValueFactory(features -> new ReadOnlyStringWrapper(
                TimeZones.displayName(features.getValue(), uiLocale)));
        allDstColumn.setCellValueFactory(features -> new ReadOnlyStringWrapper(
                TimeZones.hasDaylightSaving(features.getValue(), state.getDateTime().getYear()) ? "✓" : ""));
        allShortIdColumn.setCellValueFactory(features -> new ReadOnlyStringWrapper(TimeZones.shortIds(features.getValue())));

        all.onDoubleClick(this::showZone);

        filterField.textProperty().subscribe((_, _) -> updateFilter());
        unusualCheck.selectedProperty().subscribe((_, _) -> updateFilter());
        dstCheck.selectedProperty().subscribe((_, _) -> updateFilter());
        shortIdCheck.selectedProperty().subscribe((_, _) -> updateFilter());
        updateFilter();
    }

    private void updateFilter() {
        var query = all.query();
        var now = instant();
        var year = state.getDateTime().getYear();
        filterYear = year;
        Predicate<ZoneId> predicate = zoneId -> query.isEmpty()
                || zoneId.getId().toLowerCase(Locale.ROOT).contains(query)
                || TimeZones.displayName(zoneId, uiLocale).toLowerCase(uiLocale).contains(query);
        if (unusualCheck.isSelected()) {
            predicate = predicate.and(zoneId -> TimeZones.isUnusual(zoneId.getRules().getOffset(now)));
        }
        if (dstCheck.isSelected()) {
            predicate = predicate.and(zoneId -> TimeZones.hasDaylightSaving(zoneId, year));
        }
        if (shortIdCheck.isSelected()) {
            predicate = predicate.and(zoneId -> !TimeZones.shortIds(zoneId).isEmpty());
        }
        all.filter(predicate);
    }

    @FXML
    private void onAllUseZone() {
        var selected = allTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showZone(selected);
        }
    }

    @FXML
    private void onAllAddWorld() {
        var selected = allTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            addWorldZone(selected);
        }
    }
}
