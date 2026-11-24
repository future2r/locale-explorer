package name.ulbricht.locale.explorer.ui;

import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import name.ulbricht.locale.explorer.i18n.Messages;
import name.ulbricht.locale.explorer.text.Fact;

/// Shows locale-independent [Fact]s in two columns: label and value.
///
/// Selecting a row sends its Java code to the code panel.
final class FactTable {

    private final TableView<Fact> table;
    private final ResourceBundle resources;
    private final Consumer<String> codeSink;
    private boolean restoring;

    /// Sets up the table.
    ///
    /// @param table       the table to manage
    /// @param labelHeader the header of the label column
    /// @param valueHeader the header of the value column
    /// @param context     the category context
    FactTable(TableView<Fact> table, String labelHeader, String valueHeader, CategoryContext context) {
        this.table = table;
        this.resources = context.resources();
        this.codeSink = context.codeSink();

        var labelColumn = new TableColumn<Fact, String>(labelHeader);
        labelColumn.setSortable(false);
        labelColumn.setPrefWidth(320);
        labelColumn.setCellValueFactory(features -> new ReadOnlyStringWrapper(label(features.getValue())));
        var valueColumn = new TableColumn<Fact, String>(valueHeader);
        valueColumn.setSortable(false);
        valueColumn.setPrefWidth(420);
        valueColumn.setCellValueFactory(features -> new ReadOnlyStringWrapper(value(features.getValue())));
        // Values are mostly code, IDs and numbers: always left to right. Labels follow the UI.
        valueColumn.setCellFactory(_ -> new TableCell<>() {
            {
                setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                Controls.applyScriptFont(this, empty ? null : item);
                Controls.showTooltip(this, empty || item == null ? null : item);
            }
        });
        table.getColumns().setAll(List.of(labelColumn, valueColumn));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getSelectionModel().selectedItemProperty().subscribe(fact -> {
            if (fact != null && table.isFocused() && !restoring) {
                codeSink.accept(fact.code());
            }
        });
        table.focusedProperty().subscribe(focused -> {
            if (focused && selected() != null) {
                codeSink.accept(selected().code());
            }
        });

        var copyValue = new MenuItem(resources.getString("context.copyValue"));
        copyValue.setOnAction(_ -> Controls.copyToClipboard(selected() == null ? null : value(selected())));
        var copyCode = new MenuItem(resources.getString("context.copyCode"));
        copyCode.setOnAction(_ -> Controls.copyToClipboard(selected() == null ? null : selected().code()));
        table.setContextMenu(new ContextMenu(copyValue, copyCode));
    }

    /// Sets the rows.
    ///
    /// @param facts the facts
    void setFacts(List<Fact> facts) {
        // The same rows with new values, as every second in live mode, keep their selection.
        var sameRows = rowKeys(table.getItems()).equals(rowKeys(facts));
        var selected = table.getSelectionModel().getSelectedIndex();
        restoring = true;
        try {
            table.getItems().setAll(facts);
            table.getSelectionModel().clearSelection();
            // clearSelection() also removes the focus that setAll() put on the first row
            if (sameRows && selected >= 0) {
                table.getSelectionModel().select(selected);
            }
        } finally {
            restoring = false;
        }
        if (table.isFocused()) {
            codeSink.accept(selected() == null ? null : selected().code());
        }
    }

    private static List<List<Object>> rowKeys(List<Fact> facts) {
        return facts.stream().map(fact -> List.<Object>of(fact.labelKey(), fact.labelArgs())).toList();
    }

    private Fact selected() {
        return table.getSelectionModel().getSelectedItem();
    }

    private String label(Fact fact) {
        return label(fact, resources);
    }

    /// Returns the label of a fact as shown in the table.
    ///
    /// @param fact      the fact
    /// @param resources the messages of the UI language
    /// @return the label
    static String label(Fact fact, ResourceBundle resources) {
        return fact.labelArgs().isEmpty()
                ? resources.getString(fact.labelKey())
                : Messages.format(resources, fact.labelKey(), fact.labelArgs().toArray());
    }

    private String value(Fact fact) {
        return value(fact, resources);
    }

    /// Returns the value of a fact as shown in the table: translated if it has a value key,
    /// a dash if it is empty.
    ///
    /// @param fact      the fact
    /// @param resources the messages of the UI language
    /// @return the value
    static String value(Fact fact, ResourceBundle resources) {
        if (fact.valueKey() != null) {
            return resources.getString(fact.valueKey());
        }
        var value = fact.value();
        return value.isEmpty() ? "–" : value;
    }
}
