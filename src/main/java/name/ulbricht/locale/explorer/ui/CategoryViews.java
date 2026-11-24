package name.ulbricht.locale.explorer.ui;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

import name.ulbricht.locale.explorer.model.Category;

/// Loads the view of a category from its FXML file.
final class CategoryViews {

    /// A loaded category view.
    ///
    /// @param node       the root node
    /// @param controller the controller
    record View(Node node, CategoryController controller) {
    }

    private record Definition(String fxml, Function<CategoryContext, CategoryController> factory) {
    }

    private static final Map<Category, Definition> DEFINITIONS = Map.of(
            Category.LOCALE, new Definition("category/locale.fxml", LocaleCategoryController::new),
            Category.DATE_TIME, new Definition("category/datetime.fxml", DateTimeCategoryController::new),
            Category.CALENDAR, new Definition("category/calendar.fxml", CalendarCategoryController::new),
            Category.TIME_ZONES, new Definition("category/timezones.fxml", TimeZoneCategoryController::new),
            Category.NUMBERS, new Definition("category/numbers.fxml", NumbersCategoryController::new),
            Category.CURRENCY, new Definition("category/currency.fxml", CurrencyCategoryController::new),
            Category.LISTS_TEXT, new Definition("category/lists.fxml", ListsCategoryController::new),
            Category.SORTING, new Definition("category/sorting.fxml", SortingCategoryController::new),
            Category.SYSTEM, new Definition("category/system.fxml", SystemCategoryController::new));

    private CategoryViews() {
    }

    /// Loads the view of a category.
    ///
    /// @param category the category
    /// @param context  the context passed to the controller
    /// @return the view, or empty if the category is not implemented yet
    static Optional<View> load(Category category, CategoryContext context) {
        var definition = DEFINITIONS.get(category);
        if (definition == null) {
            return Optional.empty();
        }
        var loader = new FXMLLoader(CategoryViews.class.getResource(definition.fxml()), context.resources());
        loader.setControllerFactory(_ -> definition.factory().apply(context));
        try {
            Node node = loader.load();
            return Optional.of(new View(node, loader.getController()));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load " + definition.fxml(), e);
        }
    }
}
