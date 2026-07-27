package com.ryanheise.audioservice;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.PowerManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPowerManager;

/**
 * Guards the fork's wake lock contract: held ONLY while playing, released
 * unconditionally on pause (NOT tied to androidStopForegroundOnPause — the
 * coupling that once held the lock for days while paused) and on destroy.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, shadows = ShadowAudioServicePlugin.class)
public class WakeLockTest {

    private ServiceController<AudioService> controller;
    private AudioService service;
    private PowerManager.WakeLock lock;

    private void setUp(boolean stopForegroundOnPause) {
        controller = Robolectric.buildService(AudioService.class).create();
        service = controller.get();
        AudioServiceConfig config = new AudioServiceConfig(service.getApplicationContext());
        config.androidStopForegroundOnPause = stopForegroundOnPause;
        service.configure(config);
        lock = ShadowPowerManager.getLatestWakeLock();
    }

    @Test
    public void heldOnlyWhilePlaying_evenWithStopForegroundOnPauseFalse() {
        setUp(false); // the app's real config — the historic leak case
        assertFalse(lock.isHeld());

        service.enterPlayingState();
        assertTrue("lock must be held while playing", lock.isHeld());

        service.exitPlayingState();
        assertFalse("pause must release the lock regardless of "
                + "androidStopForegroundOnPause", lock.isHeld());
    }

    @Test
    public void reacquiresOnResume() {
        setUp(true);
        service.enterPlayingState();
        service.exitPlayingState();
        service.enterPlayingState();
        assertTrue(lock.isHeld());
        service.exitPlayingState();
        assertFalse(lock.isHeld());
    }

    @Test
    public void destroyReleasesLock() {
        setUp(false);
        service.enterPlayingState();
        assertTrue(lock.isHeld());
        controller.destroy();
        assertFalse("service teardown must never leak the lock", lock.isHeld());
    }
}
