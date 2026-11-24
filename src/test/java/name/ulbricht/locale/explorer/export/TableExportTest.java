package name.ulbricht.locale.explorer.export;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class TableExportTest {

    private static final TableExport.Table TABLE = new TableExport.Table(
            List.of("Format", "de-DE"),
            List.of(List.of("Zahl", "1.234,5"), List.of("Muster", "#,##0.00;(a|b)"), List.of("Code", "a\nb \"c\"")));

    @Test
    void markdownEscapesPipesAndLineBreaks() {
        assertEquals("""
                # Zahlen · Formate

                | Format | de-DE |
                |---|---|
                | Zahl | 1.234,5 |
                | Muster | #,##0.00;(a\\|b) |
                | Code | a ⏎ b "c" |
                """, TableExport.markdown("Zahlen · Formate", List.of(TABLE)));
    }

    @Test
    void csvQuotesWhereNeeded() {
        assertEquals("Format,de-DE\r\nZahl,\"1.234,5\"\r\nMuster,\"#,##0.00;(a|b)\"\r\nCode,\"a\nb \"\"c\"\"\"\r\n",
                TableExport.csv(List.of(TABLE)));
    }

    @Test
    void severalTablesAreSeparated() {
        var small = new TableExport.Table(List.of("A"), List.of(List.of("1")));
        assertEquals("A\r\n1\r\n\r\nA\r\n1\r\n", TableExport.csv(List.of(small, small)));
        assertEquals("# T\n\n| A |\n|---|\n| 1 |\n\n| A |\n|---|\n| 1 |\n", TableExport.markdown("T", List.of(small, small)));
    }

    @Test
    void csvKeepsSpreadsheetsFromReadingFormulas() {
        assertEquals("'= Apfel", TableExport.neutralizeFormula("= Apfel"));
        assertEquals("'-u-nu-arab", TableExport.neutralizeFormula("-u-nu-arab"));
        assertEquals("'@SUM(A1)", TableExport.neutralizeFormula("@SUM(A1)"));
        assertEquals("-1,234.5", TableExport.neutralizeFormula("-1,234.5"));
        assertEquals("+49", TableExport.neutralizeFormula("+49"));
        assertEquals("", TableExport.neutralizeFormula(""));
        assertEquals("-1.235E6", TableExport.neutralizeFormula("-1.235E6"));
        assertEquals("-1,23457E-6", TableExport.neutralizeFormula("-1,23457E-6"));
        assertEquals("'\t=1+1", TableExport.neutralizeFormula("\t=1+1"));
        assertEquals("'-1 Mio.", TableExport.neutralizeFormula("-1 Mio."));
        assertEquals("-१.२E-६", TableExport.neutralizeFormula("-१.२E-६"));
        assertEquals("-1,235Е-6", TableExport.neutralizeFormula("-1,235Е-6"));
    }
}
