package io.github.netodev66.setedit.tile;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.annotation.NonNull;

import io.github.netodev66.setedit.MainActivity;
import io.github.netodev66.setedit.R;

/**
 * A Quick Settings tile that opens SetEdit.
 *
 * <p>It is a launcher, not a toggle: there is no single "settings" state to represent, so tapping
 * the tile simply brings the app to the front. The tile is available from Android 7 (API 24), the
 * app's minimum.
 */
public class SetEditTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        Tile tile = getQsTile();
        if (tile == null) return;
        tile.setLabel(getString(R.string.tile_label));
        tile.setState(Tile.STATE_INACTIVE);
        tile.updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            if (android.os.Build.VERSION.SDK_INT >= 34) {
                startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent,
                        PendingIntent.FLAG_IMMUTABLE));
            } else {
                // The PendingIntent overload only exists from API 34; below it the Intent one is
                // the only option, so the deprecation is unavoidable on the older branch.
                startActivityAndCollapseLegacy(intent);
            }
        } catch (Throwable th) {
            startActivity(intent);
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    @SuppressWarnings("deprecation")
    private void startActivityAndCollapseLegacy(@NonNull Intent intent) {
        startActivityAndCollapse(intent);
    }
}
