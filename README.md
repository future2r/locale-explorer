# Locale Explorer

A desktop application for exploring how Java localizes dates, times, numbers, currencies,
lists and other text. Pick a locale (and optionally a few more to compare against), a time
zone and some sample values, and see how the JDK formats them, side by side.

> **Status:** all nine categories, the table export (Markdown, CSV) and packaging as a
> self-contained application work. Still open: an installer (needs the WiX Toolset) and a review
> of the Arabic and Chinese translations.

## Features

| Area | What is shown |
|---|---|
| **Locale** | BCP 47 tag, language, script, country, variant, display names, ISO 3 codes, Unicode extensions (`-u-ca`, `-u-nu`, `-u-fw`, `-u-rg`, `-u-cu`, `-u-cf`, `-u-tz`, and `-u-hc`, which the JDK ignores), `Locale.lookup` / `Locale.filter` against an Accept-Language string |
| **Date and time** | `DateTimeFormatter.ofLocalized…` in `SHORT`, `MEDIUM`, `LONG` and `FULL` with the resulting patterns, `ofLocalizedPattern` skeletons, month and weekday names (full, short, narrow, standalone), eras, AM/PM, day periods, legacy `SimpleDateFormat` for comparison |
| **Calendar and week** | `WeekFields` (first day of week, minimal days, week numbers), chronologies (ISO, Japanese, Thai Buddhist, Hijrah, Minguo) |
| **Time zones** | `ZoneId`, offsets, zone names in the `FULL`, `SHORT` and `NARROW` text styles and as pattern letters, upcoming DST transitions, the same instant in several zones |
| **Numbers** | Number, integer and percent formats, `CompactNumberFormat` (short and long), all `DecimalFormatSymbols`, alternative numbering systems, parsing |
| **Currency** | The locale's currency, symbol vs. code, display name, fraction digits, accounting format |
| **Lists and text** | `ListFormat` (standard, or, unit × full, short, narrow), `MessageFormat` and `ChoiceFormat`, `String.format`, locale-sensitive case mapping |
| **Sorting and text boundaries** | `Collator` with strengths and sample words, `BreakIterator` (characters, words, sentences, lines), `Normalizer` |
| **System** | Default locales per category, default time zone, locale providers, tzdata version (the CLDR version has no public API), number of available locales, an experiment that changes the JVM defaults |

Each result can be compared across several locales in separate columns. A collapsible panel
shows the Java code that produced the selected result, and the current table can be exported
as Markdown or CSV.

### User interface

- UI language: German, English, French, Spanish, Simplified Chinese and Arabic, switchable
  at runtime. This is independent of the locale being examined. Arabic mirrors the layout
  (right-to-left); code, locale tags, zone IDs and patterns stay left-to-right.
- Result cells use the text direction of the examined locale, not of the UI language.
- Light and dark theme, or follow the operating system setting.
- Language, theme and font size live in a drop-down settings panel.
- The sample instant is picked with a date picker and a time field; "Now" takes the current
  time once, "Live" keeps updating it every second.
- Scripts the default UI font cannot display on Windows (Ethiopic, Tifinagh, Adlam, Yi, …)
  are shown with a matching system font such as Ebrima or Microsoft Yi Baiti.
- Number formats can be tuned: minimum/maximum fraction digits, minimum integer digits,
  grouping, rounding mode and custom `DecimalFormat` patterns.

## Requirements

- JDK 27, registered in `~/.m2/toolchains.xml` (version `27`, vendor `openjdk`)
- Maven itself must also run on JDK 27 (`JAVA_HOME`): the JAR and JavaFX plugins read the
  Java 27 module descriptor inside Maven's own JVM. The build checks this and stops with a
  message otherwise.
- No local Maven installation needed; the Maven Wrapper is included.

## Build, test and run

```shell
./mvnw verify          # compile and run the tests
./mvnw javafx:run      # start the application
```

On Windows use `mvnw.cmd` instead of `./mvnw`.

## Packaging

```shell
./mvnw -Ppackage clean package
```

This links a runtime image with `jlink` (`target/locale-explorer`) and turns it into a
self-contained application in `target/dist` (about 125 MB). Start it with
`Locale Explorer/Locale Explorer.exe` on Windows, `Locale Explorer.app` on macOS or
`Locale Explorer/bin/Locale Explorer` on Linux; no Java installation is needed. The folder can
be copied or zipped as it is. `clean` is optional: the copied JavaFX jars and the old images
are removed before linking. jlink and jpackage are taken from the JDK 27 toolchain.

The JavaFX platform jars and the icon are chosen by OS profiles: Windows x64, Linux x64 and
ARM64, macOS x64 and ARM64. On other platforms the package profile stops with a message.
On macOS the application version is 1.0.0, because jpackage rejects versions starting with 0.
Packaging has only been tried on Windows so far.

The runtime image explicitly includes `jdk.localedata`. Without it, `jlink` links only the
modules the code depends on directly, and the JDK knows just a handful of English locales.
`jdk.charsets` adds the less common charsets such as Big5, EUC-JP, ISO-2022-JP and the EBCDIC
code pages, so that the application also runs on systems with such a native encoding.

A Windows installer (`--type msi` or `exe`) additionally requires the
[WiX Toolset](https://wixtoolset.org/).

## Technology

- Java 27, JavaFX 27, FXML with controllers
- Current Java language features where they help; Markdown documentation comments (`///`)
- Java Platform Module System (`module-info.java`)
- Maven Wrapper; `javafx-maven-plugin` for running; `maven-dependency-plugin` and
  `exec-maven-plugin` (jlink, jpackage) for packaging; `maven-enforcer-plugin` for the JDK check
- Locale data: CLDR as bundled with the JDK
- No runtime dependencies besides JavaFX

## Coordinates

- groupId: `name.ulbricht`
- artifactId: `locale-explorer`
- base package: `name.ulbricht.locale.explorer`

## Application icon

The icon is drawn by `tools/IconGenerator.java`: the window icons
(`src/main/resources/.../ui/icons/app-*.png`) and the jpackage icons for Windows (`.ico`),
macOS (`.icns`) and Linux (`.png`) in `src/packaging`. To regenerate them:

```shell
java tools/IconGenerator.java src/main/resources/name/ulbricht/locale/explorer/ui/icons src/packaging
```

The badge uses the font Segoe UI, so regenerate the icons on Windows; elsewhere the generator
stops instead of silently drawing with another font.
