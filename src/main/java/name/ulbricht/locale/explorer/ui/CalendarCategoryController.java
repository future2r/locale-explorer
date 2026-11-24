package name.ulbricht.locale.explorer.ui;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Subscription;

import name.ulbricht.locale.explorer.calendar.Chronologies;
import name.ulbricht.locale.explorer.calendar.MonthSheet;
import name.ulbricht.locale.explorer.calendar.WeekRules;
import name.ulbricht.locale.explorer.model.AppState;
import name.ulbricht.locale.explorer.text.JavaCode;
import name.ulbricht.locale.explorer.text.LocaleProperty;
import name.ulbricht.locale.explorer.text.TextDirection;

/// Controller of the "Calendar, week" category.
public final class CalendarCategoryController implements CategoryController {

    private final CategoryContext context;
    private final AppState state;
    private final ResourceBundle resources;
    private final List<Subscription> subscriptions = new ArrayList<>();

    private LocaleTable rules;
    private LocaleTable chronologies;
    private LocaleTable turnOfYear;
    private YearMonth month;

    @FXML
    private TableView<LocaleProperty> rulesTable;
    @FXML
    private Button previousMonthButton;
    @FXML
    private Button nextMonthButton;
    @FXML
    private Label monthLabel;
    @FXML
    private CheckBox weekNumbersCheck;
    @FXML
    private FlowPane sheets;
    @FXML
    private TableView<LocaleProperty> chronoTable;
    @FXML
    private Spinner<Integer> yearSpinner;
    @FXML
    private TableView<LocaleProperty> turnTable;

    /// Creates the controller.
    ///
    /// @param context the category context
    public CalendarCategoryController(CategoryContext context) {
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
        rules = new LocaleTable(rulesTable, resources.getString("common.property"), context);
        chronologies = new LocaleTable(chronoTable, resources.getString("calendar.chronology"), context);
        turnOfYear = new LocaleTable(turnTable, resources.getString("calendar.turn.date"), context);

        // The buttons swap places in a right-to-left layout, so the arrows must swap too.
        var rightToLeft = state.getUiLanguage().isRightToLeft();
        initArrow(previousMonthButton, rightToLeft ? "▶" : "◀", "calendar.sheet.previous");
        initArrow(nextMonthButton, rightToLeft ? "◀" : "▶", "calendar.sheet.next");
        month = YearMonth.from(date());
        weekNumbersCheck.setOnAction(_ -> updateSheets());

        Controls.integerSpinner(yearSpinner, 1900, 2200, Math.clamp(date().getYear(), 1900, 2200));
        yearSpinner.valueProperty().subscribe((_, _) -> updateTurnOfYear());

        subscriptions.add(state.mainLocaleProperty().subscribe((_, _) -> updateLocales()));
        subscriptions.add(state.getCompareLocales().subscribe(this::updateLocales));
        subscriptions.add(state.dateTimeProperty().subscribe((oldValue, newValue) -> {
            if (!oldValue.toLocalDate().equals(newValue.toLocalDate())) {
                updateRows();
            }
        }));
        // updateRows() also draws the month sheets
        setColumnLocales();
        updateRows();
    }

    private LocalDate date() {
        return state.getDateTime().toLocalDate();
    }

    private void updateLocales() {
        setColumnLocales();
        updateSheets();
    }

    private void setColumnLocales() {
        var locales = state.columnLocales();
        rules.setLocales(locales);
        chronologies.setLocales(locales);
        turnOfYear.setLocales(locales);
    }

    private void updateRows() {
        rules.setRows(WeekRules.rows(date()));
        chronologies.setRows(Chronologies.rows(date()));
        updateTurnOfYear();
        updateSheets();
    }

    private void updateTurnOfYear() {
        var year = yearSpinner.getValue() == null ? date().getYear() : yearSpinner.getValue();
        turnOfYear.setRows(WeekRules.turnOfYearRows(year, state.getUiLanguage().locale()));
    }

    // --- Month sheets ------------------------------------------------------------------------

    private void initArrow(Button button, String arrow, String key) {
        var text = resources.getString(key);
        button.setText(arrow);
        button.setTooltip(new Tooltip(text));
        button.setAccessibleText(text);
    }

    @FXML
    private void onPreviousMonth() {
        month = month.minusMonths(1);
        updateSheets();
    }

    @FXML
    private void onNextMonth() {
        month = month.plusMonths(1);
        updateSheets();
    }

    @FXML
    private void onToolbarMonth() {
        month = YearMonth.from(date());
        updateSheets();
    }

    private void updateSheets() {
        if (month == null) {
            return;
        }
        monthLabel.setText(DateTimeFormatter.ofPattern("LLLL y").localizedBy(state.getUiLanguage().locale())
                .format(month.atDay(1)));
        sheets.getChildren().setAll(state.columnLocales().stream().map(this::sheet).toList());
    }

    private Node sheet(Locale locale) {
        var sheet = MonthSheet.of(month, locale);
        var showWeeks = weekNumbersCheck.isSelected();
        var offset = showWeeks ? 1 : 0;

        var grid = new GridPane();
        grid.getStyleClass().add("month-grid");
        if (showWeeks) {
            grid.add(cell("#", "week-number"), 0, 0);
        }
        for (var i = 0; i < sheet.weekdays().size(); i++) {
            grid.add(cell(sheet.weekdays().get(i), "weekday"), i + offset, 0);
        }
        var today = date();
        for (var row = 0; row < sheet.weeks().size(); row++) {
            var week = sheet.weeks().get(row);
            if (showWeeks) {
                grid.add(cell(week.number(), "week-number"), 0, row + 1);
            }
            for (var column = 0; column < week.days().size(); column++) {
                var day = week.days().get(column);
                var styleClass = day.date().equals(today) ? "selected-day" : day.inMonth() ? "day" : "other-month";
                grid.add(cell(day.text(), styleClass), column + offset, row + 1);
            }
        }

        var title = new Label(sheet.title());
        Controls.applyScriptFont(title, sheet.title());
        title.getStyleClass().add("result-value");
        var tag = new Label(locale.toLanguageTag());
        tag.getStyleClass().add("field-label");
        tag.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        var spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        var header = new HBox(title, spacer, tag);
        header.getStyleClass().add("info-row");

        var box = new VBox(header, grid);
        box.getStyleClass().add("month-sheet");
        box.setNodeOrientation(TextDirection.isRightToLeft(locale)
                ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
        box.setOnMouseClicked(_ -> context.codeSink().accept(JavaCode.withLocale(locale,
                "var fields = WeekFields.of(locale);",
                "var first = YearMonth.parse(" + JavaCode.string(month.toString()) + ").atDay(1)",
                "        .with(TemporalAdjusters.previousOrSame(fields.getFirstDayOfWeek()));",
                "DateTimeFormatter.ofPattern(\"d\").localizedBy(locale).format(first);",
                "first.get(fields.weekOfWeekBasedYear());")));
        return box;
    }

    private static Label cell(String text, String styleClass) {
        var label = new Label(text);
        Controls.applyScriptFont(label, text);
        label.getStyleClass().addAll("month-cell", styleClass);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }
}
