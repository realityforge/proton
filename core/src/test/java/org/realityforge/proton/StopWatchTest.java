package org.realityforge.proton;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

public final class StopWatchTest {
    @Test
    public void constructorInitializesNameAndDuration() {
        final var stopWatch = new StopWatch("Load");

        assertEquals(stopWatch.getName(), "Load");
        assertEquals(stopWatch.getTotalDuration(), 0L);
        assertEquals(stopWatch.toString(), "Load: 0");
    }

    @Test
    public void startStopAccumulatesDuration() throws InterruptedException {
        final var stopWatch = new StopWatch("Load");

        stopWatch.start();
        Thread.sleep(1);
        stopWatch.stop();
        final long firstDuration = stopWatch.getTotalDuration();
        assertTrue(firstDuration > 0L);

        stopWatch.start();
        Thread.sleep(1);
        stopWatch.stop();
        assertTrue(stopWatch.getTotalDuration() > firstDuration);
    }

    @Test
    public void resetClearsAccumulatedDuration() throws InterruptedException {
        final var stopWatch = new StopWatch("Load");
        stopWatch.start();
        Thread.sleep(1);
        stopWatch.stop();

        stopWatch.reset();

        assertEquals(stopWatch.getTotalDuration(), 0L);
    }

    @Test(
            expectedExceptions = IllegalStateException.class,
            expectedExceptionsMessageRegExp = "Attempted to start Load timer that had already been started")
    public void startFailsWhenAlreadyStarted() {
        final var stopWatch = new StopWatch("Load");

        stopWatch.start();
        stopWatch.start();
    }

    @Test(
            expectedExceptions = IllegalStateException.class,
            expectedExceptionsMessageRegExp = "Attempted to stop 'Load' timer that had not been started")
    public void stopFailsWhenNotStarted() {
        final var stopWatch = new StopWatch("Load");

        stopWatch.stop();
    }
}
