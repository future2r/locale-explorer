package name.ulbricht.locale.explorer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZoneId;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PreferencesStoreTest {

    private Preferences node;
    private PreferencesStore store;

    @BeforeEach
    void createNode() {
        node = Preferences.userRoot().node("name/ulbricht/locale-explorer-test");
        store = new PreferencesStore(node);
    }

    @AfterEach
    void removeNode() throws BackingStoreException {
        node.removeNode();
        node.flush();
    }

    @Test
    void worldZonesUseDefaultsUntilSaved() {
        var defaults = List.of(ZoneId.of("Europe/Berlin"));
        assertEquals(defaults, store.loadWorldZones(defaults));

        var zones = List.of(ZoneId.of("Asia/Kathmandu"), ZoneId.of("UTC"));
        store.saveWorldZones(zones);
        assertEquals(zones, store.loadWorldZones(defaults));

        store.saveWorldZones(List.of());
        assertEquals(List.of(), store.loadWorldZones(defaults));
    }

    @Test
    void unknownZonesAreSkipped() {
        node.put("worldZones", "Europe/Berlin,Mars/Olympus,UTC");
        assertEquals(List.of(ZoneId.of("Europe/Berlin"), ZoneId.of("UTC")), store.loadWorldZones(List.of()));
    }

    @Test
    void historyKeepsTheMostRecentEntriesFirst() {
        store.addHistory("field", "a");
        store.addHistory("field", "b");
        assertEquals(List.of("b", "a"), store.loadHistory("field"));
        assertEquals(List.of("a", "b"), store.addHistory("field", "a"));
        assertEquals(List.of("a", "b"), store.loadHistory("field"));
    }

    @Test
    void historyStaysWithinTheValueLimitOfThePreferences() {
        // an old history from before entries were limited
        node.put("history.field", "x".repeat(8000));
        var history = store.addHistory("field", "y".repeat(PreferencesStore.MAX_HISTORY_ENTRY_LENGTH));
        assertEquals(List.of("y".repeat(PreferencesStore.MAX_HISTORY_ENTRY_LENGTH)), history);
        assertTrue(node.get("history.field", "").length() <= Preferences.MAX_VALUE_LENGTH);
    }

    @Test
    void historySkipsEntriesThatCannotBeStored() {
        store.addHistory("field", "a");
        var tooLong = "9".repeat(PreferencesStore.MAX_HISTORY_ENTRY_LENGTH + 1);
        assertEquals(List.of("a"), store.addHistory("field", tooLong));
        assertEquals(List.of("a"), store.addHistory("field", "x\u001Fy"));

        var longest = "9".repeat(PreferencesStore.MAX_HISTORY_ENTRY_LENGTH);
        for (var i = 0; i < PreferencesStore.MAX_HISTORY; i++) {
            store.addHistory("field", i + longest.substring(1));
        }
        assertEquals(PreferencesStore.MAX_HISTORY, store.loadHistory("field").size());
    }
}
