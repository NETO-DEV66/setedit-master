package io.github.netodev66.setedit.update;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import io.github.netodev66.setedit.BuildConfig;

/**
 * Checks GitHub Releases for a newer SetEdit build and downloads its APK.
 *
 * <p>Everything here is blocking network I/O and must run off the main thread. The version
 * comparison is numeric on the dotted parts so {@code 4.10} is correctly newer than {@code 4.9}.
 */
public final class UpdateChecker {

    private static final String LATEST_RELEASE_API =
            "https://api.github.com/repos/NETO-DEV66/setedit-master/releases/latest";
    private static final String RELEASES_API =
            "https://api.github.com/repos/NETO-DEV66/setedit-master/releases?per_page=10";
    private static final String USER_AGENT = "SetEdit-Android/" + BuildConfig.VERSION_NAME;
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 20_000;
    private static final int MAX_JSON_BYTES = 1 << 20;

    private UpdateChecker() {
    }

    /**
     * Fetches the newest release and compares it with the installed version.
     *
     * <p>{@code /releases/latest} only returns non-prerelease releases, so if the newest release
     * is a prerelease the list endpoint is used instead and the first non-draft, non-prerelease
     * entry is taken.
     */
    @NonNull
    public static UpdateInfo check(@NonNull Context context) {
        try {
            String json = httpGetString(LATEST_RELEASE_API);
            JSONObject release = new JSONObject(json);
            String tag = release.optString("tag_name", "");
            if (tag.isEmpty()) {
                // Only prereleases exist; fall back to the list endpoint.
                release = firstStableRelease(httpGetString(RELEASES_API));
                if (release == null) return UpdateInfo.upToDate(BuildConfig.VERSION_NAME);
                tag = release.optString("tag_name", "");
            }
            return evaluate(release, tag);
        } catch (Exception e) {
            return UpdateInfo.failed();
        }
    }

    @Nullable
    private static JSONObject firstStableRelease(@NonNull String json) throws Exception {
        JSONArray array = new JSONArray(json);
        for (int i = 0; i < array.length(); i++) {
            JSONObject release = array.getJSONObject(i);
            if (release.optBoolean("draft", false)) continue;
            if (release.optBoolean("prerelease", false)) continue;
            return release;
        }
        return null;
    }

    @NonNull
    private static UpdateInfo evaluate(@NonNull JSONObject release, @NonNull String tag)
            throws Exception {
        String remote = VersionComparator.normalize(tag);
        if (!VersionComparator.isNewer(remote, BuildConfig.VERSION_NAME)) {
            return UpdateInfo.upToDate(BuildConfig.VERSION_NAME);
        }

        String name = release.optString("name", "SetEdit v" + remote);
        String notes = release.optString("body", "");
        String releaseUrl = release.optString("html_url", null);

        String apkUrl = null;
        long apkSize = -1;
        JSONArray assets = release.optJSONArray("assets");
        if (assets != null) {
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                String assetName = asset.optString("name", "");
                if (assetName.toLowerCase(java.util.Locale.ROOT).endsWith(".apk")) {
                    apkUrl = asset.optString("browser_download_url", null);
                    apkSize = asset.optLong("size", -1);
                    break;
                }
            }
        }
        return UpdateInfo.available(remote, name, notes, apkUrl, apkSize, releaseUrl);
    }

    /** Downloads the APK to {@code destination} (a file path in the app's cache dir). */
    public static void download(@NonNull String apkUrl, @NonNull String destination,
                                @Nullable ProgressListener listener) throws Exception {
        HttpURLConnection connection = open(apkUrl);
        try {
            int total = connection.getContentLength();
            if (listener != null) listener.onStart(total);
            try (InputStream raw = new BufferedInputStream(connection.getInputStream());
                 java.io.FileOutputStream out = new java.io.FileOutputStream(destination)) {
                byte[] buffer = new byte[16 * 1024];
                long read = 0;
                int count;
                while ((count = raw.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                    read += count;
                    if (listener != null) listener.onProgress(read, total);
                }
                out.flush();
            }
        } finally {
            connection.disconnect();
        }
    }

    @NonNull
    private static HttpURLConnection open(@NonNull String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("Accept-Encoding", "gzip");
        connection.setInstanceFollowRedirects(true);
        return connection;
    }

    @NonNull
    private static String httpGetString(@NonNull String url) throws Exception {
        HttpURLConnection connection = open(url);
        try {
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new java.io.IOException("HTTP " + code + " for " + url);
            }
            InputStream stream = connection.getInputStream();
            if ("gzip".equalsIgnoreCase(connection.getContentEncoding())) {
                stream = new GZIPInputStream(stream);
            }
            try (InputStream in = new BufferedInputStream(stream);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int count;
                int total = 0;
                while ((count = in.read(buffer)) != -1) {
                    total += count;
                    if (total > MAX_JSON_BYTES) break;
                    out.write(buffer, 0, count);
                }
                return out.toString(StandardCharsets.UTF_8.name());
            }
        } finally {
            connection.disconnect();
        }
    }

    /** Reports download progress. {@code total} is {@code -1} when the server omits it. */
    public interface ProgressListener {
        void onStart(int totalBytes);

        void onProgress(long bytesRead, int totalBytes);
    }

    /** True when this device can install an APK handed to it by the package installer. */
    public static boolean canInstall(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return context.getPackageManager().canRequestPackageInstalls();
        }
        return true;
    }
}
