package name.ulbricht.locale.explorer.ui;

import static name.ulbricht.locale.explorer.ui.Controls.readOnly;
import static name.ulbricht.locale.explorer.ui.Controls.showError;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.Subscription;

import name.ulbricht.locale.explorer.i18n.Messages;
import name.ulbricht.locale.explorer.lists.TextFormats;
import name.ulbricht.locale.explorer.model.AppState;
import name.ulbricht.locale.explorer.text.LocaleProperty;

/// Controller of the "Lists, text" category.
public final class ListsCategoryController implements CategoryController {

    private static final String DEFAULT_ITEMS = "Äpfel | Birnen | Bananen";
    private static final String DEFAULT_MESSAGE =
            "{0} hat am {1,date,long} {2,choice,0#keine Dateien|1#eine Datei|1<{2,number,integer} Dateien} gelöscht.";
    private static final String DEFAULT_WORDS = "title | İSTANBUL | ıssız | Straße | ΟΔΟΣ | ǅemal";
    private static final String DEFAULT_FORMAT = "%1$,.2f € am %2$tA";
    private static final double FORMAT_VALUE = -1234567.891;
    private static final String FORMAT_TEXT = "title";
    private static final long FORMAT_NUMBER = 1234567;

    private static final List<Suggestion> ITEM_SUGGESTIONS = List.of(
            new Suggestion(DEFAULT_ITEMS, "suggest.list.fruit"),
            new Suggestion("Ana | Isabel", "suggest.list.spanishAnd"),
            new Suggestion("siete | ocho", "suggest.list.spanishOr"),
            new Suggestion("Hund", "suggest.list.single"),
            new Suggestion("A | B | C | D | E", "suggest.list.many"),
            new Suggestion("Rot, Grün | Blau", "suggest.list.comma"));

    private static final List<Suggestion> MESSAGE_SUGGESTIONS = List.of(
            new Suggestion(DEFAULT_MESSAGE, "suggest.message.choice"),
            new Suggestion("{0} deleted {2,choice,0#no files|1#one file|1<{2,number,integer} files} on {1,date,full}.",
                    "suggest.message.english"),
            new Suggestion("'{0}' bleibt stehen, ''{0}'' wird zu '{0}'", "suggest.message.quotes"),
            new Suggestion("{2,number,percent} · {2,number,#,##0.0} · {2,number,currency}", "suggest.message.numbers"),
            new Suggestion("{1,date,short} {1,time,short} · {1,time,full}", "suggest.message.timeZone"));

    private static final List<Suggestion> WORD_SUGGESTIONS = List.of(
            new Suggestion(DEFAULT_WORDS, "suggest.case.mixed"),
            new Suggestion("title | İstanbul | ıssız | kırmızı", "suggest.case.turkish"),
            new Suggestion("Straße | ﬂuss | ŉ", "suggest.case.expansion"),
            new Suggestion("ΟΔΟΣ | ΣΊΣΥΦΟΣ | όσος", "suggest.case.greek"),
            new Suggestion("i̇ | İ | Ĩ", "suggest.case.lithuanian"));

    private static final List<Suggestion> FORMAT_SUGGESTIONS = List.of(
            new Suggestion(DEFAULT_FORMAT, "suggest.format.mixed"),
            new Suggestion("%2$tA, %2$te. %2$tB %2$tY", "suggest.format.date"),
            new Suggestion("%4$,d Dateien", "suggest.format.grouping"),
            new Suggestion("%1$s", "suggest.format.toString"),
            new Suggestion("%3$S", "suggest.format.upper"),
            new Suggestion("%1$.0f · %1$.1f", "suggest.format.rounding"));

    private enum CaseLocales {
        FIXED, COLUMNS
    }

    private final CategoryContext context;
    private final AppState state;
    private final ResourceBundle resources;
    private final List<Subscription> subscriptions = new ArrayList<>();

    private LocaleTable lists;
    private LocaleTable messages;
    private LocaleTable cases;
    private LocaleTable formats;
    private SuggestionBox itemSuggestions;
    private SuggestionBox messageSuggestions;
    private SuggestionBox wordSuggestions;
    private SuggestionBox formatSuggestions;

    @FXML
    private ComboBox<String> itemsBox;
    @FXML
    private TableView<LocaleProperty> listTable;
    @FXML
    private ComboBox<String> messagePatternBox;
    @FXML
    private TextField nameField;
    @FXML
    private Label dateLabel;
    @FXML
    private Spinner<Integer> countSpinner;
    @FXML
    private Label messageError;
    @FXML
    private TableView<LocaleProperty> messageTable;
    @FXML
    private ComboBox<String> wordsBox;
    @FXML
    private ComboBox<CaseLocales> caseLocalesBox;
    @FXML
    private TableView<LocaleProperty> caseTable;
    @FXML
    private ComboBox<String> formatBox;
    @FXML
    private Label formatHint;
    @FXML
    private Label formatError;
    @FXML
    private TableView<LocaleProperty> formatTable;

    /// Creates the controller.
    ///
    /// @param context the category context
    public ListsCategoryController(CategoryContext context) {
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
        lists = new LocaleTable(listTable, format, context);
        messages = new LocaleTable(messageTable, format, context);
        cases = new LocaleTable(caseTable, resources.getString("case.word"), context);
        formats = new LocaleTable(formatTable, resources.getString("format.pattern"), context);

        initLists();
        initMessages();
        initCases();
        initFormats();

        subscriptions.add(state.mainLocaleProperty().subscribe((_, _) -> updateLocales()));
        subscriptions.add(state.getCompareLocales().subscribe(this::updateLocales));
        subscriptions.add(state.dateTimeProperty().subscribe((_, _) -> updateDateTimeDependent()));
        subscriptions.add(state.zoneProperty().subscribe((_, _) -> updateDateTimeDependent()));
        updateLocales();
    }

    private void updateLocales() {
        var locales = state.columnLocales();
        lists.setLocales(locales);
        messages.setLocales(locales);
        formats.setLocales(locales);
        updateCaseLocales();
    }

    private void updateDateTimeDependent() {
        updateMessages(false);
        updateFormats();
    }

    private static String text(ComboBox<String> box) {
        return box.getValue() == null ? "" : box.getValue();
    }

    // --- ListFormat --------------------------------------------------------------------------

    private void initLists() {
        itemSuggestions = SuggestionBox.install(itemsBox, context, "list.items", ITEM_SUGGESTIONS, value -> {
            updateLists();
            itemSuggestions.remember(value);
        });
        itemSuggestions.setValue(DEFAULT_ITEMS);
    }

    private void updateLists() {
        lists.setRows(TextFormats.listRows(TextFormats.items(text(itemsBox))));
    }

    // --- MessageFormat -----------------------------------------------------------------------

    private void initMessages() {
        messagePatternBox.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        messagePatternBox.getStyleClass().add("mono");
        messageSuggestions = SuggestionBox.install(messagePatternBox, context, "message.pattern", MESSAGE_SUGGESTIONS,
                _ -> updateMessages(true));
        messageSuggestions.setValue(DEFAULT_MESSAGE);
        Controls.integerSpinner(countSpinner, 0, Integer.MAX_VALUE, 1234);
        countSpinner.valueProperty().subscribe((_, _) -> updateMessages(false));
        nameField.setOnAction(_ -> updateMessages(false));
        nameField.focusedProperty().subscribe((_, focused) -> {
            if (!focused) {
                updateMessages(false);
            }
        });
        updateMessages(false);
    }

    private void updateMessages(boolean remember) {
        if (countSpinner.getValueFactory() == null) {
            return;
        }
        var dateTime = state.resolution().resolved();
        dateLabel.setText(dateTime.toInstant().toString());
        var pattern = text(messagePatternBox);
        try {
            messages.setRows(TextFormats.messageRows(pattern, nameField.getText(), dateTime, countSpinner.getValue()));
            showError(messageError, null);
            if (remember) {
                messageSuggestions.remember(pattern);
            }
        } catch (IllegalArgumentException e) {
            messages.setRows(List.of());
            showError(messageError, Messages.format(resources, "message.invalid", e.getMessage()));
        }
    }

    // --- Case mapping ------------------------------------------------------------------------

    private void initCases() {
        wordSuggestions = SuggestionBox.install(wordsBox, context, "case.words", WORD_SUGGESTIONS, value -> {
            updateCases();
            wordSuggestions.remember(value);
        });
        wordSuggestions.setValue(DEFAULT_WORDS);
        caseLocalesBox.getItems().setAll(CaseLocales.values());
        caseLocalesBox.setConverter(readOnly(value -> resources.getString(value == CaseLocales.FIXED ? "case.locales.fixed" : "common.locales.columns")));
        caseLocalesBox.setValue(CaseLocales.FIXED);
        caseLocalesBox.setOnAction(_ -> updateCaseLocales());
    }

    private void updateCases() {
        cases.setRows(TextFormats.caseRows(TextFormats.items(text(wordsBox))));
    }

    private void updateCaseLocales() {
        cases.setLocales(caseLocalesBox.getValue() == CaseLocales.COLUMNS ? state.columnLocales() : TextFormats.CASE_LOCALES);
    }

    // --- String.format -----------------------------------------------------------------------

    private void initFormats() {
        formatBox.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        formatBox.getStyleClass().add("mono");
        formatSuggestions = SuggestionBox.install(formatBox, context, "format.pattern", FORMAT_SUGGESTIONS, value -> {
            if (updateFormats()) {
                formatSuggestions.remember(value);
            }
        });
        formatSuggestions.setValue(DEFAULT_FORMAT);
    }

    /// Shows the format rows, including the own format string if there is one.
    ///
    /// @return whether the own format string is valid
    private boolean updateFormats() {
        var dateTime = state.resolution().resolved();
        formatHint.setText(Messages.format(resources, "format.hint",
                String.valueOf(FORMAT_VALUE), dateTime.toString(), FORMAT_TEXT, String.valueOf(FORMAT_NUMBER)));
        var own = text(formatBox);
        var error = own.isEmpty() ? Optional.<String>empty()
                : TextFormats.formatError(own, FORMAT_VALUE, dateTime, FORMAT_TEXT, FORMAT_NUMBER);
        showError(formatError, error.map(message -> Messages.format(resources, "format.invalid", message)).orElse(null));
        // an invalid own format string is not shown as a row; it might not even be formattable
        var patterns = new ArrayList<String>();
        if (!own.isEmpty() && error.isEmpty()) {
            patterns.add(own);
        }
        TextFormats.FORMAT_PATTERNS.stream().filter(pattern -> !patterns.contains(pattern)).forEach(patterns::add);
        formats.setRows(TextFormats.formatRows(patterns, FORMAT_VALUE, dateTime, FORMAT_TEXT, FORMAT_NUMBER));
        return error.isEmpty();
    }
}
