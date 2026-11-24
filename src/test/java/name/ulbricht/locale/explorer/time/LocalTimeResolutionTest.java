package name.ulbricht.locale.explorer.time;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class LocalTimeResolutionTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    @Test
    void normalTime() {
        var resolution = LocalTimeResolution.of(LocalDateTime.of(2027, 6, 15, 12, 0), BERLIN);
        assertEquals(LocalTimeResolution.Kind.NORMAL, resolution.kind());
        assertEquals(ZoneOffset.ofHours(2), resolution.offset());
    }

    @Test
    void gapIsMovedForward() {
        var resolution = LocalTimeResolution.of(LocalDateTime.of(2027, 3, 28, 2, 30), BERLIN);
        assertEquals(LocalTimeResolution.Kind.GAP, resolution.kind());
        assertEquals(LocalDateTime.of(2027, 3, 28, 3, 30), resolution.resolved().toLocalDateTime());
        assertEquals(ZoneOffset.ofHours(2), resolution.offset());
    }

    @Test
    void overlapUsesEarlierOffset() {
        var resolution = LocalTimeResolution.of(LocalDateTime.of(2027, 10, 31, 2, 30), BERLIN);
        assertEquals(LocalTimeResolution.Kind.OVERLAP, resolution.kind());
        assertEquals(ZoneOffset.ofHours(2), resolution.offset());
    }
}
