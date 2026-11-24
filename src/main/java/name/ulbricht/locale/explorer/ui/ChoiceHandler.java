package name.ulbricht.locale.explorer.ui;

import java.util.Objects;
import java.util.function.Consumer;

import javafx.scene.control.ComboBox;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/// Calls an action when the user has chosen a value in a combo box, not while browsing.
///
/// A plain `onAction` fires for every value change, including each arrow key press while
/// browsing the list. Here the action runs once per chosen value:
///
/// - Enter with the list closed takes over the typed text (editable boxes) or the value reached
///   with the arrow keys, and runs the action even if the value did not change;
/// - picking an item with the mouse or closing the drop-down takes over the shown value,
///   except when it is closed with Escape;
/// - leaving the box after only browsing restores the last chosen value;
/// - values set by the program while the user is not browsing are taken over at once.
///
/// Except on Enter with the list closed, the action is not called again for the value it was
/// called with last. In the open list, Enter picks the item like a mouse click.
///
/// @param <T> the value type
final class ChoiceHandler<T> {

    private final ComboBox<T> box;
    private final Consumer<T> action;
    private T chosen;
    private boolean browsing;
    private boolean pending;
    private boolean suppressed;
    private boolean cancelled;

    private ChoiceHandler(ComboBox<T> box, Consumer<T> action) {
        this.box = box;
        this.action = action;
        this.chosen = box.getValue();
    }

    /// Installs the handler; the `onHidden` handler of the box is replaced.
    ///
    /// The handler follows the value property, not `onAction`: the action event is fired by the
    /// skin, which does not exist yet while a view is initialized.
    ///
    /// @param box    the combo box
    /// @param action called with the chosen value, which may be `null`
    /// @param <T>    the value type
    static <T> void install(ComboBox<T> box, Consumer<T> action) {
        var handler = new ChoiceHandler<>(box, action);
        box.addEventFilter(KeyEvent.KEY_PRESSED, handler::keyPressed);
        box.addEventFilter(KeyEvent.KEY_RELEASED, _ -> handler.browsing = false);
        box.valueProperty().subscribe((_, _) -> handler.valueChanged());
        box.setOnHidden(_ -> handler.popupHidden());
        box.focusedProperty().subscribe(focused -> {
            if (!focused) {
                handler.restoreIfOnlyBrowsed();
            }
        });
        // Escape in the open list closes it before the box sees the key; catch it in the list.
        box.skinProperty().subscribe(skin -> {
            if (skin instanceof ComboBoxListViewSkin<?> listSkin) {
                listSkin.getPopupContent().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        handler.cancelled = true;
                    }
                });
            }
        });
    }

    private void popupHidden() {
        if (cancelled) {
            // Escape: forget what was browsed in the list
            cancelled = false;
            restoreIfOnlyBrowsed();
            return;
        }
        pending = false;
        choose(box.getValue());
    }

    private void keyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case UP, DOWN, PAGE_UP, PAGE_DOWN, HOME, END -> browsing = true;
            case ENTER -> {
                if (box.isShowing()) {
                    return;
                }
                if (box.isEditable()) {
                    // take over the typed text now; the box would do it only on key release
                    suppressed = true;
                    try {
                        box.commitValue();
                    } finally {
                        suppressed = false;
                    }
                }
                pending = false;
                // Enter runs the action also for an unchanged value, to evaluate it again
                chosen = box.getValue();
                action.accept(chosen);
            }
            default -> {
                // other keys edit the text or are handled by the combo box
            }
        }
    }

    private void valueChanged() {
        if (suppressed) {
            return;
        }
        if (box.isShowing() || browsing) {
            pending = true;
        } else {
            pending = false;
            choose(box.getValue());
        }
    }

    private void restoreIfOnlyBrowsed() {
        if (!pending) {
            return;
        }
        pending = false;
        suppressed = true;
        try {
            box.setValue(chosen);
        } finally {
            suppressed = false;
        }
    }

    private void choose(T value) {
        if (Objects.equals(value, chosen)) {
            return;
        }
        chosen = value;
        action.accept(value);
    }
}
