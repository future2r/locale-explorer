package name.ulbricht.locale.explorer.ui;

import java.util.Locale;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import name.ulbricht.locale.explorer.text.CodePoints;
import name.ulbricht.locale.explorer.text.CodePoints.Mark;

/// Renders a text with its invisible and look-alike characters highlighted in place.
///
/// The text keeps its original characters, so it reads and lays out like the plain value:
///
/// - spaces get a colored background in their real width, one color per kind
/// - zero-width direction marks are drawn as a thin bar
/// - the minus sign U+2212 is colored
///
/// The colors come from the style classes `mark-…` in the theme style sheets.
final class MarkedText {

    private MarkedText() {
    }

    /// Creates the node for a text.
    ///
    /// @param text the text
    /// @return a text flow with one child per segment
    static TextFlow of(String text) {
        var flow = new SingleLineFlow();
        flow.getStyleClass().add("marked-text");
        for (var segment : CodePoints.segments(text)) {
            var mark = segment.mark();
            if (mark == null) {
                flow.getChildren().add(plain(segment.text()));
            } else if (mark == Mark.DIRECTION_MARK) {
                // Keep the character for the bidi algorithm, and draw a bar next to it.
                flow.getChildren().addAll(plain(segment.text()), bar());
            } else if (mark == Mark.MINUS_SIGN) {
                var minus = plain(segment.text());
                minus.getStyleClass().add(styleClass(mark));
                flow.getChildren().add(minus);
            } else {
                flow.getChildren().add(space(segment.text(), mark));
            }
        }
        return flow;
    }

    /// Returns the style class for a kind of special character, for example `mark-no-break-space`.
    ///
    /// @param mark the kind
    /// @return the style class
    static String styleClass(Mark mark) {
        return "mark-" + mark.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /// A text flow that always stays on a single line.
    ///
    /// A table cell asks its graphic for the height at the width left over beside the (empty)
    /// text, which can be zero; a normal text flow would then wrap after every character. In a
    /// column narrower than the text it would wrap as well and draw over the next row. This flow
    /// is never laid out narrower than its text; the cell clips what does not fit.
    private static final class SingleLineFlow extends TextFlow {

        @Override
        public Orientation getContentBias() {
            return null;
        }

        @Override
        protected double computeMinHeight(double width) {
            return super.computePrefHeight(-1);
        }

        @Override
        protected double computePrefHeight(double width) {
            return super.computePrefHeight(-1);
        }

        @Override
        public void resize(double width, double height) {
            super.resize(Math.max(width, prefWidth(-1)), height);
        }
    }

    private static Text plain(String text) {
        var node = new Text(text);
        node.getStyleClass().add("marked-text-plain");
        return node;
    }

    private static Node space(String text, Mark mark) {
        var label = new Label(text);
        label.getStyleClass().addAll("mark-space", styleClass(mark));
        return label;
    }

    private static Node bar() {
        var bar = new Region();
        bar.getStyleClass().addAll("mark-bar", styleClass(Mark.DIRECTION_MARK));
        return bar;
    }
}
