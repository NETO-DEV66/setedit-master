package io.github.netodev66.setedit.ui;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import io.github.netodev66.setedit.Native;
import io.github.netodev66.setedit.TableTypeInt;
import io.github.netodev66.setedit.boot.ActionItem;
import io.github.netodev66.setedit.boot.BootUtils;
import io.github.netodev66.setedit.shortcut.ShortcutItem;
import io.github.netodev66.setedit.shortcut.ShortcutUtils;
import io.github.netodev66.setedit.utils.SettingsUtils;

/** Loads the rows of any table, whether it is a settings table or one of the read-only views. */
public final class TableData {
    private TableData() {
    }

    @NonNull
    public static List<Pair<String, String>> load(@NonNull Context context,
                                                  @TableTypeInt int tableType) {
        switch (tableType) {
            case TableTypeInt.TABLE_SYSTEM:
            case TableTypeInt.TABLE_SECURE:
            case TableTypeInt.TABLE_GLOBAL: {
                List<Pair<String, String>> items =
                        SettingsUtils.readAll(context, Tables.settingsTypeOf(tableType));
                return items == null ? new ArrayList<>() : items;
            }
            case TableTypeInt.TABLE_PROPERTIES:
                return androidProperties();
            case TableTypeInt.TABLE_JAVA:
                return javaProperties();
            case TableTypeInt.TABLE_ENV:
                return environment();
            case TableTypeInt.TABLE_BOOT:
                return bootItems(context);
            case TableTypeInt.TABLE_SHORTCUTS:
                return shortcuts(context);
            default:
                return new ArrayList<>();
        }
    }

    @NonNull
    private static List<Pair<String, String>> androidProperties() {
        List<String[]> list = new ArrayList<>();
        try {
            Native.setPropertyList(list);
        } catch (Throwable ignored) {
        }
        List<Pair<String, String>> items = new ArrayList<>(list.size());
        for (String[] pair : list) {
            items.add(new Pair<>(pair[0], pair[1]));
        }
        return items;
    }

    @NonNull
    private static List<Pair<String, String>> javaProperties() {
        Properties properties = System.getProperties();
        List<String> names = new ArrayList<>(properties.stringPropertyNames());
        Collections.sort(names);
        List<Pair<String, String>> items = new ArrayList<>(names.size());
        for (String name : names) {
            items.add(new Pair<>(name, properties.getProperty(name)));
        }
        return items;
    }

    @NonNull
    private static List<Pair<String, String>> environment() {
        java.util.Map<String, String> env = System.getenv();
        List<String> names = new ArrayList<>(env.keySet());
        Collections.sort(names);
        List<Pair<String, String>> items = new ArrayList<>(names.size());
        for (String name : names) {
            items.add(new Pair<>(name, env.get(name)));
        }
        return items;
    }

    @NonNull
    private static List<Pair<String, String>> bootItems(@NonNull Context context) {
        List<ActionItem> actions = new ArrayList<>(BootUtils.getBootItems(context));
        Collections.sort(actions, (a, b) -> a.name.compareToIgnoreCase(b.name));
        List<Pair<String, String>> items = new ArrayList<>(actions.size());
        for (ActionItem action : actions) {
            items.add(new Pair<>(action.table + " \u2013 " + action.name, action.flattenToString()));
        }
        return items;
    }

    @NonNull
    private static List<Pair<String, String>> shortcuts(@NonNull Context context) {
        List<ShortcutItem> shortcuts = new ArrayList<>(ShortcutUtils.getShortcutItems(context));
        Collections.sort(shortcuts, (a, b) -> a.name.compareToIgnoreCase(b.name));
        List<Pair<String, String>> items = new ArrayList<>(shortcuts.size());
        for (ShortcutItem shortcut : shortcuts) {
            items.add(new Pair<>(shortcut.name, shortcut.actionItems.size() + " items"));
        }
        return items;
    }

    @Nullable
    public static String readValue(@NonNull Context context, @TableTypeInt int tableType,
                                   @NonNull String key) {
        for (Pair<String, String> item : load(context, tableType)) {
            if (key.equals(item.first)) return item.second;
        }
        return null;
    }
}
