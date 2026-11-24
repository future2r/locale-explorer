package name.ulbricht.locale.explorer.ui;

import java.util.Set;
import java.util.function.Function;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.text.Font;
import javafx.util.StringConverter;

import name.ulbricht.locale.explorer.text.ScriptFonts;

/// Small helpers shared by the controllers.
final class Controls {

    private static final ScriptFonts SCRIPT_FONTS = ScriptFonts.forOperatingSystem(System.getProperty("os.name"));

    private static Set<String> installedFamilies;

    private Controls() {
    }

    /// Copies a text to the system clipboard; empty texts are ignored.
    ///
    /// @param text the text, may be `null`
    static void copyToClipboard(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        var content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }

    /// Shows an error message below an input, or hides the label if there is none.
    ///
    /// @param label   the error label
    /// @param message the message, or `null` to hide the label
    static void showError(Label label, String message) {
        var visible = message != null;
        label.setText(visible ? message : "");
        label.setVisible(visible);
        label.setManaged(visible);
    }

    /// Creates a converter for non-editable choice boxes and combo boxes.
    ///
    /// @param toText turns a value into its display text
    /// @param <T>    the value type
    /// @return the converter; `fromString` is not supported
    static <T> StringConverter<T> readOnly(Function<T, String> toText) {
        return new StringConverter<>() {
            @Override
            public String toString(T value) {
                return value == null ? "" : toText.apply(value);
            }

            @Override
            public T fromString(String text) {
                throw new UnsupportedOperationException();
            }
        };
    }

    /// Sets up a spinner for whole numbers whose text field only accepts digits.
    ///
    /// Unlike the default converter, an empty or out-of-range entry never throws or produces a
    /// `null` value: it is clamped to the range or falls back to the current value, and the
    /// field shows the value again when it loses the focus.
    ///
    /// @param spinner the spinner
    /// @param min     the smallest value
    /// @param max     the largest value
    /// @param initial the initial value
    static void integerSpinner(Spinner<Integer> spinner, int min, int max, int initial) {
        var factory = new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, initial);
        factory.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer value) {
                return value == null ? "" : value.toString();
            }

            @Override
            public Integer fromString(String text) {
                try {
                    return Math.clamp(Long.parseLong(text.strip()), min, max);
                } catch (NumberFormatException _) {
                    return factory.getValue();
                }
            }
        });
        spinner.setValueFactory(factory);
        var editor = spinner.getEditor();
        editor.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().matches("\\d{0,10}") ? change : null));
        editor.focusedProperty().subscribe(focused -> {
            if (!focused) {
                // after the spinner has committed the text
                Platform.runLater(() -> editor.setText(factory.getConverter().toString(factory.getValue())));
            }
        });
    }

    /// Sets a font family that can display the text, if the default font cannot.
    ///
    /// Only the family is set, so the font size still follows the settings. The style also
    /// reaches popups owned by the node, such as the calendar of a date picker.
    ///
    /// @param node the node showing the text
    /// @param text the text, may be `null`
    static void applyScriptFont(Node node, String text) {
        var style = scriptFontStyle(text);
        if (!style.equals(node.getStyle())) {
            node.setStyle(style);
        }
    }

    /// Shows a text as tooltip of a control, reusing its tooltip.
    ///
    /// Table cells are updated often, every second in live mode; replacing the tooltip each time
    /// would hide a tooltip the user is just reading.
    ///
    /// @param control the control, typically a table cell
    /// @param text    the text, or `null` for no tooltip
    static void showTooltip(Control control, String text) {
        var tooltip = control.getTooltip();
        if (text == null) {
            control.setTooltip(null);
        } else if (tooltip == null) {
            control.setTooltip(tooltip(text));
        } else if (!text.equals(tooltip.getText())) {
            tooltip.setText(text);
            tooltip.setStyle(scriptFontStyle(text));
        }
    }

    /// Creates a tooltip whose font can display the text.
    ///
    /// Tooltips are shown in their own window and do not inherit the font of their node.
    ///
    /// @param text the text
    /// @return the tooltip
    static Tooltip tooltip(String text) {
        var tooltip = new Tooltip(text);
        tooltip.setStyle(scriptFontStyle(text));
        return tooltip;
    }

    private static String scriptFontStyle(String text) {
        return SCRIPT_FONTS.familyFor(text, Controls::isInstalled)
                .map("-fx-font-family: \"%s\";"::formatted)
                .orElse("");
    }

    private static boolean isInstalled(String family) {
        if (installedFamilies == null) {
            installedFamilies = Set.copyOf(Font.getFamilies());
        }
        return installedFamilies.contains(family);
    }
}
