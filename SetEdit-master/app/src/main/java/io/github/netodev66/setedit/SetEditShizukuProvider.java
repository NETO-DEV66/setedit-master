package io.github.netodev66.setedit;

import android.util.Log;

import rikka.shizuku.ShizukuProvider;

/**
 * Shizuku entry point that cannot take the whole app down with it.
 *
 * <p>{@link ShizukuProvider} runs during process startup, before the first screen. Its built-in
 * Sui initialisation performs non-SDK reflection ({@code ServiceManager#getService}) and, when
 * that reflection is denied on a modern {@code targetSdk}, it throws out of the provider and the
 * framework aborts the process with "Unable to get provider". Sui is a Magisk module and root is
 * already available to this app through libsu, so Sui auto-initialisation is turned off and any
 * other failure is swallowed. The Shizuku (ADB) binder path is unaffected.
 */
public final class SetEditShizukuProvider extends ShizukuProvider {
    private static final String TAG = "SetEditShizuku";

    static {
        try {
            ShizukuProvider.disableAutomaticSuiInitialization();
        } catch (Throwable th) {
            Log.w(TAG, "Could not disable automatic Sui initialisation", th);
        }
    }

    @Override
    public boolean onCreate() {
        try {
            return super.onCreate();
        } catch (Throwable th) {
            Log.w(TAG, "Shizuku provider initialisation failed", th);
            return true;
        }
    }
}
