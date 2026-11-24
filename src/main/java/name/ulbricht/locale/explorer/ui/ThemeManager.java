package name.ulbricht.locale.explorer.ui;

import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.scene.Scene;

import name.ulbricht.locale.explorer.model.AppState;

/// Applies the color theme and font size to the scene.
///
/// With [name.ulbricht.locale.explorer.model.Theme#SYSTEM] the theme follows the color scheme
/// of the operating system, also when it changes while the application runs.
public final class ThemeManager {

    private static final String BASE_CSS = stylesheet("css/base.css");
    private static final String DARK_CSS = stylesheet("css/dark.css");

    private final Scene scene;
    private final AppState state;

    /// Creates the manager and re-applies the theme on every relevant change.
    ///
    /// @param scene the scene to style
    /// @param state the application state
    public ThemeManager(Scene scene, AppState state) {
        this.scene = scene;
        this.state = state;
        // invalidation subscriptions: they do not run at once, the owner calls apply() once
        state.themeProperty().subscribe(this::apply);
        state.fontSizeProperty().subscribe(this::apply);
        Platform.getPreferences().colorSchemeProperty().subscribe(this::apply);
    }

    /// Applies theme and font size to the current scene root.
    public void apply() {
        var dark = switch (state.getTheme()) {
            case LIGHT -> false;
            case DARK -> true;
            case SYSTEM -> Platform.getPreferences().getColorScheme() == ColorScheme.DARK;
        };
        if (dark) {
            scene.getStylesheets().setAll(BASE_CSS, DARK_CSS);
        } else {
            scene.getStylesheets().setAll(BASE_CSS);
        }
        if (scene.getRoot() != null) {
            scene.getRoot().setStyle("-fx-font-size: %dpx;".formatted(state.getFontSize()));
        }
    }

    private static String stylesheet(String name) {
        var url = ThemeManager.class.getResource(name);
        if (url == null) {
            throw new IllegalStateException("Missing stylesheet " + name);
        }
        return url.toExternalForm();
    }
}
