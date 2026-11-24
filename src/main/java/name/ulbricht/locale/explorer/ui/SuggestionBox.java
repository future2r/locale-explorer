package name.ulbricht.locale.explorer.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/// Turns an editable [ComboBox] into an input field with suggestions and a history of recent
/// entries.
///
/// The drop-down lists the recent entries first, then the suggestions, each with a short
/// explanation. The history is kept in the preferences.
///
/// A value is committed when the user enters or picks it (see [ChoiceHandler]), not while
/// browsing the list. Values set by the program with [#setValue] are evaluated too, but never
/// added to the history.
final class SuggestionBox {

    private final ComboBox<String> box;
    private final CategoryContext context;
    private final String fieldId;
    private final Map<String, String> descriptions = new LinkedHashMap<>();
    private Consumer<String> onCommit;
    private List<String> history;
    private boolean programmatic;
    private boolean muted;

    private SuggestionBox(ComboBox<String> box, CategoryContext context, String fieldId,
                          List<Suggestion> suggestions) {
        this.box = box;
        this.context = context;
        this.fieldId = fieldId;
        putDescriptions(suggestions);
        this.history = context.store().loadHistory(fieldId);
    }

    /// Installs suggestions and history on a combo box.
    ///
    /// @param box         the combo box; it is made editable
    /// @param context     the category context
    /// @param fieldId     a stable identifier for the history
    /// @param suggestions the suggestions
    /// @param onCommit    called with the entered or chosen value
    /// @return the installed helper, used to set values and add them to the history
    static SuggestionBox install(ComboBox<String> box, CategoryContext context, String fieldId,
                                 List<Suggestion> suggestions, Consumer<String> onCommit) {
        var suggestionBox = new SuggestionBox(box, context, fieldId, suggestions);
        suggestionBox.onCommit = onCommit;
        box.setEditable(true);
        box.getEditor().textProperty().subscribe(text -> Controls.applyScriptFont(box.getEditor(), text));
        box.setCellFactory(_ -> suggestionBox.new Cell());
        suggestionBox.updateItems();
        ChoiceHandler.install(box, suggestionBox::commit);
        return suggestionBox;
    }

    /// Sets a value from the program, for example a default, and evaluates it once.
    ///
    /// During this evaluation [#isUserCommit] is `false` and [#remember] does nothing.
    ///
    /// @param value the value
    void setValue(String value) {
        programmatic = true;
        try {
            setSilently(value);
            onCommit.accept(value);
        } finally {
            programmatic = false;
        }
    }

    /// Shows a value from the program without evaluating it, for callers that evaluate all
    /// their inputs together right afterwards.
    ///
    /// @param value the value
    void show(String value) {
        setSilently(value);
    }

    /// Tells whether the current commit was made by the user, not by [#setValue].
    ///
    /// @return `true` for values entered or chosen by the user
    boolean isUserCommit() {
        return !programmatic;
    }

    /// Replaces the suggestions, for example when they depend on other input.
    ///
    /// @param suggestions the new suggestions
    void setSuggestions(List<Suggestion> suggestions) {
        descriptions.clear();
        putDescriptions(suggestions);
        var current = box.getValue();
        updateItems();
        setSilently(current);
    }

    /// Adds a value to the history, typically after it was evaluated successfully.
    ///
    /// Values set with [#setValue] are not added.
    ///
    /// @param value the value
    void remember(String value) {
        if (programmatic || value == null || value.isBlank()
                || (!history.isEmpty() && history.getFirst().equals(value))) {
            return;
        }
        history = context.store().addHistory(fieldId, value);
        var current = box.getValue();
        updateItems();
        setSilently(current);
    }

    private void commit(String value) {
        if (muted || value == null) {
            return;
        }
        onCommit.accept(value);
    }

    private void setSilently(String value) {
        var wasMuted = muted;
        muted = true;
        try {
            box.setValue(value);
        } finally {
            muted = wasMuted;
        }
    }

    private void putDescriptions(List<Suggestion> suggestions) {
        for (var suggestion : suggestions) {
            var key = suggestion.descriptionKey();
            descriptions.put(suggestion.value(), key == null ? "" : context.resources().getString(key));
        }
    }

    private void updateItems() {
        var items = new ArrayList<String>(history);
        descriptions.keySet().stream().filter(value -> !items.contains(value)).forEach(items::add);
        box.getItems().setAll(items);
    }

    private String description(String item) {
        var description = descriptions.getOrDefault(item, "");
        if (description.isEmpty() && history.contains(item)) {
            return context.resources().getString("suggestion.recent");
        }
        return description;
    }

    private final class Cell extends ListCell<String> {

        private final Label valueLabel = new Label();
        private final Label descriptionLabel = new Label();
        private final HBox content;

        Cell() {
            valueLabel.getStyleClass().add("mono");
            descriptionLabel.getStyleClass().add("suggestion-description");
            var spacer = new Region();
            spacer.setMinWidth(16);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            content = new HBox(valueLabel, spacer, descriptionLabel);
            content.getStyleClass().add("suggestion-cell");
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            valueLabel.setText(item);
            descriptionLabel.setText(description(item));
            Controls.applyScriptFont(valueLabel, item);
            setGraphic(content);
        }
    }
}
