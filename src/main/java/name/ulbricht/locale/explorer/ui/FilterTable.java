package name.ulbricht.locale.explorer.ui;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.Label;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;

import name.ulbricht.locale.explorer.i18n.Messages;

/// A searchable list of all locales, zones or currencies, as used by the "All …" tabs.
///
/// Shows the items in a table, filters them with a predicate built by the owner from a filter
/// field and further options, and shows how many items are left.
///
/// @param <T> the item type
final class FilterTable<T> {

    private final TableView<T> table;
    private final FilteredList<T> items;
    private final int total;
    private final TextField filterField;
    private final Label countLabel;
    private final CategoryContext context;

    /// Sets up the table.
    ///
    /// @param table       the table
    /// @param all         all items
    /// @param filterField the text field for the search text
    /// @param countLabel  the label showing the number of matching items
    /// @param context     the category context
    FilterTable(TableView<T> table, List<T> all, TextField filterField, Label countLabel, CategoryContext context) {
        this.table = table;
        this.items = new FilteredList<>(FXCollections.observableArrayList(all));
        this.total = all.size();
        this.filterField = filterField;
        this.countLabel = countLabel;
        this.context = context;
        // A filtered list cannot be sorted itself; the sorted view follows the column headers.
        var sorted = new SortedList<>(items);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
    }

    /// Calls an action when a row is double-clicked.
    ///
    /// @param action called with the item of the row
    void onDoubleClick(Consumer<T> action) {
        table.setRowFactory(_ -> {
            var row = new TableRow<T>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && !row.isEmpty()) {
                    action.accept(row.getItem());
                }
            });
            return row;
        });
    }

    /// Returns the search text, stripped and in lower case of the UI language.
    ///
    /// @return the search text, empty if there is none
    String query() {
        var text = filterField.getText();
        return text == null ? "" : text.strip().toLowerCase(uiLocale());
    }

    /// Returns the locale of the user interface, for names and case-insensitive search.
    ///
    /// @return the locale
    Locale uiLocale() {
        return context.state().getUiLanguage().locale();
    }

    /// Shows only the items matching a predicate and updates the count.
    ///
    /// If the same items match as before, the list is left alone, so that selection and scroll
    /// position survive when the filter is applied again, for example every second in live mode.
    ///
    /// @param predicate the predicate
    void filter(Predicate<T> predicate) {
        var matching = items.getSource().stream().filter(predicate).toList();
        if (!matching.equals(List.copyOf(items))) {
            items.setPredicate(predicate);
        }
        countLabel.setText(Messages.format(context.resources(), "common.count", items.size(), total));
    }
}
