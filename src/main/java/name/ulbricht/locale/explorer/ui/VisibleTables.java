package name.ulbricht.locale.explorer.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javafx.scene.Node;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;

import name.ulbricht.locale.explorer.export.TableExport;

/// Finds the tables of the visible tab of a category view and reads their contents.
final class VisibleTables {

    /// The tables of the visible tab.
    ///
    /// @param tabTitle the title of the tab, or an empty string if the view has no tabs
    /// @param tables   the contents of the tables, in layout order
    record Result(String tabTitle, List<TableExport.Table> tables) {
    }

    private VisibleTables() {
    }

    /// Reads the tables of the visible tab.
    ///
    /// @param categoryView the root node of the category view
    /// @return the tables, or empty if there are none
    static Optional<Result> read(Node categoryView) {
        var container = categoryView;
        var tabTitle = "";
        if (categoryView.lookup(".category-tabs") instanceof TabPane tabs && tabs.getSelectionModel().getSelectedItem() != null) {
            var tab = tabs.getSelectionModel().getSelectedItem();
            container = tab.getContent();
            tabTitle = tab.getText();
        }
        if (container == null) {
            return Optional.empty();
        }
        var tables = new ArrayList<TableExport.Table>();
        for (var node : container.lookupAll(".table-view")) {
            if (node instanceof TableView<?> table && table.isVisible() && !table.getVisibleLeafColumns().isEmpty()) {
                tables.add(contents(table));
            }
        }
        return tables.isEmpty() ? Optional.empty() : Optional.of(new Result(tabTitle, tables));
    }

    private static TableExport.Table contents(TableView<?> table) {
        var columns = table.getVisibleLeafColumns();
        var headers = columns.stream().map(column -> column.getText() == null ? "" : column.getText()).toList();
        var rows = new ArrayList<List<String>>();
        for (var index = 0; index < table.getItems().size(); index++) {
            var row = new ArrayList<String>();
            for (var column : columns) {
                row.add(text(column.getCellData(index)));
            }
            rows.add(row);
        }
        return new TableExport.Table(headers, rows);
    }

    private static String text(Object value) {
        return switch (value) {
            case null -> "";
            case Locale locale -> locale.getDisplayName(locale);
            default -> value.toString();
        };
    }
}
