package name.ulbricht.locale.explorer.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.css.PseudoClass;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.shape.Rectangle;

import name.ulbricht.locale.explorer.i18n.Messages;
import name.ulbricht.locale.explorer.text.CodePoints;
import name.ulbricht.locale.explorer.text.LocaleProperty;
import name.ulbricht.locale.explorer.text.TextDirection;

/// Shows [LocaleProperty] rows with one column per locale.
///
/// Each locale column takes the text direction of its locale. Selecting a cell sends the Java
/// code for that row and locale to the code panel; the context menu copies the value, the
/// code or the code points.
final class LocaleTable {

    private static final PseudoClass EMPTY = PseudoClass.getPseudoClass("empty-value");
    private static final PseudoClass ERROR = PseudoClass.getPseudoClass("error-value");
    private static final PseudoClass HIGHLIGHT = PseudoClass.getPseudoClass("differs");
    private static final String NO_VALUE = "–";
    private static final String ERROR_PREFIX = "⚠ ";

    private final TableView<LocaleProperty> table;
    private final ResourceBundle resources;
    private final Consumer<String> codeSink;
    private final TableColumn<LocaleProperty, String> labelColumn;
    private List<Locale> locales = List.of();
    private boolean visualize;
    private boolean restoring;

    /// Sets up the table.
    ///
    /// @param table       the table to manage
    /// @param labelHeader the header of the first column
    /// @param context     the category context
    LocaleTable(TableView<LocaleProperty> table, String labelHeader, CategoryContext context) {
        this.table = table;
        this.resources = context.resources();
        this.codeSink = context.codeSink();

        labelColumn = new TableColumn<>(labelHeader);
        labelColumn.setSortable(false);
        labelColumn.setReorderable(false);
        labelColumn.setPrefWidth(220);
        labelColumn.setCellValueFactory(features -> new ReadOnlyStringWrapper(label(features.getValue())));
        labelColumn.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                Controls.applyScriptFont(this, empty ? null : item);
                Controls.showTooltip(this, empty || item == null ? null : item);
                // Labels that are code, such as patterns, are always left to right.
                var row = empty || getTableRow() == null ? null : getTableRow().getItem();
                setNodeOrientation(row != null && row.literal() ? NodeOrientation.LEFT_TO_RIGHT : NodeOrientation.INHERIT);
            }
        });

        table.getSelectionModel().setCellSelectionEnabled(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        // Only a table the user works with may change the code panel; tables on hidden tabs
        // also get a focused cell when their rows are set.
        table.getFocusModel().focusedCellProperty().subscribe(position -> {
            if (table.isFocused() && !restoring) {
                showCode(position);
            }
        });
        table.focusedProperty().subscribe(focused -> {
            if (focused) {
                showCode(table.getFocusModel().getFocusedCell());
            }
        });
        table.setContextMenu(contextMenu());
        table.getColumns().setAll(List.of(labelColumn));
    }

    /// Sets the locale columns.
    ///
    /// The columns are only rebuilt if the locales changed, so that column widths set by the user
    /// and the scroll position stay.
    ///
    /// @param locales the locales, one column each
    void setLocales(List<Locale> locales) {
        if (this.locales.equals(locales)) {
            return;
        }
        this.locales = List.copyOf(locales);
        var columns = new ArrayList<TableColumn<LocaleProperty, ?>>();
        columns.add(labelColumn);
        for (var locale : this.locales) {
            columns.add(localeColumn(locale));
        }
        keepingSelection(true, () -> table.getColumns().setAll(columns));
    }

    /// Sets the rows.
    ///
    /// If the rows are the same as before (same labels, as when the instant changes in live
    /// mode), the selected cells stay selected, so that the table can still be worked with.
    /// Otherwise the selection is cleared.
    ///
    /// @param rows the rows
    void setRows(List<LocaleProperty> rows) {
        var sameRows = rowKeys(table.getItems()).equals(rowKeys(rows));
        keepingSelection(sameRows, () -> table.getItems().setAll(rows));
    }

    private static List<List<Object>> rowKeys(List<LocaleProperty> rows) {
        return rows.stream().map(row -> List.<Object>of(row.labelKey(), row.literal(), row.labelArgs())).toList();
    }

    /// Runs a change of rows or columns and, if wanted, selects the same cells afterwards.
    ///
    /// Cells are identified by row index and by the locale of their column (or the label
    /// column); cells whose locale column is gone are dropped. The code panel is updated once
    /// at the end, not for each intermediate selection, so it does not flicker in live mode.
    private void keepingSelection(boolean keep, Runnable change) {
        record Cell(int row, Object column) {
        }
        var selected = table.getSelectionModel().getSelectedCells().stream()
                .map(position -> new Cell(position.getRow(), columnKey(position.getTableColumn())))
                .toList();
        var focused = table.getFocusModel().getFocusedCell();
        var focusedCell = focused == null ? null : new Cell(focused.getRow(), columnKey(focused.getTableColumn()));
        restoring = true;
        try {
            change.run();
            table.getSelectionModel().clearSelection();
            // clearSelection() also removes the focus that setAll() put on the first row
            if (keep) {
                for (var cell : selected) {
                    column(cell.column()).filter(_ -> cell.row() >= 0 && cell.row() < table.getItems().size())
                            .ifPresent(column -> table.getSelectionModel().select(cell.row(), column));
                }
                if (focusedCell != null && focusedCell.row() >= 0 && focusedCell.row() < table.getItems().size()) {
                    column(focusedCell.column()).ifPresent(column -> table.getFocusModel().focus(focusedCell.row(), column));
                }
            }
        } finally {
            restoring = false;
        }
        if (table.isFocused()) {
            showCode(table.getFocusModel().getFocusedCell());
        }
    }

    private Object columnKey(TableColumn<?, ?> column) {
        return column == null || column == labelColumn ? labelColumn : column.getUserData();
    }

    private Optional<TableColumn<LocaleProperty, ?>> column(Object key) {
        if (key == labelColumn) {
            return Optional.of(labelColumn);
        }
        return table.getColumns().stream().filter(column -> key != null && key.equals(column.getUserData())).findFirst();
    }

    /// Highlights invisible and look-alike characters in place, see [MarkedText].
    ///
    /// Copying a value always copies the original text.
    ///
    /// @param visualize whether to highlight special characters
    void setVisualizeSpecialCharacters(boolean visualize) {
        this.visualize = visualize;
        table.refresh();
    }

    private String label(LocaleProperty row) {
        String label;
        if (row.literal()) {
            label = row.labelKey();
        } else if (row.labelArgs().isEmpty()) {
            label = resources.getString(row.labelKey());
        } else {
            label = Messages.format(resources, row.labelKey(), row.labelArgs().toArray());
        }
        return row.descriptionKey() == null ? label : label + "  —  " + resources.getString(row.descriptionKey());
    }

    private TableColumn<LocaleProperty, String> localeColumn(Locale locale) {
        var column = new TableColumn<LocaleProperty, String>(locale.toLanguageTag());
        column.setUserData(locale);
        column.setSortable(false);
        column.setPrefWidth(200);
        column.setCellValueFactory(features -> new ReadOnlyStringWrapper(value(features.getValue(), locale)));
        var orientation = TextDirection.isRightToLeft(locale)
                ? NodeOrientation.RIGHT_TO_LEFT
                : NodeOrientation.LEFT_TO_RIGHT;
        column.setCellFactory(_ -> new TableCell<>() {
            {
                setNodeOrientation(orientation);
                // Highlighted values never wrap; cut them off at the cell border instead.
                var clip = new Rectangle();
                clip.widthProperty().bind(widthProperty());
                clip.heightProperty().bind(heightProperty());
                setClip(clip);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                var blank = !empty && item != null && item.isEmpty();
                var marked = !empty && item != null && visualize && CodePoints.hasSpecialCharacters(item);
                setText(empty || item == null || marked ? null : blank ? NO_VALUE : item);
                setGraphic(marked ? MarkedText.of(item) : null);
                Controls.applyScriptFont(this, empty ? null : item);
                Controls.showTooltip(this, empty || item == null || blank ? null :
                        CodePoints.hasSpecialCharacters(item) ? item + "\n" + CodePoints.describe(item) : item);
                pseudoClassStateChanged(EMPTY, blank);
                pseudoClassStateChanged(ERROR, !empty && item != null && item.startsWith(ERROR_PREFIX));
                var row = empty || getTableRow() == null ? null : getTableRow().getItem();
                pseudoClassStateChanged(HIGHLIGHT, row != null && row.highlight() != null && highlight(row, locale));
            }
        });
        return column;
    }

    private static boolean highlight(LocaleProperty row, Locale locale) {
        try {
            return row.highlight().test(locale);
        } catch (RuntimeException _) {
            return false;
        }
    }

    private static String value(LocaleProperty row, Locale locale) {
        try {
            var value = row.value().apply(locale);
            return value == null ? "" : value;
        } catch (RuntimeException e) {
            return ERROR_PREFIX + e.getClass().getSimpleName()
                    + (e.getMessage() == null ? "" : ": " + e.getMessage());
        }
    }

    private void showCode(TablePosition<?, ?> position) {
        var row = selectedRow(position);
        if (row == null) {
            codeSink.accept(null);
            return;
        }
        var locale = localeOf(position);
        codeSink.accept(locale == null ? null : row.code().apply(locale));
    }

    /// Returns the locale of the column of a cell; for the label column the first locale.
    private Locale localeOf(TablePosition<?, ?> position) {
        if (position.getTableColumn() != null && position.getTableColumn().getUserData() instanceof Locale columnLocale) {
            return columnLocale;
        }
        return locales.isEmpty() ? null : locales.getFirst();
    }

    private LocaleProperty selectedRow(TablePosition<?, ?> position) {
        if (position == null || position.getRow() < 0 || position.getRow() >= table.getItems().size()) {
            return null;
        }
        return table.getItems().get(position.getRow());
    }

    private ContextMenu contextMenu() {
        var copyValue = new MenuItem(resources.getString("context.copyValue"));
        copyValue.setOnAction(_ -> Controls.copyToClipboard(selectedText()));
        var copyCode = new MenuItem(resources.getString("context.copyCode"));
        copyCode.setOnAction(_ -> {
            var position = table.getFocusModel().getFocusedCell();
            var row = selectedRow(position);
            if (row != null) {
                var locale = localeOf(position);
                if (locale != null) {
                    Controls.copyToClipboard(row.code().apply(locale));
                }
            }
        });
        var copyCodePoints = new MenuItem(resources.getString("context.copyCodePoints"));
        copyCodePoints.setOnAction(_ -> Controls.copyToClipboard(CodePoints.describeWithNames(selectedText())));
        return new ContextMenu(copyValue, copyCode, copyCodePoints);
    }

    private String selectedText() {
        var position = table.getFocusModel().getFocusedCell();
        if (selectedRow(position) == null || position.getTableColumn() == null) {
            return "";
        }
        var data = position.getTableColumn().getCellData(position.getRow());
        return data == null ? "" : data.toString();
    }
}
