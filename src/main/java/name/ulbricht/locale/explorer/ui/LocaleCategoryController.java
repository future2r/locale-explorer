package name.ulbricht.locale.explorer.ui;

import static name.ulbricht.locale.explorer.ui.Controls.readOnly;
import static name.ulbricht.locale.explorer.ui.Controls.showError;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.IllformedLocaleException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Locale.FilteringMode;
import java.util.Locale.LanguageRange;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.Subscription;

import name.ulbricht.locale.explorer.i18n.Messages;
import name.ulbricht.locale.explorer.i18n.UiLanguage;
import name.ulbricht.locale.explorer.locale.LocaleDetails;
import name.ulbricht.locale.explorer.locale.LocaleExtensions;
import name.ulbricht.locale.explorer.locale.LocaleMatching;
import name.ulbricht.locale.explorer.model.AppState;
import name.ulbricht.locale.explorer.text.JavaCode;
import name.ulbricht.locale.explorer.text.LocaleProperty;
import name.ulbricht.locale.explorer.text.Locales;
import name.ulbricht.locale.explorer.text.TextDirection;

/// Controller of the "Locale" category with the tabs details, extensions, matching and all
/// locales.
public final class LocaleCategoryController implements CategoryController {

    private static final int EXTENSION_COLUMNS = 4;
    private static final int MAX_FILTER_RESULTS_SHOWN = 30;
    private static final String PRIORITY_LIST_FIELD = "locale.priorityList";
    private static final String DEFAULT_PRIORITY_LIST = "fr-CH, fr;q=0.9, en;q=0.8, de;q=0.7";
    private static final String DEFAULT_CUSTOM_CANDIDATES = "de-DE, de-AT, de-CH, fr-FR, fr-CH, en-US, en-GB";

    private static final List<Suggestion> PRIORITY_LIST_SUGGESTIONS = List.of(
            new Suggestion("de-CH, de;q=0.9, en;q=0.8", "suggest.match.swiss"),
            new Suggestion(DEFAULT_PRIORITY_LIST, "suggest.match.fr"),
            new Suggestion("*", "suggest.match.any"),
            new Suggestion("zh-Hant-TW, zh;q=0.8, en;q=0.5", "suggest.match.zh"),
            new Suggestion("en;q=0.1, ja;q=0.9", "suggest.match.weights"),
            new Suggestion("iw, ji, in", "suggest.match.legacy"),
            new Suggestion("de-*-DE", "suggest.match.extended"));

    /// The sets of candidate locales for matching.
    private enum Candidates {
        ALL, UI_LANGUAGES, CUSTOM
    }

    private final CategoryContext context;
    private final AppState state;
    private final ResourceBundle resources;
    private final Locale uiLocale;
    private final List<Subscription> subscriptions = new ArrayList<>();
    private final Map<String, ComboBox<String>> extensionBoxes = new LinkedHashMap<>();

    private LocaleTable details;
    private LocaleTable preview;
    private Locale extended;
    private SuggestionBox prioritySuggestions;
    private FilterTable<Locale> all;
    private boolean resettingExtensions;

    @FXML
    private TableView<LocaleProperty> detailsTable;

    @FXML
    private Label extBaseLabel;
    @FXML
    private GridPane extGrid;
    @FXML
    private Label extResultLabel;
    @FXML
    private Label extErrorLabel;
    @FXML
    private TableView<LocaleProperty> extPreviewTable;

    @FXML
    private ComboBox<String> priorityBox;
    @FXML
    private ComboBox<FilteringMode> modeBox;
    @FXML
    private ComboBox<Candidates> candidatesBox;
    @FXML
    private Label customCandidatesLabel;
    @FXML
    private TextField customCandidatesField;
    @FXML
    private Label matchErrorLabel;
    @FXML
    private TableView<LanguageRange> rangesTable;
    @FXML
    private TableColumn<LanguageRange, String> rangeColumn;
    @FXML
    private TableColumn<LanguageRange, String> weightColumn;
    @FXML
    private Label lookupLabel;
    @FXML
    private Label filterLabel;

    @FXML
    private TextField filterField;
    @FXML
    private CheckBox withRegionCheck;
    @FXML
    private CheckBox rtlOnlyCheck;
    @FXML
    private Label countLabel;
    @FXML
    private TableView<Locale> allTable;
    @FXML
    private TableColumn<Locale, String> allTagColumn;
    @FXML
    private TableColumn<Locale, String> allNameUiColumn;
    @FXML
    private TableColumn<Locale, Locale> allNameOwnColumn;
    @FXML
    private TableColumn<Locale, String> allDirectionColumn;

    /// Creates the controller.
    ///
    /// @param context the category context
    public LocaleCategoryController(CategoryContext context) {
        this.context = context;
        this.state = context.state();
        this.resources = context.resources();
        this.uiLocale = state.getUiLanguage().locale();
    }

    @Override
    public void dispose() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
    }

    @FXML
    private void initialize() {
        initDetails();
        initExtensions();
        initMatching();
        initAllLocales();
    }

    // --- Details -----------------------------------------------------------------------------

    private void initDetails() {
        details = new LocaleTable(detailsTable, resources.getString("common.property"), context);
        details.setRows(LocaleDetails.rows(uiLocale));
        subscriptions.add(state.mainLocaleProperty().subscribe(_ -> details.setLocales(state.columnLocales())));
        subscriptions.add(state.getCompareLocales().subscribe(() -> details.setLocales(state.columnLocales())));
    }

    // --- Extensions --------------------------------------------------------------------------

    private void initExtensions() {
        preview = new LocaleTable(extPreviewTable, resources.getString("locale.ext.example"), context);
        var index = 0;
        for (var key : LocaleExtensions.KEYS) {
            var box = new ComboBox<String>();
            box.setEditable(true);
            box.setMaxWidth(Double.MAX_VALUE);
            box.setVisibleRowCount(15);
            box.getStyleClass().add("mono");
            box.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
            box.getItems().add("");
            box.getItems().addAll(LocaleExtensions.suggestedValues(key));
            ChoiceHandler.install(box, _ -> updateExtensions());
            extensionBoxes.put(key, box);

            var label = new Label(resources.getString("locale.ext." + key) + "  (" + key + ")");
            label.getStyleClass().add("field-label");
            var field = new VBox(label, box);
            field.getStyleClass().add("tool-field");
            extGrid.add(field, index % EXTENSION_COLUMNS, index / EXTENSION_COLUMNS);
            index++;
        }
        subscriptions.add(state.mainLocaleProperty().subscribe(this::resetExtensions));
        subscriptions.add(state.dateTimeProperty().subscribe((_, _) -> updatePreviewRows()));
        subscriptions.add(state.zoneProperty().subscribe((_, _) -> updatePreviewRows()));
    }

    private void resetExtensions(Locale base) {
        extBaseLabel.setText(base.toLanguageTag());
        // evaluate once at the end, not once per box
        resettingExtensions = true;
        try {
            extensionBoxes.forEach((key, box) -> {
                var type = base.getUnicodeLocaleType(key);
                box.setValue(type == null ? "" : type);
            });
        } finally {
            resettingExtensions = false;
        }
        updateExtensions();
    }

    private void updateExtensions() {
        if (resettingExtensions) {
            return;
        }
        var base = state.getMainLocale();
        var keywords = new LinkedHashMap<String, String>();
        // the value, not the editor text: the editor is updated only after the value changed
        extensionBoxes.forEach((key, box) -> keywords.put(key, box.getValue() == null ? "" : box.getValue()));
        try {
            extended = LocaleExtensions.apply(base, keywords);
            extResultLabel.setText(extended.toLanguageTag());
            showError(extErrorLabel, null);
            preview.setLocales(extended.equals(base) ? List.of(base) : List.of(base, extended));
        } catch (IllformedLocaleException e) {
            extended = null;
            extResultLabel.setText("");
            showError(extErrorLabel, Messages.format(resources, "locale.ext.invalid", e.getMessage()));
            preview.setLocales(List.of(base));
        }
        updatePreviewRows();
    }

    private void updatePreviewRows() {
        preview.setRows(LocaleExtensions.previewRows(zonedDateTime()));
    }

    private ZonedDateTime zonedDateTime() {
        return state.resolution().resolved();
    }

    @FXML
    private void onExtUseMain() {
        if (extended != null) {
            state.setMainLocale(extended);
        }
    }

    @FXML
    private void onExtAddCompare() {
        addCompare(extended);
    }

    // --- Matching ----------------------------------------------------------------------------

    private void initMatching() {
        priorityBox.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        priorityBox.getStyleClass().add("mono");
        prioritySuggestions = SuggestionBox.install(priorityBox, context, PRIORITY_LIST_FIELD,
                PRIORITY_LIST_SUGGESTIONS, _ -> {
                    var user = prioritySuggestions.isUserCommit();
                    runMatch(user, user);
                });

        modeBox.getItems().setAll(FilteringMode.values());
        modeBox.setValue(FilteringMode.AUTOSELECT_FILTERING);
        modeBox.setOnAction(_ -> runMatch(true, false));

        candidatesBox.getItems().setAll(Candidates.values());
        candidatesBox.setConverter(readOnly(candidates -> switch (candidates) {
                case ALL -> Messages.format(resources, "locale.match.candidates.all", Locales.available().size());
                case UI_LANGUAGES -> Messages.format(resources, "locale.match.candidates.ui", UiLanguage.values().length);
                case CUSTOM -> resources.getString("locale.match.candidates.custom");
        }));
        candidatesBox.setValue(Candidates.ALL);
        candidatesBox.setOnAction(_ -> {
            updateCustomCandidatesVisibility();
            runMatch(true, false);
        });

        customCandidatesField.setText(DEFAULT_CUSTOM_CANDIDATES);
        customCandidatesField.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        customCandidatesField.setOnAction(_ -> runMatch(true, false));
        updateCustomCandidatesVisibility();

        rangeColumn.setCellValueFactory(features -> new ReadOnlyStringWrapper(features.getValue().getRange()));
        weightColumn.setCellValueFactory(features -> new ReadOnlyStringWrapper(
                String.valueOf(features.getValue().getWeight())));
        rangesTable.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);

        prioritySuggestions.setValue(DEFAULT_PRIORITY_LIST);
    }

    private void updateCustomCandidatesVisibility() {
        var custom = candidatesBox.getValue() == Candidates.CUSTOM;
        for (var node : List.of(customCandidatesLabel, customCandidatesField)) {
            node.setVisible(custom);
            node.setManaged(custom);
        }
    }

    private Collection<Locale> candidates() {
        return switch (candidatesBox.getValue()) {
            case ALL -> Locales.available();
            case UI_LANGUAGES -> Arrays.stream(UiLanguage.values()).map(UiLanguage::locale).toList();
            case CUSTOM -> Locales.parseTags(customCandidatesField.getText());
        };
    }

    /// Matches the priority list against the candidates.
    ///
    /// @param showCode whether to show the matching code (the user changed something)
    /// @param remember whether to add the priority list to its history (the user entered it)
    private void runMatch(boolean showCode, boolean remember) {
        // The value is updated on Enter and on selection; the editor text is only a fallback.
        var text = priorityBox.getValue() != null ? priorityBox.getValue() : priorityBox.getEditor().getText();
        var mode = modeBox.getValue();
        try {
            var result = LocaleMatching.match(text, candidates(), mode);
            rangesTable.getItems().setAll(result.ranges());
            lookupLabel.setText(result.lookup().map(Locale::toLanguageTag)
                    .orElseGet(() -> resources.getString("locale.match.noMatch")));
            var tags = result.filtered().stream()
                    .limit(MAX_FILTER_RESULTS_SHOWN)
                    .map(Locale::toLanguageTag)
                    .collect(Collectors.joining(", "));
            var more = result.filtered().size() > MAX_FILTER_RESULTS_SHOWN ? ", …" : "";
            filterLabel.setText(result.filtered().isEmpty()
                    ? resources.getString("locale.match.noMatch")
                    : tags + more + "  (" + Messages.format(resources, "locale.match.matches", result.filtered().size()) + ")");
            showError(matchErrorLabel, null);
            if (remember) {
                prioritySuggestions.remember(text);
            }
        } catch (IllegalArgumentException e) {
            rangesTable.getItems().clear();
            lookupLabel.setText("");
            filterLabel.setText("");
            showError(matchErrorLabel, Messages.format(resources, "locale.match.invalid", e.getMessage()));
        }
        if (showCode) {
            context.codeSink().accept(matchingCode(text, mode));
        }
    }

    private String matchingCode(String priorityList, FilteringMode mode) {
        var candidates = switch (candidatesBox.getValue()) {
            case ALL -> "Arrays.asList(Locale.getAvailableLocales())";
            case UI_LANGUAGES -> Arrays.stream(UiLanguage.values())
                    .map(language -> JavaCode.locale(language.locale()))
                    .collect(Collectors.joining(", ", "List.of(", ")"));
            case CUSTOM -> Locales.parseTags(customCandidatesField.getText()).stream()
                    .map(JavaCode::locale)
                    .collect(Collectors.joining(", ", "List.of(", ")"));
        };
        return String.join("\n",
                "var ranges = Locale.LanguageRange.parse(" + JavaCode.string(priorityList) + ");",
                "var candidates = " + candidates + ";",
                "Locale.lookup(ranges, candidates);",
                "Locale.filter(ranges, candidates, Locale.FilteringMode." + mode.name() + ");");
    }

    // --- All locales -------------------------------------------------------------------------

    private void initAllLocales() {
        all = new FilterTable<>(allTable, Locales.available(), filterField, countLabel, context);

        allTagColumn.setCellValueFactory(features -> new ReadOnlyStringWrapper(features.getValue().toLanguageTag()));
        allNameUiColumn.setCellValueFactory(features -> new ReadOnlyStringWrapper(features.getValue().getDisplayName(uiLocale)));
        allNameOwnColumn.setCellValueFactory(features -> new ReadOnlyObjectWrapper<>(features.getValue()));
        allNameOwnColumn.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(Locale locale, boolean empty) {
                super.updateItem(locale, empty);
                if (empty || locale == null) {
                    setText(null);
                    Controls.applyScriptFont(this, null);
                    return;
                }
                setNodeOrientation(TextDirection.isRightToLeft(locale)
                        ? NodeOrientation.RIGHT_TO_LEFT
                        : NodeOrientation.LEFT_TO_RIGHT);
                setText(locale.getDisplayName(locale));
                Controls.applyScriptFont(this, getText());
            }
        });
        allNameOwnColumn.setComparator((a, b) -> a.getDisplayName(a).compareTo(b.getDisplayName(b)));
        allDirectionColumn.setCellValueFactory(features -> new ReadOnlyStringWrapper(
                TextDirection.isRightToLeft(features.getValue()) ? "RTL" : "LTR"));
        allTagColumn.setStyle("-fx-alignment: center-left;");

        all.onDoubleClick(state::setMainLocale);
        allTable.getSelectionModel().selectedItemProperty().subscribe(locale -> {
            if (locale != null) {
                context.codeSink().accept(JavaCode.withLocale(locale,
                        "locale.getDisplayName(" + JavaCode.locale(uiLocale) + ");",
                        "locale.getDisplayName(locale);"));
            }
        });

        filterField.textProperty().subscribe((_, _) -> updateFilter());
        withRegionCheck.selectedProperty().subscribe((_, _) -> updateFilter());
        rtlOnlyCheck.selectedProperty().subscribe((_, _) -> updateFilter());
        updateFilter();
    }

    private void updateFilter() {
        var query = all.query();
        Predicate<Locale> predicate = locale -> query.isEmpty()
                || locale.toLanguageTag().toLowerCase(Locale.ROOT).contains(query)
                || locale.getDisplayName(uiLocale).toLowerCase(uiLocale).contains(query)
                || locale.getDisplayName(locale).toLowerCase(locale).contains(query);
        if (withRegionCheck.isSelected()) {
            predicate = predicate.and(locale -> !locale.getCountry().isEmpty());
        }
        if (rtlOnlyCheck.isSelected()) {
            predicate = predicate.and(TextDirection::isRightToLeft);
        }
        all.filter(predicate);
    }

    @FXML
    private void onAllUseMain() {
        var locale = allTable.getSelectionModel().getSelectedItem();
        if (locale != null) {
            state.setMainLocale(locale);
        }
    }

    @FXML
    private void onAllAddCompare() {
        addCompare(allTable.getSelectionModel().getSelectedItem());
    }

    // --- Helpers -----------------------------------------------------------------------------

    private void addCompare(Locale locale) {
        if (locale != null && !locale.equals(state.getMainLocale()) && !state.getCompareLocales().contains(locale)) {
            state.getCompareLocales().add(locale);
        }
    }
}
