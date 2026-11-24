/// Locale Explorer: explores how Java localizes dates, times, numbers, currencies, lists and
/// text.
///
/// The only runtime dependency besides the JDK is JavaFX.
module name.ulbricht.locale.explorer {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.prefs;
    requires java.management;

    exports name.ulbricht.locale.explorer to javafx.graphics;

    opens name.ulbricht.locale.explorer.ui to javafx.fxml;
}
