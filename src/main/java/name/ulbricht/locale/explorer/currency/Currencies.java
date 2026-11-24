package name.ulbricht.locale.explorer.currency;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import name.ulbricht.locale.explorer.text.JavaCode;
import name.ulbricht.locale.explorer.text.LocaleProperty;

/// Rows for the currency category.
public final class Currencies {

    /// Value for `digits` in [#formatRows]: use the default fraction digits of the currency.
    public static final int CURRENCY_DIGITS = -1;

    /// Currencies with notable fraction digits or symbols, used for the symbol matrix.
    public static final List<String> SYMBOL_CURRENCIES =
            List.of("EUR", "USD", "CAD", "AUD", "GBP", "CHF", "JPY", "CNY", "INR", "EGP", "BHD");

    /// English locales whose currency symbols differ.
    public static final List<Locale> ENGLISH_VARIANTS = Stream.of("en-US", "en-GB", "en-CA", "en-AU", "en-IN", "en-CH")
            .map(Locale::forLanguageTag).toList();

    private Currencies() {
    }

    /// All currencies known to the JDK, sorted by code.
    ///
    /// @return the currencies
    public static List<Currency> all() {
        return Currency.getAvailableCurrencies().stream()
                .sorted(Comparator.comparing(Currency::getCurrencyCode))
                .toList();
    }

    // --- Formats -----------------------------------------------------------------------------

    /// Returns the rows of the formats tab.
    ///
    /// @param amount   the amount
    /// @param currency the currency to use, or `null` for the currency of each locale
    /// @param digits   the fraction digits to use, [#CURRENCY_DIGITS] for the default digits of the
    ///                 currency, or `null` to leave the format unchanged
    /// @return the rows
    public static List<LocaleProperty> formatRows(BigDecimal amount, Currency currency, Integer digits) {
        var setup = new ArrayList<String>();
        if (currency != null) {
            setup.add("format.setCurrency(Currency.getInstance(" + JavaCode.string(currency.getCurrencyCode()) + "));");
            setup.add("// setCurrency does not change the fraction digits.");
        }
        if (digits != null) {
            var expression = digits == CURRENCY_DIGITS
                    ? "format.getCurrency().getDefaultFractionDigits()"
                    : String.valueOf(digits);
            setup.add("format.setMinimumFractionDigits(" + expression + ");");
            setup.add("format.setMaximumFractionDigits(" + expression + ");");
        }
        var amountCode = "format.format(new BigDecimal(" + JavaCode.string(amount.toPlainString()) + "));";
        return List.of(
                LocaleProperty.of("currency.format.standard",
                        locale -> currencyFormat(locale, currency, digits).format(amount),
                        locale -> code(locale, "NumberFormat.getCurrencyInstance(locale)", setup, amountCode)),
                LocaleProperty.of("currency.format.accounting",
                        locale -> currencyFormat(accounting(locale), currency, digits).format(amount),
                        locale -> code(accounting(locale), "NumberFormat.getCurrencyInstance(locale)", setup, amountCode)),
                LocaleProperty.of("currency.format.isoCode",
                        locale -> isoCodeFormat(locale, currency, digits).format(amount),
                        locale -> {
                            var lines = new ArrayList<String>();
                            lines.add("var format = (DecimalFormat) NumberFormat.getCurrencyInstance(locale);");
                            lines.addAll(setup);
                            lines.add("format.applyPattern(format.toPattern().replace(\"\\u00A4\", \"\\u00A4\\u00A4\"));");
                            lines.add(amountCode);
                            return JavaCode.withLocale(locale, lines.toArray(String[]::new));
                        }),
                LocaleProperty.of("currency.format.used",
                        locale -> describe(currencyFormat(locale, currency, digits).getCurrency()),
                        locale -> code(locale, "NumberFormat.getCurrencyInstance(locale)", setup, "format.getCurrency();")));
    }

    private static NumberFormat currencyFormat(Locale locale, Currency currency, Integer digits) {
        var format = NumberFormat.getCurrencyInstance(locale);
        if (currency != null) {
            format.setCurrency(currency);
        }
        if (digits != null) {
            var count = digits == CURRENCY_DIGITS ? Math.max(0, format.getCurrency().getDefaultFractionDigits()) : digits;
            format.setMinimumFractionDigits(count);
            format.setMaximumFractionDigits(count);
        }
        return format;
    }

    private static NumberFormat isoCodeFormat(Locale locale, Currency currency, Integer digits) {
        var format = (DecimalFormat) currencyFormat(locale, currency, digits);
        // toPattern() already contains the fraction digits set above, so applyPattern keeps them.
        format.applyPattern(format.toPattern().replace("\u00A4", "\u00A4\u00A4"));
        return format;
    }

    private static Locale accounting(Locale locale) {
        return new Locale.Builder().setLocale(locale).setUnicodeLocaleKeyword("cf", "account").build();
    }

    private static String code(Locale locale, String factory, List<String> setup, String last) {
        var lines = new ArrayList<String>();
        lines.add("var format = " + factory + ";");
        lines.addAll(setup);
        lines.add(last);
        return JavaCode.withLocale(locale, lines.toArray(String[]::new));
    }

    private static String describe(Currency currency) {
        return currency == null ? "" : currency.getCurrencyCode();
    }

    // --- Currency of the locale --------------------------------------------------------------

    /// Returns the rows of the "currency of the locale" tab.
    ///
    /// @param uiLocale the locale of the user interface, for names
    /// @return the rows
    public static List<LocaleProperty> localeCurrencyRows(Locale uiLocale) {
        var ui = JavaCode.locale(uiLocale);
        return List.of(
                LocaleProperty.of("currency.locale.code", locale -> Currency.getInstance(locale).getCurrencyCode(),
                        locale -> JavaCode.withLocale(locale, "Currency.getInstance(locale).getCurrencyCode();")),
                LocaleProperty.of("currency.locale.numericCode",
                        locale -> Currency.getInstance(locale).getNumericCodeAsString(),
                        locale -> JavaCode.withLocale(locale, "Currency.getInstance(locale).getNumericCodeAsString();")),
                LocaleProperty.of("currency.locale.digits",
                        locale -> String.valueOf(Currency.getInstance(locale).getDefaultFractionDigits()),
                        locale -> JavaCode.withLocale(locale, "Currency.getInstance(locale).getDefaultFractionDigits();")),
                LocaleProperty.of("currency.locale.symbol", locale -> Currency.getInstance(locale).getSymbol(locale),
                        locale -> JavaCode.withLocale(locale, "Currency.getInstance(locale).getSymbol(locale);")),
                LocaleProperty.of("currency.locale.nameUi",
                        locale -> Currency.getInstance(locale).getDisplayName(uiLocale),
                        locale -> JavaCode.withLocale(locale, "Currency.getInstance(locale).getDisplayName(" + ui + ");")),
                LocaleProperty.of("currency.locale.nameOwn",
                        locale -> Currency.getInstance(locale).getDisplayName(locale),
                        locale -> JavaCode.withLocale(locale, "Currency.getInstance(locale).getDisplayName(locale);")),
                LocaleProperty.of("currency.locale.extensions", Currencies::extensions,
                        locale -> JavaCode.withLocale(locale,
                                "locale.getUnicodeLocaleType(\"cu\");",
                                "locale.getUnicodeLocaleType(\"rg\");")));
    }

    private static String extensions(Locale locale) {
        return Stream.of("cu", "rg")
                .filter(key -> locale.getUnicodeLocaleType(key) != null)
                .map(key -> key + "-" + locale.getUnicodeLocaleType(key))
                .collect(Collectors.joining(" · "));
    }

    // --- Symbols -----------------------------------------------------------------------------

    /// Returns one row per currency with its symbol in each locale.
    ///
    /// Highlighted where the symbol is the ISO code, that is, the locale has no own symbol.
    ///
    /// @param codes the currency codes
    /// @return the rows
    public static List<LocaleProperty> symbolRows(List<String> codes) {
        return codes.stream()
                .map(Currency::getInstance)
                .map(currency -> LocaleProperty.literal(currency.getCurrencyCode(),
                                locale -> currency.getSymbol(locale),
                                locale -> JavaCode.withLocale(locale,
                                        "Currency.getInstance(" + JavaCode.string(currency.getCurrencyCode()) + ").getSymbol(locale);"))
                        .withHighlight(locale -> currency.getSymbol(locale).equals(currency.getCurrencyCode())))
                .toList();
    }
}
