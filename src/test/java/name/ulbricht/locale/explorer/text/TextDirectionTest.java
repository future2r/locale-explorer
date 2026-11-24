package name.ulbricht.locale.explorer.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TextDirectionTest {

    @ParameterizedTest
    @CsvSource({
            "de-DE, false",
            "en-US, false",
            "zh-Hans-CN, false",
            "ar, true",
            "ar-EG, true",
            "he-IL, true",
            "fa-IR, true",
            "ur-PK, true",
            "pa-Arab-PK, true",
            "pa-Guru-IN, false",
            "sd-Deva-IN, false",
            "az-Arab, true",
            "pa-PK, true",
            "pa-IN, false",
            "sd-PK, true",
            "sd-IN, false",
            "uz-AF, true",
            "az-IR, true",
            "kk-CN, true",
            "ky-CN, true",
            "tk-AF, true",
            "tk-IR, true",
            "az-IQ, true",
            "ms-CC, true",
            "ug-MN, false",
            "ug-KZ, false",
            "ug, true",
            "az-AZ, false",
    })
    void detectsDirection(String tag, boolean rightToLeft) {
        assertEquals(rightToLeft, TextDirection.isRightToLeft(Locale.forLanguageTag(tag)), tag);
    }
}
