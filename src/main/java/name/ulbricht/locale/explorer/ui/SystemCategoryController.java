package name.ulbricht.locale.explorer.ui;

import static name.ulbricht.locale.explorer.ui.Controls.readOnly;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TimeZone;

import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Subscription;

import name.ulbricht.locale.explorer.model.AppState;
import name.ulbricht.locale.explorer.system.SystemInfo;
import name.ulbricht.locale.explorer.system.SystemInfo.Defaults;
import name.ulbricht.locale.explorer.system.SystemInfo.Experiment;
import name.ulbricht.locale.explorer.text.Fact;
import name.ulbricht.locale.explorer.text.Locales;

/// Controller of the "System" category.
public final class SystemCategoryController implements CategoryController {

    private static final PseudoClass DIFFERS = PseudoClass.getPseudoClass("differs");

    private enum Scope {
        ALL, DISPLAY, FORMAT
    }

    private final CategoryContext context;
    private final AppState state;
    private final ResourceBundle resources;
    private final List<Subscription> subscriptions = new ArrayList<>();

    private FactTable runtime;
    private FactTable defaults;
    private FactTable availability;

    @FXML
    private Label copyStatus;
    @FXML
    private Tab runtimeTab;
    @FXML
    private Tab defaultsTab;
    @FXML
    private Tab availabilityTab;
    @FXML
    private TableView<Fact> runtimeTable;
    @FXML
    private TableView<Fact> defaultsTable;
    @FXML
    private TableView<Fact> availabilityTable;
    @FXML
    private ComboBox<Locale> defaultLocaleBox;
    @FXML
    private ComboBox<Scope> scopeBox;
    @FXML
    private ComboBox<String> defaultZoneBox;
    @FXML
    private TableView<Experiment> experimentTable;
    @FXML
    private TableColumn<Experiment, String> callColumn;
    @FXML
    private TableColumn<Experiment, String> originalColumn;
    @FXML
    private TableColumn<Experiment, String> currentColumn;

    /// Creates the controller.
    ///
    /// @param context the category context
    public SystemCategoryController(CategoryContext context) {
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
        var property = resources.getString("common.property");
        var value = resources.getString("common.value");
        runtime = new FactTable(runtimeTable, property, value, context);
        defaults = new FactTable(defaultsTable, property, value, context);
        availability = new FactTable(availabilityTable, resources.getString("common.method"), value, context);

        runtime.setFacts(SystemInfo.runtimeFacts());
        updateDefaults();
        subscriptions.add(state.mainLocaleProperty().subscribe(locale -> availability.setFacts(SystemInfo.availabilityFacts(locale))));
        initChange();
    }

    private void updateDefaults() {
        var facts = new ArrayList<>(SystemInfo.defaultFacts());
        facts.add(Fact.of("system.uiLanguage", state.getUiLanguage().tag(), "// setting of this application"));
        facts.add(Fact.of("system.colorScheme", Platform.getPreferences().getColorScheme() == ColorScheme.DARK ? "DARK" : "LIGHT",
                "Platform.getPreferences().getColorScheme();"));
        defaults.setFacts(facts);
    }

    // --- Changing the defaults ---------------------------------------------------------------

    private void initChange() {
        var converter = new LocaleConverter(state.getUiLanguage().locale());
        defaultLocaleBox.getItems().setAll(Locales.available());
        defaultLocaleBox.setConverter(converter);
        defaultLocaleBox.setValue(Locale.getDefault());

        scopeBox.getItems().setAll(Scope.values());
        scopeBox.setConverter(readOnly(scope -> scope == Scope.ALL ? resources.getString("system.change.all") : scope.name()));
        scopeBox.setValue(Scope.ALL);

        defaultZoneBox.getItems().setAll(ZoneId.getAvailableZoneIds().stream().sorted().toList());
        defaultZoneBox.setValue(TimeZone.getDefault().getID());
        defaultZoneBox.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);

        experimentTable.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        experimentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        callColumn.setCellValueFactory(features -> new ReadOnlyStringWrapper(features.getValue().call()));
        originalColumn.setCellValueFactory(features -> new ReadOnlyStringWrapper(SystemInfo.originalValue(features.getValue())));
        currentColumn.setCellValueFactory(features -> new ReadOnlyStringWrapper(features.getValue().evaluate()));
        currentColumn.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                Controls.applyScriptFont(this, empty ? null : item);
                var row = empty || getTableRow() == null ? null : getTableRow().getItem();
                pseudoClassStateChanged(DIFFERS, row != null && !SystemInfo.originalValue(row).equals(item));
            }
        });
        experimentTable.getSelectionModel().selectedItemProperty().subscribe(experiment -> {
            if (experiment != null) {
                context.codeSink().accept(experiment.call() + ";\n// depends on Locale.getDefault() and TimeZone.getDefault()");
            }
        });
        experimentTable.getItems().setAll(SystemInfo.EXPERIMENTS);
    }

    @FXML
    private void onApplyDefaults() {
        var locale = defaultLocaleBox.getValue();
        if (locale != null) {
            switch (scopeBox.getValue()) {
                case ALL -> Locale.setDefault(locale);
                case DISPLAY -> Locale.setDefault(Locale.Category.DISPLAY, locale);
                case FORMAT -> Locale.setDefault(Locale.Category.FORMAT, locale);
            }
        }
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(defaultZoneBox.getValue())));
        } catch (RuntimeException _) {
            // keep the previous time zone for an invalid ID
        }
        refreshExperiments();
    }

    @FXML
    private void onResetDefaults() {
        SystemInfo.ORIGINAL.apply();
        defaultLocaleBox.setValue(Locale.getDefault());
        defaultZoneBox.setValue(TimeZone.getDefault().getID());
        refreshExperiments();
    }

    private void refreshExperiments() {
        experimentTable.refresh();
        updateDefaults();
    }

    // --- Markdown ----------------------------------------------------------------------------

    @FXML
    private void onCopyMarkdown() {
        var markdown = new StringBuilder("# ").append(resources.getString("app.title")).append("\n");
        appendSection(markdown, runtimeTab.getText(), runtimeTable.getItems());
        appendSection(markdown, defaultsTab.getText(), defaultsTable.getItems());
        appendSection(markdown, availabilityTab.getText(), availabilityTable.getItems());
        Controls.copyToClipboard(markdown.toString());
        copyStatus.setText(resources.getString("system.copied"));
    }

    private void appendSection(StringBuilder markdown, String title, List<Fact> facts) {
        markdown.append("\n## ").append(title).append("\n\n");
        markdown.append("| ").append(resources.getString("common.property")).append(" | ")
                .append(resources.getString("common.value")).append(" |\n|---|---|\n");
        for (var fact : facts) {
            var label = FactTable.label(fact, resources);
            var value = FactTable.value(fact, resources);
            markdown.append("| ").append(escape(label)).append(" | ").append(escape(value)).append(" |\n");
        }
    }

    private static String escape(String text) {
        return text.replace("|", "\\|").replace("\n", " ");
    }
}
