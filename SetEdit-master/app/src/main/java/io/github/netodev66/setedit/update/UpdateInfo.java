package io.github.netodev66.setedit.update;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** The newest release the update check found, or the reason none is available. */
public final class UpdateInfo {

    public enum Status {
        /** A newer release exists and can be downloaded. */
        AVAILABLE,
        /** The installed build is already the newest one. */
        UP_TO_DATE,
        /** The check could not reach GitHub (no network, rate limit, or a server error). */
        FAILED
    }

    @NonNull
    public final Status status;
    /** Release tag without the leading {@code v}, e.g. {@code 4.1}. Empty when not available. */
    @NonNull
    public final String version;
    /** Human readable release title, e.g. {@code SetEdit v4.1}. */
    @NonNull
    public final String name;
    /** Release notes in Markdown. */
    @NonNull
    public final String notes;
    /** Direct download URL of the APK asset. */
    @Nullable
    public final String apkUrl;
    /** Size of the APK asset in bytes, or {@code -1} when unknown. */
    public final long apkSize;
    /** Page the user can open in a browser to see the release. */
    @Nullable
    public final String releaseUrl;

    private UpdateInfo(@NonNull Status status, @NonNull String version, @NonNull String name,
                       @NonNull String notes, @Nullable String apkUrl, long apkSize,
                       @Nullable String releaseUrl) {
        this.status = status;
        this.version = version;
        this.name = name;
        this.notes = notes;
        this.apkUrl = apkUrl;
        this.apkSize = apkSize;
        this.releaseUrl = releaseUrl;
    }

    static UpdateInfo available(@NonNull String version, @NonNull String name, @NonNull String notes,
                                @Nullable String apkUrl, long apkSize, @Nullable String releaseUrl) {
        return new UpdateInfo(Status.AVAILABLE, version, name, notes, apkUrl, apkSize, releaseUrl);
    }

    static UpdateInfo upToDate(@NonNull String version) {
        return new UpdateInfo(Status.UP_TO_DATE, version, "", "", null, -1, null);
    }

    static UpdateInfo failed() {
        return new UpdateInfo(Status.FAILED, "", "", "", null, -1, null);
    }
}
