package name.ulbricht.locale.explorer.ui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.zone.ZoneRulesProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.util.Subscription;
import javafx.util.converter.LocalDateStringConverter;

import name.ulbricht.locale.explorer.export.TableExport;
import name.ulbricht.locale.explorer.i18n.Messages;
import name.ulbricht.locale.explorer.i18n.UiLanguage;
import name.ulbricht.locale.explorer.model.AppState;
import name.ulbricht.locale.explorer.model.Category;
import name.ulbricht.locale.explorer.model.PreferencesStore;
import name.ulbricht.locale.explorer.model.Theme;
import name.ulbricht.locale.explorer.text.Locales;

/// Controller of the main window: settings, toolbar, category list, code panel and status bar.
///
/// A new instance is created whenever the UI language changes; [#dispose()] removes the
/// listeners this controller registered on the long-lived [AppState].
public final class MainController {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");

    private final AppState state;
    private final PreferencesStore store;
    private final List<Subscription> subscriptions = new ArrayList<>();
    private CategoryController categoryController;
    private boolean updating;
    /// The time text last written into the time field by the program.
    private String shownTime = "";

    @FXML
    private ResourceBundle resources;
    @FXML
    private MenuButton settingsButton;
    @FXML
    private ComboBox<UiLanguage> languageBox;
    @FXML
    private ComboBox<Theme> themeBox;
    @FXML
    private Spinner<Integer> fontSizeSpinner;
    @FXML
    private ComboBox<Locale> mainLocaleBox;
    @FXML
    private HBox compareChips;
    @FXML
    private ComboBox<Locale> addCompareBox;
    @FXML
    private ComboBox<String> zoneBox;
    @FXML
    private DatePicker datePicker;
    @FXML
    private TextField timeField;
    @FXML
    private CheckBox liveCheck;
    @FXML
    private Label resolutionHint;
    @FXML
    private ListView<Category> categoryList;
    @FXML
    private StackPane categoryHost;
    @FXML
    private TextArea codeArea;
    @FXML
    private Label javaLabel;
    @FXML
    private Label javafxLabel;
    @FXML
    private Label tzdataLabel;
    @FXML
    private Label localesLabel;
    @FXML
    private Label statusMessage;

    /// Creates the controller.
    ///
    /// @param state the application state
    /// @param store the preferences store
    public MainController(AppState state, PreferencesStore store) {
        this.state = state;
        this.store = store;
    }

    /// Removes all listeners registered on the application state.
    public void dispose() {
        disposeCategory();
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
    }

    @FXML
    private void initialize() {
        initSettings();
        initLocales();
        initZone();
        initDateTime();
        initCategories();
        initStatusBar();
        initAccessibleTexts();
    }

    /// Names the toolbar inputs for screen readers; their visible labels are separate nodes.
    private void initAccessibleTexts() {
        mainLocaleBox.setAccessibleText(resources.getString("toolbar.locale"));
        addCompareBox.setAccessibleText(resources.getString("toolbar.compare.add"));
        zoneBox.setAccessibleText(resources.getString("toolbar.zone"));
        datePicker.setAccessibleText(resources.getString("toolbar.instant"));
        timeField.setAccessibleText(resources.getString("toolbar.instant"));
    }

    private void initSettings() {
        languageBox.getItems().setAll(UiLanguage.values());
        languageBox.setConverter(Controls.readOnly(UiLanguage::nativeName));
        languageBox.setValue(state.getUiLanguage());
        ChoiceHandler.install(languageBox, language -> {
            if (language != null && language != state.getUiLanguage()) {
                settingsButton.hide();
                // The whole view is rebuilt, so let the current event finish first.
                Platform.runLater(() -> state.setUiLanguage(language));
            }
        });

        themeBox.getItems().setAll(Theme.values());
        themeBox.setConverter(Controls.readOnly(theme -> resources.getString(theme.messageKey())));
        themeBox.setValue(state.getTheme());
        ChoiceHandler.install(themeBox, theme -> {
            if (theme != null) {
                state.setTheme(theme);
            }
        });

        Controls.integerSpinner(fontSizeSpinner, AppState.MIN_FONT_SIZE, AppState.MAX_FONT_SIZE, state.getFontSize());
        fontSizeSpinner.valueProperty().subscribe(size -> {
            if (size != null) {
                state.setFontSize(size);
            }
        });
    }

    private void initLocales() {
        var converter = new LocaleConverter(state.getUiLanguage().locale());

        mainLocaleBox.getItems().setAll(Locales.available());
        mainLocaleBox.setConverter(converter);
        mainLocaleBox.setValue(state.getMainLocale());
        ChoiceHandler.install(mainLocaleBox, locale -> {
            if (locale == null) {
                mainLocaleBox.setValue(state.getMainLocale());
            } else {
                state.setMainLocale(locale);
            }
        });
        // The main locale can also be changed in the Locale category.
        subscriptions.add(state.mainLocaleProperty().subscribe(locale -> {
            if (!locale.equals(mainLocaleBox.getValue())) {
                mainLocaleBox.setValue(locale);
            }
        }));

        addCompareBox.getItems().setAll(Locales.available());
        addCompareBox.setConverter(converter);
        ChoiceHandler.install(addCompareBox, locale -> {
            if (locale == null) {
                return;
            }
            var compare = state.getCompareLocales();
            if (!compare.contains(locale) && !locale.equals(state.getMainLocale())) {
                compare.add(locale);
            }
            Platform.runLater(() -> addCompareBox.getSelectionModel().clearSelection());
        });

        subscriptions.add(state.getCompareLocales().subscribe(this::updateCompareChips));
        // Code shown for a column refers to its locale; after a locale change it may be gone.
        subscriptions.add(state.mainLocaleProperty().subscribe((_, _) -> showCode(null)));
        subscriptions.add(state.getCompareLocales().subscribe(() -> showCode(null)));
        updateCompareChips();
    }

    private void updateCompareChips() {
        var displayLocale = state.getUiLanguage().locale();
        compareChips.getChildren().setAll(state.getCompareLocales().stream()
                .map(locale -> compareChip(locale, displayLocale))
                .toList());
    }

    private Node compareChip(Locale locale, Locale displayLocale) {
        var chip = new Button(locale.toLanguageTag() + "  ✕");
        chip.getStyleClass().add("locale-chip");
        chip.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        var removeText = Messages.format(resources, "toolbar.compare.remove", locale.getDisplayName(displayLocale));
        chip.setTooltip(Controls.tooltip(removeText));
        chip.setAccessibleText(removeText);
        chip.setOnAction(_ -> state.getCompareLocales().remove(locale));
        return chip;
    }

    private void initZone() {
        zoneBox.getItems().setAll(ZoneId.getAvailableZoneIds().stream().sorted().toList());
        zoneBox.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        ChoiceHandler.install(zoneBox, text -> {
            try {
                state.setZone(ZoneId.of(text == null ? "" : text.strip()));
                zoneBox.pseudoClassStateChanged(ERROR, false);
            } catch (DateTimeException _) {
                zoneBox.pseudoClassStateChanged(ERROR, true);
            }
        });
        subscriptions.add(state.zoneProperty().subscribe(zone -> zoneBox.setValue(zone.getId())));
        subscriptions.add(state.zoneProperty().subscribe((_, _) -> updateResolutionHint()));
    }

    private void initDateTime() {
        datePicker.setConverter(new LocalDateStringConverter(
                DateTimeFormatter.ISO_LOCAL_DATE, DateTimeFormatter.ISO_LOCAL_DATE));
        datePicker.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        // The calendar popup names months and weekdays in the default format locale.
        Controls.applyScriptFont(datePicker, DateTimeFormatter.ofPattern("LLLL ccc")
                .withLocale(Locale.getDefault(Locale.Category.FORMAT))
                .format(LocalDate.now()));
        datePicker.valueProperty().subscribe(date -> {
            if (!updating && date != null) {
                state.setDateTime(state.getDateTime().with(date));
            }
        });

        timeField.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        timeField.setOnAction(_ -> commitTime());
        timeField.focusedProperty().subscribe((_, focused) -> {
            // Only an edited time is taken over when leaving the field; otherwise, in live mode, the
            // time shown when the field got the focus would be written back.
            if (focused) {
                return;
            }
            if (timeField.getText().equals(shownTime)) {
                // back to the shown time: nothing to take over, but no error either
                timeField.pseudoClassStateChanged(ERROR, false);
                timeField.setTooltip(null);
            } else {
                commitTime();
            }
        });

        liveCheck.setSelected(state.isLive());
        liveCheck.selectedProperty().subscribe(state::setLive);

        subscriptions.add(state.dateTimeProperty().subscribe(this::showDateTime));
    }

    private void showDateTime(LocalDateTime dateTime) {
        updating = true;
        try {
            datePicker.setValue(dateTime.toLocalDate());
            if (!timeField.isFocused()) {
                shownTime = dateTime.toLocalTime().format(TIME_FORMAT);
                timeField.setText(shownTime);
                timeField.pseudoClassStateChanged(ERROR, false);
                timeField.setTooltip(null);
            }
        } finally {
            updating = false;
        }
        updateResolutionHint();
    }

    private void commitTime() {
        try {
            // whole seconds, like "Now" and live mode; shown in the same format, also while the
            // field keeps the focus
            var time = LocalTime.parse(timeField.getText().strip()).truncatedTo(ChronoUnit.SECONDS);
            shownTime = time.format(TIME_FORMAT);
            if (!timeField.getText().equals(shownTime)) {
                timeField.setText(shownTime);
                // setText puts the caret at the start; keep editing at the end
                timeField.end();
            }
            timeField.pseudoClassStateChanged(ERROR, false);
            timeField.setTooltip(null);
            LocalDate date = state.getDateTime().toLocalDate();
            state.setDateTime(LocalDateTime.of(date, time));
        } catch (DateTimeParseException _) {
            timeField.pseudoClassStateChanged(ERROR, true);
            timeField.setTooltip(Controls.tooltip(resources.getString("toolbar.time.invalid")));
        }
    }

    @FXML
    private void onNow() {
        state.setDateTime(LocalDateTime.now(state.getZone()).truncatedTo(ChronoUnit.SECONDS));
    }

    private void updateResolutionHint() {
        var resolution = state.resolution();
        var text = switch (resolution.kind()) {
            case NORMAL -> "";
            case GAP -> Messages.format(resources, "hint.gap",
                    resolution.resolved().toLocalTime().format(TIME_FORMAT) + " (" + resolution.offset().getId() + ")");
            case OVERLAP -> Messages.format(resources, "hint.overlap", resolution.offset().getId());
        };
        resolutionHint.setText(text);
        resolutionHint.setVisible(!text.isEmpty());
        resolutionHint.setManaged(!text.isEmpty());
    }

    private void initCategories() {
        categoryList.getItems().setAll(Category.values());
        categoryList.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(Category category, boolean empty) {
                super.updateItem(category, empty);
                setText(empty || category == null ? null : resources.getString(category.messageKey()));
            }
        });
        categoryList.getSelectionModel().selectedItemProperty().subscribe(category -> {
            if (category != null) {
                state.setCategory(category);
                showCategory(category);
            }
        });
        categoryList.getSelectionModel().select(state.getCategory());
    }

    private void showCategory(Category category) {
        disposeCategory();
        showCode(null);
        var context = new CategoryContext(state, store, resources, this::showCode);
        var view = CategoryViews.load(category, context);
        if (view.isPresent()) {
            categoryController = view.get().controller();
            categoryHost.getChildren().setAll(view.get().node());
            if (view.get().node().lookup(".category-tabs") instanceof TabPane tabs) {
                tabs.getSelectionModel().selectedItemProperty().subscribe((_, _) -> showCode(null));
            }
        } else {
            var title = new Label(resources.getString(category.messageKey()));
            title.getStyleClass().add("category-title");
            var text = new Label(resources.getString("category.placeholder"));
            text.setWrapText(true);
            var placeholder = new VBox(title, text);
            placeholder.getStyleClass().add("category-placeholder");
            categoryHost.getChildren().setAll(placeholder);
        }
    }

    private void disposeCategory() {
        if (categoryController != null) {
            categoryController.dispose();
            categoryController = null;
        }
    }

    private void showCode(String code) {
        // Java code is always left to right; the hint follows the UI language.
        var text = code == null ? resources.getString("code.none") : code;
        if (text.equals(codeArea.getText())) {
            // keep the scroll position when the same code is sent again, as in live mode
            return;
        }
        codeArea.setNodeOrientation(code == null ? NodeOrientation.INHERIT : NodeOrientation.LEFT_TO_RIGHT);
        codeArea.setText(text);
    }

    private void initStatusBar() {
        javaLabel.setText(Messages.format(resources, "status.java", Runtime.version().toString()));
        javafxLabel.setText(Messages.format(resources, "status.javafx",
                System.getProperty("javafx.runtime.version", "?")));
        tzdataLabel.setText(Messages.format(resources, "status.tzdata",
                ZoneRulesProvider.getVersions("UTC").lastKey()));
        localesLabel.setText(Messages.format(resources, "status.locales", Locales.available().size()));
        for (var label : List.of(javaLabel, javafxLabel, tzdataLabel)) {
            label.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        }
    }

    @FXML
    private void onAbout() {
        var alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(resources.getString("about.title"));
        alert.setHeaderText(resources.getString("app.title"));
        alert.setContentText(String.join("\n", resources.getString("about.text"), "",
                javaLabel.getText(), javafxLabel.getText(), tzdataLabel.getText()));
        showDialog(alert);
    }

    /// Shows a dialog with the theme, font size and text direction of the main window.
    private void showDialog(Alert alert) {
        var scene = categoryHost.getScene();
        alert.initOwner(scene.getWindow());
        var pane = alert.getDialogPane();
        pane.getStylesheets().setAll(scene.getStylesheets());
        pane.setStyle(scene.getRoot().getStyle());
        pane.setNodeOrientation(scene.getRoot().getNodeOrientation());
        alert.showAndWait();
    }

    // --- Export ------------------------------------------------------------------------------

    @FXML
    private void onCopyMarkdown() {
        exportTables().ifPresent(export -> {
            Controls.copyToClipboard(TableExport.markdown(export.title(), export.tables()));
            showStatus(resources.getString("export.copied"));
        });
    }

    @FXML
    private void onSaveMarkdown() {
        exportTables().ifPresent(export -> save(export, "md", "export.filter.markdown",
                TableExport.markdown(export.title(), export.tables())));
    }

    @FXML
    private void onSaveCsv() {
        exportTables().ifPresent(export -> save(export, "csv", "export.filter.csv",
                TableExport.BYTE_ORDER_MARK + TableExport.csv(export.tables())));
    }

    private record Export(String title, String fileName, List<TableExport.Table> tables) {
    }

    private java.util.Optional<Export> exportTables() {
        var view = categoryHost.getChildren().isEmpty() ? null : categoryHost.getChildren().getFirst();
        var result = view == null ? java.util.Optional.<VisibleTables.Result>empty() : VisibleTables.read(view);
        if (result.isEmpty()) {
            showStatus(resources.getString("export.noTable"));
            return java.util.Optional.empty();
        }
        var category = resources.getString(state.getCategory().messageKey());
        var tab = result.get().tabTitle();
        var title = tab.isEmpty() ? category : category + " · " + tab;
        var fileName = (state.getCategory().id() + (tab.isEmpty() ? "" : "-" + tab))
                .replaceAll("[^\\p{L}\\p{N}._-]+", "-").toLowerCase(Locale.ROOT);
        return java.util.Optional.of(new Export(title, fileName, result.get().tables()));
    }

    private void save(Export export, String extension, String filterKey, String content) {
        var chooser = new FileChooser();
        chooser.setInitialFileName(export.fileName() + "." + extension);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(resources.getString(filterKey), "*." + extension));
        var file = chooser.showSaveDialog(categoryHost.getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
            showStatus(Messages.format(resources, "export.saved", file.getName()));
        } catch (IOException e) {
            showDialog(new Alert(Alert.AlertType.ERROR, Messages.format(resources, "export.failed", e.getMessage())));
        }
    }

    private void showStatus(String message) {
        statusMessage.setText(message);
        var clear = new PauseTransition(Duration.seconds(6));
        clear.setOnFinished(_ -> {
            if (message.equals(statusMessage.getText())) {
                statusMessage.setText("");
            }
        });
        clear.play();
    }

    @FXML
    private void onExit() {
        Platform.exit();
    }

}
