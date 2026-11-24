package name.ulbricht.locale.explorer.export;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/// Converts tables to Markdown and CSV.
public final class TableExport {

    /// The E of an exponent between digits of any script, as in `1.235E6` or `१.२३५E-६`.
    private static final Pattern EXPONENT = Pattern.compile("(?<=\\p{Nd})[Ee](?=[+-]?\\p{Nd})");

    /// A letter of the Latin alphabet, as used in formula names.
    private static final Pattern LATIN_LETTER = Pattern.compile("[A-Za-z]");

    /// The contents of one table.
    ///
    /// @param headers the column headers
    /// @param rows    the rows; each row has one cell per header
    public record Table(List<String> headers, List<List<String>> rows) {

        public Table {
            headers = List.copyOf(headers);
            rows = rows.stream().map(List::copyOf).toList();
        }
    }

    /// The byte order mark written at the start of CSV files, so that spreadsheet programs
    /// recognize UTF-8.
    public static final String BYTE_ORDER_MARK = "﻿";

    private TableExport() {
    }

    /// Formats tables as Markdown.
    ///
    /// @param title  the heading of the document
    /// @param tables the tables, separated by a blank line
    /// @return the Markdown text
    public static String markdown(String title, List<Table> tables) {
        var text = new StringBuilder("# ").append(singleLine(title)).append("\n");
        for (var table : tables) {
            text.append('\n');
            text.append(markdownRow(table.headers()));
            text.append(table.headers().stream().map(_ -> "---").collect(Collectors.joining("|", "|", "|\n")));
            table.rows().forEach(row -> text.append(markdownRow(row)));
        }
        return text.toString();
    }

    private static String markdownRow(List<String> cells) {
        return cells.stream()
                .map(cell -> singleLine(cell).replace("|", "\\|"))
                .collect(Collectors.joining(" | ", "| ", " |\n"));
    }

    /// Formats tables as CSV according to RFC 4180, with a comma as separator.
    ///
    /// Several tables are separated by an empty line.
    ///
    /// @param tables the tables
    /// @return the CSV text, without byte order mark
    public static String csv(List<Table> tables) {
        var text = new StringBuilder();
        for (var i = 0; i < tables.size(); i++) {
            if (i > 0) {
                text.append("\r\n");
            }
            var table = tables.get(i);
            text.append(csvRow(table.headers()));
            table.rows().forEach(row -> text.append(csvRow(row)));
        }
        return text.toString();
    }

    private static String csvRow(List<String> cells) {
        return cells.stream().map(TableExport::csvCell).collect(Collectors.joining(",", "", "\r\n"));
    }

    private static String csvCell(String cell) {
        var value = neutralizeFormula(cell == null ? "" : cell);
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /// Keeps spreadsheet programs from reading a cell as a formula.
    ///
    /// Cells starting with `=`, `@`, a tab or a carriage return get a leading apostrophe, and so
    /// do cells starting with `+` or `-` that contain Latin letters, such as `-u-nu-arab`, as most
    /// spreadsheet programs use Latin function names. Formatted numbers stay as they are, also in
    /// other scripts and with an exponent such as `-1.235E6` or `-१.२३५E-६`. Function names of
    /// localized spreadsheet programs in other scripts, such as Russian, are not detected.
    static String neutralizeFormula(String value) {
        if (value.isEmpty()) {
            return value;
        }
        var formula = switch (value.charAt(0)) {
            case '=', '@', '\t', '\r' -> true;
            case '+', '-' -> LATIN_LETTER.matcher(EXPONENT.matcher(value.substring(1)).replaceAll("")).find();
            default -> false;
        };
        return formula ? "'" + value : value;
    }

    private static String singleLine(String text) {
        return text == null ? "" : text.replace("\r", "").replace("\n", " ⏎ ");
    }
}
