package name.ulbricht.locale.explorer.ui;

import static name.ulbricht.locale.explorer.ui.Controls.readOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.Subscription;

import name.ulbricht.locale.explorer.lists.TextFormats;
import name.ulbricht.locale.explorer.model.AppState;
import name.ulbricht.locale.explorer.sorting.TextCollation;
import name.ulbricht.locale.explorer.text.Fact;
import name.ulbricht.locale.explorer.text.LocaleProperty;

/// Controller of the "Sorting" category.
public final class SortingCategoryController implements CategoryController {

    private static final String DEFAULT_WORDS =
            "Zebra | Äpfel | apfel | Apfel | Ahorn | Österreich | Ozean | résumé | resume | Resume | Ñandu | Nadel | chata | cena | ıssız | Istanbul";
    private static final String DEFAULT_BOUNDARY_TEXT = "Dr. Müller kam um 9.30 Uhr. Er brachte éin Bild 👨‍👩‍👧 mit 🇩🇪-Fahne! Schön?";
    private static final String DEFAULT_NORMALIZATION_TEXT = "é · é · ﬁ · Å · Å · ① · ｶ · ㎏";

    private static final List<Locale> BOUNDARY_LOCALES = Stream.of("de-DE", "en-US", "th-TH", "ja-JP")
            .map(Locale::forLanguageTag).toList();

    private static final List<Suggestion> WORD_SUGGESTIONS = List.of(
            new Suggestion(DEFAULT_WORDS, "suggest.sort.mixed"),
            new Suggestion("Müller | Mueller | Muller | Mülheim | Mulde", "suggest.sort.umlauts"),
            new Suggestion("ångström | zebra | ärlig | öde | apa", "suggest.sort.swedish"),
            new Suggestion("chata | cena | hrad | dům | zámek", "suggest.sort.czech"),
            new Suggestion("côte | cote | coté | côté", "suggest.sort.french"),
            new Suggestion("item10 | item2 | item1 | Item3", "suggest.sort.numbers"));

    private static final List<Suggestion> BOUNDARY_SUGGESTIONS = List.of(
            new Suggestion(DEFAULT_BOUNDARY_TEXT, "suggest.boundary.mixed"),
            new Suggestion("ภาษาไทยไม่มีช่องว่างระหว่างคำ ประโยคที่สองครับ", "suggest.boundary.thai"),
            new Suggestion("日本語の文章です。二つ目の文。", "suggest.boundary.japanese"),
            new Suggestion("z.B. bzw. ca. 3 km. Ende.", "suggest.boundary.abbreviations"),
            new Suggestion("नमस्ते दुनिया", "suggest.boundary.devanagari"));

    private static final List<Suggestion> NORMALIZATION_SUGGESTIONS = List.of(
            new Suggestion(DEFAULT_NORMALIZATION_TEXT, "suggest.normalization.mixed"),
            new Suggestion("Å · Å · Å", "suggest.normalization.angstrom"),
            new Suggestion("ﬀ ﬁ ﬂ ﬃ ﬄ", "suggest.normalization.ligatures"),
            new Suggestion("한국어", "suggest.normalization.hangul"),
            new Suggestion("x² · ½ · Ⅻ", "suggest.normalization.compatibility"));

    private enum LocaleChoice {
        FIXED, COLUMNS
    }

    private final CategoryContext context;
    private final AppState state;
    private final ResourceBundle resources;
    private final List<Subscription> subscriptions = new ArrayList<>();

    private LocaleTable sorted;
    private LocaleTable compared;
    private FactTable plain;
    private LocaleTable boundaries;
    private FactTable normalization;
    private SuggestionBox wordSuggestions;
    private SuggestionBox boundarySuggestions;
    private SuggestionBox normalizationSuggestions;

    @FXML
    private ComboBox<String> wordsBox;
    @FXML
    private ComboBox<TextCollation.Strength> strengthBox;
    @FXML
    private ComboBox<TextCollation.Decomposition> decompositionBox;
    @FXML
    private ComboBox<LocaleChoice> sortLocalesBox;
    @FXML
    private TableView<LocaleProperty> sortTable;
    @FXML
    private TextField firstField;
    @FXML
    private TextField secondField;
    @FXML
    private TableView<LocaleProperty> compareTable;
    @FXML
    private TableView<Fact> plainTable;
    @FXML
    private ComboBox<String> boundaryTextBox;
    @FXML
    private ComboBox<LocaleChoice> boundaryLocalesBox;
    @FXML
    private TableView<LocaleProperty> boundaryTable;
    @FXML
    private ComboBox<String> normalizationBox;
    @FXML
    private TableView<Fact> normalizationTable;

    /// Creates the controller.
    ///
    /// @param context the category context
    public SortingCategoryController(CategoryContext context) {
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
        sorted = new LocaleTable(sortTable, resources.getString("sort.codePointOrder"), context);
        compared = new LocaleTable(compareTable, resources.getString("sort.strength"), context);
        plain = new FactTable(plainTable, resources.getString("common.method"), resources.getString("common.value"), context);
        boundaries = new LocaleTable(boundaryTable, resources.getString("sort.boundary"), context);
        normalization = new FactTable(normalizationTable, resources.getString("sort.form"),
                resources.getString("common.value"), context);

        initSorting();
        initComparing();
        initBoundaries();
        initNormalization();

        subscriptions.add(state.mainLocaleProperty().subscribe((_, _) -> updateLocales()));
        subscriptions.add(state.getCompareLocales().subscribe(this::updateLocales));
        updateLocales();
    }

    private void updateLocales() {
        compared.setLocales(state.columnLocales());
        boundaries.setLocales(boundaryLocalesBox.getValue() == LocaleChoice.COLUMNS ? state.columnLocales() : BOUNDARY_LOCALES);
        updateSorting();
    }

    private static String text(ComboBox<String> box) {
        return box.getValue() == null ? "" : box.getValue();
    }

    // --- Sorting -----------------------------------------------------------------------------

    private void initSorting() {
        strengthBox.getItems().setAll(TextCollation.STRENGTHS);
        strengthBox.setConverter(readOnly(TextCollation.Strength::name));
        strengthBox.setValue(TextCollation.STRENGTHS.get(2));
        strengthBox.setOnAction(_ -> updateSorting());
        decompositionBox.getItems().setAll(TextCollation.DECOMPOSITIONS);
        decompositionBox.setConverter(readOnly(TextCollation.Decomposition::name));
        decompositionBox.setValue(TextCollation.DECOMPOSITIONS.get(1));
        decompositionBox.setOnAction(_ -> {
            updateSorting();
            updateComparing();
        });
        sortLocalesBox.getItems().setAll(LocaleChoice.values());
        sortLocalesBox.setConverter(readOnly(choice -> choice == LocaleChoice.FIXED
                ? TextCollation.SORT_LOCALES.stream().map(Locale::toLanguageTag).reduce((a, b) -> a + ", " + b).orElse("")
                : resources.getString("common.locales.columns")));
        sortLocalesBox.setValue(LocaleChoice.FIXED);
        sortLocalesBox.setOnAction(_ -> updateSorting());

        wordSuggestions = SuggestionBox.install(wordsBox, context, "sort.words", WORD_SUGGESTIONS, value -> {
            updateSorting();
            wordSuggestions.remember(value);
        });
        // updateLocales() at the end of initialize() evaluates the words
        wordSuggestions.show(DEFAULT_WORDS);
    }

    private void updateSorting() {
        if (sortLocalesBox.getValue() == null || strengthBox.getValue() == null || decompositionBox.getValue() == null) {
            return;
        }
        var locales = sortLocalesBox.getValue() == LocaleChoice.COLUMNS ? state.columnLocales() : TextCollation.SORT_LOCALES;
        sorted.setLocales(locales);
        sorted.setRows(TextCollation.sortRows(TextFormats.items(text(wordsBox)), strengthBox.getValue(),
                decompositionBox.getValue(), locales.getFirst()));
    }

    // --- Comparing ---------------------------------------------------------------------------

    private void initComparing() {
        for (var field : List.of(firstField, secondField)) {
            field.textProperty().subscribe((_, _) -> updateComparing());
        }
        updateComparing();
    }

    private void updateComparing() {
        if (decompositionBox.getValue() == null) {
            return;
        }
        compared.setRows(TextCollation.compareRows(firstField.getText(), secondField.getText(), decompositionBox.getValue()));
        plain.setFacts(TextCollation.plainComparisons(firstField.getText(), secondField.getText()));
    }

    // --- Boundaries --------------------------------------------------------------------------

    private void initBoundaries() {
        boundaryLocalesBox.getItems().setAll(LocaleChoice.values());
        boundaryLocalesBox.setConverter(readOnly(choice -> choice == LocaleChoice.FIXED
                ? BOUNDARY_LOCALES.stream().map(Locale::toLanguageTag).reduce((a, b) -> a + ", " + b).orElse("")
                : resources.getString("common.locales.columns")));
        boundaryLocalesBox.setValue(LocaleChoice.FIXED);
        boundaryLocalesBox.setOnAction(_ -> updateLocales());

        boundarySuggestions = SuggestionBox.install(boundaryTextBox, context, "sort.boundaryText", BOUNDARY_SUGGESTIONS, value -> {
            boundaries.setRows(TextCollation.boundaryRows(value));
            boundarySuggestions.remember(value);
        });
        boundarySuggestions.setValue(DEFAULT_BOUNDARY_TEXT);
    }

    // --- Normalization -----------------------------------------------------------------------

    private void initNormalization() {
        normalizationSuggestions = SuggestionBox.install(normalizationBox, context, "sort.normalization",
                NORMALIZATION_SUGGESTIONS, value -> {
                    normalization.setFacts(TextCollation.normalizationFacts(value));
                    normalizationSuggestions.remember(value);
                });
        normalizationSuggestions.setValue(DEFAULT_NORMALIZATION_TEXT);
    }
}
