package name.ulbricht.locale.explorer.ui;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import name.ulbricht.locale.explorer.model.AppState;

/// Sets the example date-time to the current time once per second while "live" is on.
public final class LiveClock {

    private final AppState state;
    private final Timeline timeline;

    /// Creates the clock and follows the live flag of the state.
    ///
    /// @param state the application state
    public LiveClock(AppState state) {
        this.state = state;
        this.timeline = new Timeline(new KeyFrame(Duration.seconds(1), _ -> tick()));
        this.timeline.setCycleCount(Animation.INDEFINITE);
        state.liveProperty().subscribe(live -> {
            if (live) {
                tick();
                timeline.play();
            } else {
                timeline.stop();
            }
        });
    }

    /// Stops the clock.
    public void stop() {
        timeline.stop();
    }

    private void tick() {
        state.setDateTime(LocalDateTime.now(state.getZone()).truncatedTo(ChronoUnit.SECONDS));
    }
}
