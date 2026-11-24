package name.ulbricht.locale.explorer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.NodeOrientation;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;

import name.ulbricht.locale.explorer.i18n.Messages;
import name.ulbricht.locale.explorer.model.AppState;
import name.ulbricht.locale.explorer.model.PreferencesStore;
import name.ulbricht.locale.explorer.ui.LiveClock;
import name.ulbricht.locale.explorer.ui.MainController;
import name.ulbricht.locale.explorer.ui.ThemeManager;

/// Entry point of the Locale Explorer.
///
/// Builds the main window from FXML and rebuilds it whenever the UI language changes. The
/// [AppState] outlives these rebuilds.
public final class LocaleExplorerApp extends Application {

    private static final double WINDOW_WIDTH = 1100;
    private static final double WINDOW_HEIGHT = 720;
    private static final List<Integer> ICON_SIZES = List.of(16, 24, 32, 48, 64, 128, 256);

    private PreferencesStore store;
    private AppState state;
    private LiveClock liveClock;
    private Stage stage;
    private Scene scene;
    private ThemeManager themeManager;
    private MainController controller;

    /// Creates the application; JavaFX calls this before [#init()].
    public LocaleExplorerApp() {
        // all set-up happens in init() and start(Stage)
    }

    /// Starts the application.
    ///
    /// @param args the command line arguments
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() {
        store = new PreferencesStore();
        state = new AppState(store.load());
    }

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        liveClock = new LiveClock(state);

        // On small or highly scaled screens, leave room for the title bar and the taskbar.
        var screen = Screen.getPrimary().getVisualBounds();
        scene = new Scene(loadMainView(),
                Math.min(WINDOW_WIDTH, screen.getWidth() * 0.9), Math.min(WINDOW_HEIGHT, screen.getHeight() * 0.85));
        themeManager = new ThemeManager(scene, state);
        themeManager.apply();

        state.uiLanguageProperty().subscribe((_, _) -> rebuild());

        stage.setTitle(Messages.bundle(state.getUiLanguage()).getString("app.title"));
        stage.getIcons().setAll(icons());
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (liveClock != null) {
            liveClock.stop();
        }
        store.save(state.toSettings());
    }

    private void rebuild() {
        if (scene == null) {
            return;
        }
        scene.setRoot(loadMainView());
        themeManager.apply();
        stage.setTitle(Messages.bundle(state.getUiLanguage()).getString("app.title"));
    }

    /// Loads the application icon in all sizes; the platform picks the best one for the title bar
    /// and the taskbar.
    private static List<Image> icons() {
        return ICON_SIZES.stream()
                .map(size -> MainController.class.getResource("icons/app-" + size + ".png"))
                .filter(Objects::nonNull)
                .map(url -> new Image(url.toExternalForm()))
                .toList();
    }

    private Parent loadMainView() {
        var previous = controller;
        var language = state.getUiLanguage();
        var loader = new FXMLLoader(MainController.class.getResource("main.fxml"), Messages.bundle(language));
        loader.setControllerFactory(_ -> new MainController(state, store));
        try {
            Parent root = loader.load();
            root.setNodeOrientation(language.isRightToLeft()
                    ? NodeOrientation.RIGHT_TO_LEFT
                    : NodeOrientation.LEFT_TO_RIGHT);
            controller = loader.getController();
            // release the old view only once the new one has loaded
            if (previous != null) {
                previous.dispose();
            }
            return root;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load main view", e);
        }
    }
}
