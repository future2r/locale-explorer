package name.ulbricht.locale.explorer.ui;

/// Controller of a category view.
public interface CategoryController {

    /// Removes the listeners the controller registered on long-lived objects.
    ///
    /// Called before the view is replaced.
    void dispose();
}
