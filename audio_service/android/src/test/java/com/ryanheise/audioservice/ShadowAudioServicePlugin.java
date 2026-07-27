package com.ryanheise.audioservice;

import android.content.Context;

import io.flutter.embedding.engine.FlutterEngine;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/** JVM tests can't load Flutter's native engine; onCreate only stores it. */
@Implements(AudioServicePlugin.class)
public class ShadowAudioServicePlugin {
    @Implementation
    public static FlutterEngine getFlutterEngine(Context context) {
        return null;
    }
}
