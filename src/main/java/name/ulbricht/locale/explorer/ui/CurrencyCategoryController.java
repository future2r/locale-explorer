package name.ulbricht.locale.explorer.ui;

import static name.ulbricht.locale.explorer.ui.Controls.showError;
import static name.ulbricht.locale.explorer.ui.Controls.readOnly;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Predicate;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.Subscription;

import name.ulbricht.locale.explorer.currency.Currencies;
import name.ulbricht.locale.explorer.i18n.Messages;
import name.ulbricht.locale.explorer.model.AppState;
import name.ulbricht.locale.explorer.number.NumberOptions;
import name.ulbricht.locale.explorer.text.LocaleProperty;

/// Controller of the "Currency" category.
public final class CurrencyCategoryController implements CategoryController {

    private static final String DEFAULT_AMOUNT = "-1234.567";
    private static final String FROM_LOCALE = "";
    private static final int UNCHANGED_DIGITS = -2;
    private static final int ALL_DIGITS = -99;

    private static final List<Suggestion> AMOUNT_SUGGESTIONS = List.of(
            new Suggestion(DEFAULT_AMOUNT, "suggest.currency.default"),
            new Suggestion("0.5", "suggest.currency.half"),
            new Suggestion("1234567.891", "suggest.currency.large"),
            new Suggestion("0.005", "suggest.currency.small"));

    private enum SymbolLocales {
        COLUMNS, ENGLISH
    }

    private final CategoryContext context;
    private final AppState state;
    private final ResourceBundle resources;
    private final Locale uiLocale;
    private final List<Subscription> subscriptions = new ArrayList<>();
    private final List<LocaleTable> tables = new ArrayList<>();
    private final List<Currency> currencies = Currencies.all();

    private LocaleTable formats;
    private LocaleTable localeCurrency;
    private LocaleTable symbols;
    private SuggestionBox amountSuggestions;
    private BigDecimal amount = new BigDecimal(DEFAULT_AMOUNT);
    private FilterTable<Currency> all;

    @FXML
    private CheckBox visualizeCheck;
    @FXML
    private ComboBox<String> amountBox;
    @FXML
    private ComboBox<String> currencyBox;
    @FXML
    private ComboBox<Integer> digitsBox;
    @FXML
    private Label amountError;
    @FXML
    private TableView<LocaleProperty> formatsTable;
    @FXML
    private TableView<LocaleProperty> localeTable;
    @FXML
    private ComboBox<SymbolLocales> symbolLocalesBox;
    @FXML
    private TableView<LocaleProperty> symbolsTable;
    @FXML
    private TextField filterField;
    @FXML
    private ComboBox<Integer> digitsFilterBox;
    @FXML
    private Label countLabel;
    @FXML
    private TableView<Currency> allTable;
    @FXML
    private TableColumn<Currency, String> allCodeColumn;
    @FXML
    private TableColumn<Currency, String> allNumericColumn;
    @FXML
    private TableColumn<Currency, String> allDigitsColumn;
    @FXML
    private TableColumn<Currency, String> allNameColumn;
    @FXML
    private TableColumn<Currency, String> allSymbolColumn;

    /// Creates the controller.
    ///
    /// @param context the category context
    public CurrencyCategoryController(CategoryContext context) {
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
        formats = table(formatsTable, resources.getString("number.format"));
        localeCurrency = table(localeTable, resources.getString("common.property"));
        symbols = table(symbolsTable, resources.getString("currency.currency"));
        visualizeCheck.selectedProperty().subscribe(selected -> tables.forEach(t -> t.setVisualizeSpecialCharacters(selected)));

        initFormats();
        localeCurrency.setRows(Currencies.localeCurrencyRows(uiLocale));
        initSymbols();
        initAll();

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
        formats.setLocales(locales);
        localeCurrency.setLocales(locales);
        updateSymbolLocales();
        allTable.refresh();
    }

    // --- Formats -----------------------------------------------------------------------------

    private void initFormats() {
        amountBox.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        amountBox.getStyleClass().add("mono");
        amountSuggestions = SuggestionBox.install(amountBox, context, "currency.amount", AMOUNT_SUGGESTIONS, text -> {
            try {
                amount = NumberOptions.parseDecimal(text);
                showError(amountError, null);
                amountSuggestions.remember(text.strip());
                updateFormats();
            } catch (NumberFormatException _) {
                showError(amountError, Messages.format(resources, "common.number.invalid", text));
            }
        });
        amountSuggestions.setValue(DEFAULT_AMOUNT);

        var codes = new ArrayList<String>();
        codes.add(FROM_LOCALE);
        currencies.forEach(currency -> codes.add(currency.getCurrencyCode()));
        currencyBox.getItems().setAll(codes);
        currencyBox.setConverter(readOnly(code -> code.isEmpty()
                ? resources.getString("currency.fromLocale")
                : code + " · " + Currency.getInstance(code).getDisplayName(uiLocale)));
        currencyBox.setValue(FROM_LOCALE);
        currencyBox.setOnAction(_ -> updateFormats());

        digitsBox.getItems().setAll(UNCHANGED_DIGITS, Currencies.CURRENCY_DIGITS, 0, 1, 2, 3, 4);
        digitsBox.setConverter(readOnly(digits -> switch (digits) {
            case UNCHANGED_DIGITS -> resources.getString("currency.digits.unchanged");
            case Currencies.CURRENCY_DIGITS -> resources.getString("currency.digits.currency");
            default -> String.valueOf(digits);
        }));
        digitsBox.setValue(UNCHANGED_DIGITS);
        digitsBox.setOnAction(_ -> updateFormats());
        updateFormats();
    }

    private void updateFormats() {
        if (digitsBox.getValue() == null || currencyBox.getValue() == null) {
            return;
        }
        var code = currencyBox.getValue();
        var currency = code.isEmpty() ? null : Currency.getInstance(code);
        var digits = digitsBox.getValue() == UNCHANGED_DIGITS ? null : digitsBox.getValue();
        formats.setRows(Currencies.formatRows(amount, currency, digits));
    }

    // --- Symbols -----------------------------------------------------------------------------

    private void initSymbols() {
        symbolLocalesBox.getItems().setAll(SymbolLocales.values());
        symbolLocalesBox.setConverter(readOnly(choice -> resources.getString(switch (choice) {
            case COLUMNS -> "common.locales.columns";
            case ENGLISH -> "currency.symbols.english";
        })));
        symbolLocalesBox.setValue(SymbolLocales.COLUMNS);
        symbolLocalesBox.setOnAction(_ -> updateSymbolLocales());
        symbols.setRows(Currencies.symbolRows(Currencies.SYMBOL_CURRENCIES));
    }

    private void updateSymbolLocales() {
        symbols.setLocales(symbolLocalesBox.getValue() == SymbolLocales.ENGLISH
                ? Currencies.ENGLISH_VARIANTS
                : state.columnLocales());
    }

    // --- All currencies ----------------------------------------------------------------------

    private void initAll() {
        all = new FilterTable<>(allTable, currencies, filterField, countLabel, context);
        allCodeColumn.setCellValueFactory(features -> new ReadOnlyStringWrapper(features.getValue().getCurrencyCode()));
        allNumericColumn.setCellValueFactory(features -> new ReadOnlyStringWrapper(features.getValue().getNumericCodeAsString()));
        allDigitsColumn.setCellValueFactory(features -> new ReadOnlyStringWrapper(
                String.valueOf(features.getValue().getDefaultFractionDigits())));
        allNameColumn.setCellValueFactory(features -> new ReadOnlyStringWrapper(features.getValue().getDisplayName(uiLocale)));
        allSymbolColumn.setCellValueFactory(features -> new ReadOnlyStringWrapper(
                features.getValue().getSymbol(state.getMainLocale())));
        all.onDoubleClick(this::useCurrency);

        digitsFilterBox.getItems().setAll(ALL_DIGITS, -1, 0, 2, 3, 4);
        digitsFilterBox.setConverter(readOnly(digits -> digits == ALL_DIGITS
                ? resources.getString("currency.all.digitsAll")
                : String.valueOf(digits)));
        digitsFilterBox.setValue(ALL_DIGITS);
        digitsFilterBox.setOnAction(_ -> updateFilter());
        filterField.textProperty().subscribe((_, _) -> updateFilter());
        updateFilter();
    }

    private void updateFilter() {
        var query = all.query();
        Predicate<Currency> predicate = currency -> query.isEmpty()
                || currency.getCurrencyCode().toLowerCase(Locale.ROOT).contains(query)
                || currency.getDisplayName(uiLocale).toLowerCase(uiLocale).contains(query);
        var digits = digitsFilterBox.getValue();
        if (digits != null && digits != ALL_DIGITS) {
            predicate = predicate.and(currency -> currency.getDefaultFractionDigits() == digits);
        }
        all.filter(predicate);
    }

    @FXML
    private void onUseCurrency() {
        var currency = allTable.getSelectionModel().getSelectedItem();
        if (currency != null) {
            useCurrency(currency);
        }
    }

    private void useCurrency(Currency currency) {
        // the currency box updates the formats itself
        currencyBox.setValue(currency.getCurrencyCode());
    }
}
