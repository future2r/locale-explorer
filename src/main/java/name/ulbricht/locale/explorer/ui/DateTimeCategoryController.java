package name.ulbricht.locale.explorer.ui;

import static name.ulbricht.locale.explorer.ui.Controls.showError;
import static name.ulbricht.locale.explorer.ui.Controls.readOnly;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Subscription;

import name.ulbricht.locale.explorer.datetime.DateTimeFormats;
import name.ulbricht.locale.explorer.datetime.DateTimeNames;
import name.ulbricht.locale.explorer.datetime.DateTimeParsing;
import name.ulbricht.locale.explorer.datetime.LegacyDateFormats;
import name.ulbricht.locale.explorer.datetime.TemporalKind;
import name.ulbricht.locale.explorer.i18n.Messages;
import name.ulbricht.locale.explorer.model.AppState;
import name.ulbricht.locale.explorer.text.LocaleProperty;
import name.ulbricht.locale.explorer.text.TextDirection;

/// Controller of the "Date and time" category.
public final class DateTimeCategoryController implements CategoryController {

    private static final PseudoClass DIFFERS = PseudoClass.getPseudoClass("differs");

    private static final List<String> PRESET_SKELETONS =
            List.of("yMMMEd", "yMMMMd", "yMd", "MMMd", "yQQQ", "Hm", "jm", "jmsv", "Bhm", "GGGGGyMd");

    private static final List<Suggestion> SKELETON_SUGGESTIONS = List.of(
            new Suggestion("yMMMEd", "suggest.skeleton.weekdayDate"),
            new Suggestion("yMMMMd", "suggest.skeleton.longDate"),
            new Suggestion("yMd", "suggest.skeleton.numericDate"),
            new Suggestion("MMMd", "suggest.skeleton.withoutYear"),
            new Suggestion("yQQQ", "suggest.skeleton.quarter"),
            new Suggestion("Hm", "suggest.skeleton.hour24"),
            new Suggestion("jm", "suggest.skeleton.preferredHour"),
            new Suggestion("jmsv", "suggest.skeleton.genericZone"),
            new Suggestion("Bhm", "suggest.skeleton.dayPeriod"),
            new Suggestion("GGGGGyMd", "suggest.skeleton.era"));

    private static final List<Suggestion> PATTERN_SUGGESTIONS = List.of(
            new Suggestion("EEEE, d. MMMM uuuu", "suggest.pattern.long"),
            new Suggestion("dd.MM.YYYY", "suggest.pattern.weekYearTrap"),
            new Suggestion("uuuu-'W'ww-e", "suggest.pattern.week"),
            new Suggestion("h:mm B", "suggest.pattern.dayPeriod"),
            new Suggestion("QQQQ uuuu", "suggest.pattern.quarter"),
            new Suggestion("uuuu-MM-dd'T'HH:mm:ssXXX", "suggest.pattern.iso"),
            new Suggestion("LLLL / d MMMM", "suggest.pattern.standalone"),
            new Suggestion("zzzz (VV)", "suggest.pattern.zone"));

    private static final List<Suggestion> PARSE_SUGGESTIONS = List.of(
            new Suggestion("24.11.26", "suggest.parse.german"),
            new Suggestion("11/24/26", "suggest.parse.us"),
            new Suggestion("24 nov. 2026", "suggest.parse.french"),
            new Suggestion("2026-02-30", "suggest.parse.invalidDate"),
            new Suggestion("١٢/١١/٢٠٢٦", "suggest.parse.arabicDigits"));

    private final CategoryContext context;
    private final AppState state;
    private final ResourceBundle resources;
    private final List<Subscription> subscriptions = new ArrayList<>();

    private LocaleTable styles;
    private LocaleTable skeletons;
    private LocaleTable patterns;
    private LocaleTable parsing;
    private LocaleTable legacy;
    private SuggestionBox skeletonSuggestions;
    private SuggestionBox patternSuggestions;
    private SuggestionBox parseSuggestions;
    private String skeleton = "yMMMEd";
    private String pattern = "EEEE, d. MMMM uuuu";

    @FXML
    private ComboBox<TemporalKind> kindBox;
    @FXML
    private CheckBox matrixCheck;
    @FXML
    private CheckBox patternsCheck;
    @FXML
    private TableView<LocaleProperty> stylesTable;
    @FXML
    private ComboBox<String> skeletonBox;
    @FXML
    private Label skeletonError;
    @FXML
    private TableView<LocaleProperty> skeletonTable;
    @FXML
    private ComboBox<String> patternBox;
    @FXML
    private Label patternError;
    @FXML
    private TableView<LocaleProperty> patternTable;
    @FXML
    private ComboBox<Locale> namesLocaleBox;
    @FXML
    private ComboBox<DateTimeNames.Kind> namesKindBox;
    @FXML
    private TableView<TemporalAccessor> namesTable;
    @FXML
    private ComboBox<String> parseInputBox;
    @FXML
    private ComboBox<DateTimeParsing.Parser> parserBox;
    @FXML
    private ComboBox<ResolverStyle> resolverBox;
    @FXML
    private TableView<LocaleProperty> parseTable;
    @FXML
    private TableView<LocaleProperty> legacyTable;

    /// Creates the controller.
    ///
    /// @param context the category context
    public DateTimeCategoryController(CategoryContext context) {
        this.context = context;
        this.state = context.state();
        this.resources = context.resources();
    }

    @Override
    public void dispose() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
    }

    @FXML
    private void initialize() {
        var format = resources.getString("datetime.format");
        styles = new LocaleTable(stylesTable, format, context);
        skeletons = new LocaleTable(skeletonTable, format, context);
        patterns = new LocaleTable(patternTable, format, context);
        parsing = new LocaleTable(parseTable, format, context);
        legacy = new LocaleTable(legacyTable, format, context);

        initStyles();
        initSkeletons();
        initPatterns();
        initNames();
        initParsing();

        subscriptions.add(state.mainLocaleProperty().subscribe((_, _) -> updateLocales()));
        subscriptions.add(state.getCompareLocales().subscribe(this::updateLocales));
        subscriptions.add(state.dateTimeProperty().subscribe((_, _) -> updateRows()));
        subscriptions.add(state.zoneProperty().subscribe((_, _) -> updateRows()));
        updateLocales();
        updateRows();
    }

    private ZonedDateTime dateTime() {
        return state.resolution().resolved();
    }

    private void updateLocales() {
        var locales = state.columnLocales();
        for (var table : List.of(styles, skeletons, patterns, parsing, legacy)) {
            table.setLocales(locales);
        }
        var selected = namesLocaleBox.getValue();
        namesLocaleBox.getItems().setAll(locales);
        namesLocaleBox.setValue(selected != null && locales.contains(selected) ? selected : locales.getFirst());
    }

    private void updateRows() {
        updateStyles();
        updateSkeletons();
        updatePatterns();
        updateParsing();
        legacy.setRows(LegacyDateFormats.rows(dateTime()));
    }

    // --- Format styles -----------------------------------------------------------------------

    private void initStyles() {
        kindBox.getItems().setAll(TemporalKind.values());
        kindBox.setConverter(readOnly(TemporalKind::typeName));
        kindBox.setValue(TemporalKind.ZONED_DATE_TIME);
        kindBox.setOnAction(_ -> updateStyles());
        matrixCheck.setOnAction(_ -> updateStyles());
        patternsCheck.setOnAction(_ -> updateStyles());
    }

    private void updateStyles() {
        styles.setRows(DateTimeFormats.styleRows(dateTime(), kindBox.getValue(),
                matrixCheck.isSelected(), patternsCheck.isSelected()));
    }

    // --- Skeletons ---------------------------------------------------------------------------

    private void initSkeletons() {
        skeletonBox.getStyleClass().add("mono");
        skeletonBox.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        skeletonSuggestions = SuggestionBox.install(skeletonBox, context, "datetime.skeleton",
                SKELETON_SUGGESTIONS, value -> {
                    skeleton = value.strip();
                    if (updateSkeletons()) {
                        skeletonSuggestions.remember(skeleton);
                    }
                });
        skeletonSuggestions.show(skeleton);
    }

    private boolean updateSkeletons() {
        var list = new ArrayList<String>();
        var valid = true;
        if (!skeleton.isEmpty()) {
            try {
                DateTimeFormatter.ofLocalizedPattern(skeleton);
                list.add(skeleton);
                showError(skeletonError, null);
            } catch (IllegalArgumentException e) {
                valid = false;
                showError(skeletonError, Messages.format(resources, "datetime.skeleton.invalid", e.getMessage()));
            }
        }
        PRESET_SKELETONS.stream().filter(preset -> !list.contains(preset)).forEach(list::add);
        skeletons.setRows(DateTimeFormats.skeletonRows(dateTime(), list));
        return valid;
    }

    // --- Pattern -----------------------------------------------------------------------------

    private void initPatterns() {
        patternBox.getStyleClass().add("mono");
        patternBox.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        patternSuggestions = SuggestionBox.install(patternBox, context, "datetime.pattern",
                PATTERN_SUGGESTIONS, value -> {
                    pattern = value;
                    if (updatePatterns()) {
                        patternSuggestions.remember(pattern);
                        updateParsers();
                    }
                });
        patternSuggestions.show(pattern);
    }

    private boolean updatePatterns() {
        try {
            DateTimeFormatter.ofPattern(pattern);
            showError(patternError, null);
            patterns.setRows(DateTimeFormats.patternRows(dateTime(), pattern));
            return true;
        } catch (IllegalArgumentException e) {
            showError(patternError, Messages.format(resources, "datetime.pattern.invalid", e.getMessage()));
            patterns.setRows(DateTimeFormats.patternRows(dateTime(), null));
            return false;
        }
    }

    // --- Names -------------------------------------------------------------------------------

    private void initNames() {
        namesLocaleBox.setConverter(readOnly(Locale::toLanguageTag));
        namesLocaleBox.setOnAction(_ -> namesTable.refresh());
        namesKindBox.getItems().setAll(DateTimeNames.Kind.values());
        namesKindBox.setConverter(readOnly(kind -> resources.getString(switch (kind) {
            case MONTHS -> "datetime.names.months";
            case DAYS_OF_WEEK -> "datetime.names.daysOfWeek";
            case ERAS -> "datetime.names.eras";
            case AM_PM -> "datetime.names.amPm";
            case QUARTERS -> "datetime.names.quarters";
        })));
        // a value listener, not onAction: the action event needs the skin, which does not exist yet
        namesKindBox.valueProperty().subscribe(kind -> {
            if (kind != null) {
                namesTable.getItems().setAll(kind.samples());
            }
        });

        var numberColumn = new TableColumn<TemporalAccessor, String>(resources.getString("datetime.names.number"));
        numberColumn.setSortable(false);
        numberColumn.setPrefWidth(60);
        numberColumn.setCellValueFactory(features -> new ReadOnlyStringWrapper(
                String.valueOf(namesKindBox.getValue().number(features.getValue()))));
        var columns = new ArrayList<TableColumn<TemporalAccessor, ?>>();
        columns.add(numberColumn);
        for (var style : DateTimeNames.STYLES) {
            columns.add(nameColumn(style));
        }
        namesTable.getColumns().setAll(columns);
        namesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        namesTable.getSelectionModel().setCellSelectionEnabled(true);
        namesTable.getFocusModel().focusedCellProperty().subscribe(position -> {
            if (namesTable.isFocused() && position != null && position.getTableColumn() != null
                    && position.getTableColumn().getUserData() instanceof TextStyle style
                    && position.getRow() >= 0 && position.getRow() < namesTable.getItems().size()) {
                var sample = namesTable.getItems().get(position.getRow());
                context.codeSink().accept(namesKindBox.getValue().code(sample, style, namesLocaleBox.getValue()));
            }
        });

        namesKindBox.setValue(DateTimeNames.Kind.MONTHS);
    }

    private TableColumn<TemporalAccessor, String> nameColumn(TextStyle style) {
        var column = new TableColumn<TemporalAccessor, String>(style.name());
        column.setUserData(style);
        column.setSortable(false);
        column.setPrefWidth(130);
        column.setCellValueFactory(features -> new ReadOnlyStringWrapper(name(features.getValue(), style)));
        column.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                var locale = namesLocaleBox.getValue();
                setText(empty ? null : item);
                Controls.applyScriptFont(this, empty ? null : item);
                setNodeOrientation(locale != null && TextDirection.isRightToLeft(locale)
                        ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
                var differs = false;
                if (!empty && style.isStandalone() && getTableRow() != null && getTableRow().getItem() != null) {
                    differs = !item.equals(name(getTableRow().getItem(), style.asNormal()));
                }
                pseudoClassStateChanged(DIFFERS, differs);
            }
        });
        return column;
    }

    private String name(TemporalAccessor sample, TextStyle style) {
        var locale = namesLocaleBox.getValue();
        if (locale == null) {
            return "";
        }
        try {
            return namesKindBox.getValue().name(sample, style, locale);
        } catch (RuntimeException e) {
            return "⚠ " + e.getMessage();
        }
    }

    // --- Parsing -----------------------------------------------------------------------------

    private void initParsing() {
        parseInputBox.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        parseSuggestions = SuggestionBox.install(parseInputBox, context, "datetime.parse",
                PARSE_SUGGESTIONS, value -> {
                    updateParsing();
                    parseSuggestions.remember(value);
                });
        parseSuggestions.show("24.11.26");

        parserBox.setConverter(readOnly(parser -> parser.literal()
                ? parser.labelKey()
                : Messages.format(resources, parser.labelKey(), parser.labelArgs().toArray())));
        parserBox.setOnAction(_ -> updateParsing());
        updateParsers();

        resolverBox.getItems().setAll(ResolverStyle.values());
        resolverBox.setValue(ResolverStyle.SMART);
        resolverBox.setOnAction(_ -> updateParsing());
    }

    private void updateParsers() {
        var selectedIndex = Math.max(0, parserBox.getSelectionModel().getSelectedIndex());
        var parsers = new ArrayList<>(DateTimeParsing.Parser.standard());
        parsers.add(DateTimeParsing.Parser.ofPattern(pattern));
        parserBox.getItems().setAll(parsers);
        parserBox.getSelectionModel().select(Math.min(selectedIndex, parsers.size() - 1));
    }

    private void updateParsing() {
        if (parserBox.getValue() == null || resolverBox.getValue() == null) {
            return;
        }
        var text = parseInputBox.getValue() == null ? "" : parseInputBox.getValue();
        parsing.setRows(DateTimeParsing.rows(text, parserBox.getValue(), resolverBox.getValue(), dateTime()));
    }

    // --- Helpers -----------------------------------------------------------------------------
}
