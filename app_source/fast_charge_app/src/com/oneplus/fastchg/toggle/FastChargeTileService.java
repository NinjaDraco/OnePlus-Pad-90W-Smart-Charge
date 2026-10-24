package com.oneplus.fastchg.toggle;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import java.io.File;

public class FastChargeTileService extends TileService {
    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "/data/local/tmp/toggle_fast_charge.sh"});
            p.waitFor();
            updateTile();
        } catch (Exception e) {}
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile != null) {
            boolean unlocked = new File("/data/local/tmp/super_charge_unlocked").exists();
            tile.setState(unlocked ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            tile.setLabel(unlocked ? "满血快充: 开" : "满血快充: 关");
            tile.updateTile();
        }
    }
}
