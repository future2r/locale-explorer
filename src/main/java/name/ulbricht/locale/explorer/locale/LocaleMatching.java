package name.ulbricht.locale.explorer.locale;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Locale.FilteringMode;
import java.util.Locale.LanguageRange;
import java.util.Optional;

/// Matches a priority list such as an HTTP `Accept-Language` header against candidate locales.
public final class LocaleMatching {

    /// The outcome of a match.
    ///
    /// @param ranges   the parsed priority list
    /// @param lookup   the result of [Locale#lookup(List, Collection)]
    /// @param filtered the result of [Locale#filter(List, Collection, FilteringMode)]
    public record Result(List<LanguageRange> ranges, Optional<Locale> lookup, List<Locale> filtered) {
    }

    private LocaleMatching() {
    }

    /// Parses the priority list and matches it.
    ///
    /// @param priorityList the priority list, for example `"fr-CH, fr;q=0.9, en;q=0.8"`
    /// @param candidates   the locales to choose from
    /// @param mode         the filtering mode for [Locale#filter(List, Collection, FilteringMode)]
    /// @return the result
    /// @throws IllegalArgumentException if the priority list is malformed or the mode does not
    ///                                  accept it
    public static Result match(String priorityList, Collection<Locale> candidates, FilteringMode mode) {
        var ranges = LanguageRange.parse(priorityList);
        var lookup = Optional.ofNullable(Locale.lookup(ranges, candidates));
        var filtered = Locale.filter(ranges, candidates, mode);
        return new Result(ranges, lookup, filtered);
    }
}
