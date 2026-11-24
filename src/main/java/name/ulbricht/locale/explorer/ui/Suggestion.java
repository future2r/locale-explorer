package name.ulbricht.locale.explorer.ui;

/// A suggested value for an input field, with a short explanation.
///
/// @param value          the value inserted into the field
/// @param descriptionKey the message key of the explanation, or `null`
record Suggestion(String value, String descriptionKey) {
}
