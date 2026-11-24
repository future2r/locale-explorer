package name.ulbricht.locale.explorer.ui;

import static name.ulbricht.locale.explorer.ui.Controls.readOnly;
import static name.ulbricht.locale.explorer.ui.Controls.showError;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableView;
import javafx.util.Subscription;

import name.ulbricht.locale.explorer.i18n.Messages;
import name.ulbricht.locale.explorer.model.AppState;
import name.ulbricht.locale.explorer.number.NumberFormats;
import name.ulbricht.locale.explorer.number.NumberOptions;
import name.ulbricht.locale.explorer.text.LocaleProperty;

/// Controller of the "Numbers" category.
public final class NumbersCategoryController implements CategoryController {

    private static final String DEFAULT_VALUE = "-1234567.891";
    private static final String DEFAULT_PATTERN = "#,##0.00;(#,##0.00)";

    private static final List<Suggestion> VALUE_SUGGESTIONS = List.of(
            new Suggestion(DEFAULT_VALUE, "suggest.number.default"),
            new Suggestion("0.5", "suggest.number.half"),
            new Suggestion("2.5", "suggest.number.halfEven"),
            new Suggestion("3.5", "suggest.number.halfEven"),
            new Suggestion("0.005", "suggest.number.small"),
            new Suggestion("1e21", "suggest.number.large"),
            new Suggestion("0.30000000000000004", "suggest.number.floatingPoint"),
            new Suggestion("NaN", "suggest.number.special"),
            new Suggestion("-Infinity", "suggest.number.special"));

    private static final List<Suggestion> PATTERN_SUGGESTIONS = List.of(
            new Suggestion(DEFAULT_PATTERN, "suggest.numberPattern.parentheses"),
            new Suggestion("#,##,##0.00", "suggest.numberPattern.indian"),
            new Suggestion("##0.##E0", "suggest.numberPattern.engineering"),
            new Suggestion("0000", "suggest.numberPattern.leadingZeros"),
            new Suggestion("#,##0 ‰", "suggest.numberPattern.perMill"),
            new Suggestion("¤¤ #,##0.00", "suggest.numberPattern.isoCurrency"),
            new Suggestion("'#'0", "suggest.numberPattern.quote"),
            new Suggestion("#,##0.0 'kg'", "suggest.numberPattern.unit"));

    private static final List<Suggestion> PARSE_SUGGESTIONS = List.of(
            new Suggestion("1.234,5", "suggest.numberParse.german"),
            new Suggestion("1,234.5", "suggest.numberParse.us"),
            new Suggestion("1 234,5", "suggest.numberParse.french"),
            new Suggestion("-12 %", "suggest.numberParse.percent"),
            new Suggestion("١٬٢٣٤٫٥", "suggest.numberParse.arabic"),
            new Suggestion("12K", "suggest.numberParse.compact"),
            new Suggestion("1E3", "suggest.numberParse.exponent"));

    private final CategoryContext context;
    private final AppState state;
    private final ResourceBundle resources;
    private final List<Subscription> subscriptions = new ArrayList<>();
    private final List<LocaleTable> tables = new ArrayList<>();

    private LocaleTable formats;
    private LocaleTable symbols;
    private LocaleTable patterns;
    private LocaleTable compact;
    private LocaleTable numbering;
    private LocaleTable parsing;
    private SuggestionBox valueSuggestions;
    private SuggestionBox patternSuggestions;
    private SuggestionBox parseSuggestions;
    private Number value = new BigDecimal(DEFAULT_VALUE);
    private String pattern = DEFAULT_PATTERN;

    @FXML
    private CheckBox visualizeCheck;
    @FXML
    private ComboBox<String> valueBox;
    @FXML
    private Spinner<Integer> minIntegerSpinner;
    @FXML
    private Spinner<Integer> minFractionSpinner;
    @FXML
    private Spinner<Integer> maxFractionSpinner;
    @FXML
    private ComboBox<RoundingMode> roundingBox;
    @FXML
    private CheckBox groupingCheck;
    @FXML
    private Label valueError;
    @FXML
    private TableView<LocaleProperty> formatsTable;
    @FXML
    private TableView<LocaleProperty> symbolsTable;
    @FXML
    private ComboBox<String> patternBox;
    @FXML
    private CheckBox localizedCheck;
    @FXML
    private Label patternError;
    @FXML
    private TableView<LocaleProperty> patternsTable;
    @FXML
    private Spinner<Integer> compactFractionSpinner;
    @FXML
    private TableView<LocaleProperty> compactTable;
    @FXML
    private TableView<LocaleProperty> numberingTable;
    @FXML
    private ComboBox<String> parseInputBox;
    @FXML
    private ComboBox<NumberFormats.Kind> parseKindBox;
    @FXML
    private CheckBox strictCheck;
    @FXML
    private CheckBox integerOnlyCheck;
    @FXML
    private TableView<LocaleProperty> parseTable;

    /// Creates the controller.
    ///
    /// @param context the category context
    public NumbersCategoryController(CategoryContext context) {
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
        var format = resources.getString("number.format");
        formats = table(formatsTable, format);
        symbols = table(symbolsTable, resources.getString("number.symbol"));
        patterns = table(patternsTable, resources.getString("number.pattern"));
        compact = table(compactTable, resources.getString("number.value"));
        numbering = table(numberingTable, resources.getString("number.numbering"));
        parsing = table(parseTable, format);

        visualizeCheck.selectedProperty().subscribe(selected -> tables.forEach(t -> t.setVisualizeSpecialCharacters(selected)));

        initFormats();
        symbols.setRows(NumberFormats.symbolRows());
        initPatterns();
        initCompact();
        initParsing();

        subscriptions.add(state.mainLocaleProperty().subscribe((_, _) -> updateLocales()));
        subscriptions.add(state.getCompareLocales().subscribe(this::updateLocales));
        updateLocales();
    }

    private LocaleTable table(TableView<LocaleProperty> view, String header) {
        var table = new LocaleTable(view, header, context);
        tables.add(table);
        return table;
    }

    private void updateLocales() {
        var locales = state.columnLocales();
        tables.forEach(table -> table.setLocales(locales));
    }

    // --- Formats -----------------------------------------------------------------------------

    private void initFormats() {
        valueBox.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        valueBox.getStyleClass().add("mono");
        valueSuggestions = SuggestionBox.install(valueBox, context, "number.value", VALUE_SUGGESTIONS, text -> {
            try {
                value = NumberOptions.parseValue(text);
                showError(valueError, null);
                valueSuggestions.remember(text.strip());
                updateValueDependent();
            } catch (NumberFormatException _) {
                showError(valueError, Messages.format(resources, "common.number.invalid", text));
            }
        });
        valueSuggestions.setValue(DEFAULT_VALUE);

        var defaults = NumberOptions.DEFAULT;
        Controls.integerSpinner(minIntegerSpinner, 1, 20, defaults.minimumIntegerDigits());
        Controls.integerSpinner(minFractionSpinner, 0, 20, defaults.minimumFractionDigits());
        Controls.integerSpinner(maxFractionSpinner, 0, 20, defaults.maximumFractionDigits());
        roundingBox.getItems().setAll(RoundingMode.values());
        roundingBox.setValue(defaults.roundingMode());
        for (var spinner : List.of(minIntegerSpinner, minFractionSpinner, maxFractionSpinner)) {
            spinner.setEditable(true);
            spinner.valueProperty().subscribe((_, _) -> updateFormats());
        }
        roundingBox.setOnAction(_ -> updateFormats());
        groupingCheck.setOnAction(_ -> updateFormats());
        updateValueDependent();
    }

    private NumberOptions options() {
        return new NumberOptions(minIntegerSpinner.getValue(), minFractionSpinner.getValue(),
                maxFractionSpinner.getValue(), roundingBox.getValue(), groupingCheck.isSelected());
    }

    private void updateFormats() {
        formats.setRows(NumberFormats.formatRows(value, options()));
    }

    private void updateValueDependent() {
        if (roundingBox.getValue() == null) {
            return;
        }
        updateFormats();
        updatePatterns();
        numbering.setRows(NumberFormats.numberingRows(value));
    }

    // --- Patterns ----------------------------------------------------------------------------

    private void initPatterns() {
        patternBox.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        patternBox.getStyleClass().add("mono");
        patternSuggestions = SuggestionBox.install(patternBox, context, "number.pattern", PATTERN_SUGGESTIONS, text -> {
            pattern = text;
            if (updatePatterns()) {
                patternSuggestions.remember(text);
            }
        });
        localizedCheck.setOnAction(_ -> updatePatterns());
        // the patterns were already evaluated by initFormats via updateValueDependent
        patternSuggestions.show(DEFAULT_PATTERN);
    }

    private boolean updatePatterns() {
        try {
            patterns.setRows(NumberFormats.patternRows(value, pattern, localizedCheck.isSelected()));
            showError(patternError, null);
            return true;
        } catch (IllegalArgumentException e) {
            patterns.setRows(NumberFormats.patternRows(value, null, false));
            showError(patternError, Messages.format(resources, "number.pattern.invalid", e.getMessage()));
            return false;
        }
    }

    // --- Compact -----------------------------------------------------------------------------

    private void initCompact() {
        compactFractionSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 5, 1));
        compactFractionSpinner.valueProperty().subscribe(digits -> compact.setRows(NumberFormats.compactRows(digits)));
    }

    // --- Parsing -----------------------------------------------------------------------------

    private void initParsing() {
        parseKindBox.getItems().setAll(NumberFormats.Kind.values());
        parseKindBox.setConverter(readOnly(kind -> resources.getString("number.kind." + kind.name())));
        parseKindBox.setValue(NumberFormats.Kind.NUMBER);
        parseKindBox.setOnAction(_ -> updateParsing());
        strictCheck.setOnAction(_ -> updateParsing());
        integerOnlyCheck.setOnAction(_ -> updateParsing());

        parseInputBox.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        parseSuggestions = SuggestionBox.install(parseInputBox, context, "number.parse", PARSE_SUGGESTIONS, text -> {
            updateParsing();
            parseSuggestions.remember(text);
        });
        parseSuggestions.setValue("1.234,5");
    }

    private void updateParsing() {
        if (parseKindBox.getValue() == null) {
            return;
        }
        var text = parseInputBox.getValue() == null ? "" : parseInputBox.getValue();
        parsing.setRows(NumberFormats.parseRows(text, parseKindBox.getValue(), strictCheck.isSelected(),
                integerOnlyCheck.isSelected()));
    }

    // --- Helpers -----------------------------------------------------------------------------
}
