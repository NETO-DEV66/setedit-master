package io.github.netodev66.setedit.data;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import io.github.netodev66.setedit.R;
import io.github.netodev66.setedit.TableTypeInt;

/**
 * Presentation metadata for one row of the table list: what it is called, how it is described and
 * which icon represents it. Keeping this in one place lets the dashboard, the search results and
 * the table browser agree on how a table is shown.
 */
public final class TableMeta {

    @TableTypeInt
    public final int tableType;
    @StringRes
    public final int titleRes;
    @StringRes
    public final int descriptionRes;
    @DrawableRes
    public final int iconRes;
    /** Whether the table is written through the {@code settings} provider (system/secure/global). */
    public final boolean settingsTable;
    /** Whether the table is considered advanced and hidden behind the "show advanced" preference. */
    public final boolean advanced;

    private TableMeta(@TableTypeInt int tableType, @StringRes int titleRes,
                      @StringRes int descriptionRes, @DrawableRes int iconRes,
                      boolean settingsTable, boolean advanced) {
        this.tableType = tableType;
        this.titleRes = titleRes;
        this.descriptionRes = descriptionRes;
        this.iconRes = iconRes;
        this.settingsTable = settingsTable;
        this.advanced = advanced;
    }

    private static final TableMeta[] ALL = {
            new TableMeta(TableTypeInt.TABLE_SYSTEM, R.string.system_table, R.string.table_desc_system,
                    R.drawable.ic_table_system, true, false),
            new TableMeta(TableTypeInt.TABLE_SECURE, R.string.secure_table, R.string.table_desc_secure,
                    R.drawable.ic_table_secure, true, false),
            new TableMeta(TableTypeInt.TABLE_GLOBAL, R.string.global_table, R.string.table_desc_global,
                    R.drawable.ic_table_global, true, false),
            new TableMeta(TableTypeInt.TABLE_PROPERTIES, R.string.android_properties,
                    R.string.table_desc_properties, R.drawable.ic_table_properties, false, true),
            new TableMeta(TableTypeInt.TABLE_JAVA, R.string.java_properties, R.string.table_desc_java,
                    R.drawable.ic_table_java, false, true),
            new TableMeta(TableTypeInt.TABLE_ENV, R.string.linux_environment, R.string.table_desc_env,
                    R.drawable.ic_table_env, false, true),
            new TableMeta(TableTypeInt.TABLE_BOOT, R.string.boot_items, R.string.table_desc_boot,
                    R.drawable.ic_table_boot, false, true),
            new TableMeta(TableTypeInt.TABLE_SHORTCUTS, R.string.shortcuts,
                    R.string.table_desc_shortcuts, R.drawable.ic_table_shortcut, false, true),
    };

    @NonNull
    public static TableMeta[] all() {
        return ALL.clone();
    }

    @NonNull
    public static TableMeta of(@TableTypeInt int tableType) {
        for (TableMeta meta : ALL) {
            if (meta.tableType == tableType) return meta;
        }
        return ALL[0];
    }
}
