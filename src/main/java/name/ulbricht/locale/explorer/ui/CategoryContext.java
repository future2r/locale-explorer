package name.ulbricht.locale.explorer.ui;

import java.util.ResourceBundle;
import java.util.function.Consumer;

import name.ulbricht.locale.explorer.model.AppState;
import name.ulbricht.locale.explorer.model.PreferencesStore;

/// Everything a category view needs from its surroundings.
///
/// @param state     the application state
/// @param store     the preferences store, for input histories
/// @param resources the translated texts
/// @param codeSink  receives the Java code for the selected result, or `null` to clear it
public record CategoryContext(AppState state, PreferencesStore store, ResourceBundle resources,
                              Consumer<String> codeSink) {
}
