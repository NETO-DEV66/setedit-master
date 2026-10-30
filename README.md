<div align="center">

<img src="docs/assets/icon.png" alt="SetEdit icon" width="112" height="112" />

# SetEdit

**Settings Database Editor — view and edit Android system settings directly.**

A modern, Material 3 Settings Database Editor for Android.

[![Latest release](https://img.shields.io/github/v/release/NETO-DEV66/setedit-master?label=release&color=5B4CDB)](https://github.com/NETO-DEV66/setedit-master/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/NETO-DEV66/setedit-master/total?color=5B4CDB)](https://github.com/NETO-DEV66/setedit-master/releases)
[![Min SDK](https://img.shields.io/badge/Android-7.0%2B%20(API%2024)-5B4CDB)](https://developer.android.com/about/versions/nougat)
[![Target SDK](https://img.shields.io/badge/target-API%2034-5B4CDB)](https://developer.android.com/about/versions/14)
[![License](https://img.shields.io/badge/license-GPLv3-5B4CDB)](LICENSE)
[![Website](https://img.shields.io/badge/website-live-5B4CDB)](https://neto-dev66.github.io/setedit-master/)

[**Download APK**](https://github.com/NETO-DEV66/setedit-master/releases/latest/download/SetEdit-4.0-release.apk) ·
[**Website**](https://neto-dev66.github.io/setedit-master/) ·
[**Releases**](https://github.com/NETO-DEV66/setedit-master/releases) ·
[**Report a bug**](https://github.com/NETO-DEV66/setedit-master/issues)

</div>

---

## What is SetEdit?

SetEdit lets you read and change Android's system settings tables — `system`,
`secure`, and `global` — the same values you would normally reach with `adb shell
settings`. Edit any row by name or value, or add a new one.

> [!WARNING]
> Changing system settings can break your device. SetEdit supports the app, not your
> device's system software. Make a note of the original value before you change
> anything.

## Features

| | |
|---|---|
| **Material 3 design** | Full colour-role palette for light and dark, tonal cards, and edge-to-edge system bars. |
| **Dashboard home** | A bottom-navigation shell — Home, Search, Favorites, Backup, Settings — with quick actions and a table overview. |
| **Search everywhere** | One search box that scans the `system`, `secure` and `global` tables and jumps straight to a match. |
| **Favorites** | Star any key and reach it again from the Favorites tab. |
| **Backup & restore** | Export the tables to a file and restore them later, key by key. |
| **Recipes** | One-tap bundles of well-known tweaks, applied together with a confirmation. |
| **History & revert** | Every change is recorded, so an earlier value can be restored from the editor. |
| **Risky-key warnings** | Keys that can destabilise a device are flagged before they are written. |
| **Onboarding** | A short first-run tour that explains the privileged-access model. |
| **Editor bottom sheet** | Create, edit and delete keys from a Material 3 modal sheet. |
| **Table switcher** | Move between `system`, `secure`, `global`, and other tables from the toolbar. |
| **Shortcuts & automation** | Export actions as shortcuts and run them from other apps. |
| **Android TV** | A leanback surface for the big screen. |
| **Works on Android 15, 16 and 17** | Targets API 34 so it installs everywhere, and uses root or Shizuku to keep every table editable. |
| **Lean build** | R8 and resource shrinking keep the signed release APK small and quick to start. |

## Download

Grab the latest signed APK:

**[⬇ SetEdit-4.0-release.apk](https://github.com/NETO-DEV66/setedit-master/releases/latest/download/SetEdit-4.0-release.apk)** (~2.0 MB)

| | |
|---|---|
| Package | `io.github.netodev66.setedit` |
| Version | 4.0 (versionCode 20) |
| Requires | Android 7.0 (API 24) or newer |
| Size | ~2.0 MB |

Verify the download:

```shell
sha256sum SetEdit-4.0-release.apk
# 424d4aa018bac2053a7f8cf511022fc4baead64e7c93ac393bb913c98088de29
```

## Permissions

By default Android blocks edits to the **secure** and **global** tables. To allow
them, grant the permission once over ADB:

```shell
pm grant io.github.netodev66.setedit android.permission.WRITE_SECURE_SETTINGS
```

`WRITE_SETTINGS` and `WRITE_SECURE_SETTINGS` are both optional if you only need to
read values. On older devices you can instead install SetEdit to the system
partition on a rooted device.

### Android 15, 16 and 17

Starting with Android 15 the platform refuses to install any app that targets below
API 24, and once an app targets a modern API the `SettingsProvider` hides most
`system`, `secure` and `global` keys from it and rejects many writes. SetEdit now
targets API 34, so it installs and launches normally, and it reads and writes the
tables through a **privileged shell** instead of the `ContentResolver`:

- **Root** — used automatically when the device is rooted. Nothing to set up.
- **Shizuku** — for non-rooted devices. Install and start
  [Shizuku](https://shizuku.rikka.app/), then open SetEdit's overflow menu →
  **Grant privileged access** → **Grant**.

Both routes run the standard `settings get/put/delete` commands as the shell user
(uid 2000), which the `SettingsProvider` exempts from its target-SDK checks — the
same behaviour as `adb shell settings`. Without either, the app still opens but can
only show the keys the platform exposes to a regular app.

> On Android 14 you may still see a low-target-SDK prompt when sideloading older
> builds; SetEdit 3.3+ no longer needs it because it targets API 34.

## Build from source

Requirements: **JDK 17** and the **Android SDK** (platform 34, build-tools 34.0.0,
NDK 26.1.10909125, CMake 3.22.1).

```shell
git clone https://github.com/NETO-DEV66/setedit-master.git
cd setedit-master/SetEdit-master

# point Gradle at your SDK
echo "sdk.dir=$ANDROID_HOME" > local.properties

./gradlew assembleDebug     # debug APK
./gradlew assembleRelease   # signed release APK
```

Output lands in `app/build/outputs/apk/`.

### Release signing

`app/build.gradle` reads signing values in this order: `key.properties` →
`STORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD` environment variables → a local
`app/release.keystore`. Without a keystore the release build is left unsigned.
Keystores and `key.properties` are git-ignored.

## Project layout

```
SetEdit-master/
├── app/
│   ├── src/main/java/io/github/netodev66/setedit/   # activities, ui/, data/, utils
│   ├── src/main/res/                                    # layouts, themes, icons
│   └── build.gradle
└── docs/                                                # GitHub Pages site
```

## License

GNU General Public License v3.0 — see [LICENSE](SetEdit-master/LICENSE).

## Credits

Maintained by [NETO-DEV66](https://github.com/NETO-DEV66).

SetEdit is distributed under the GNU General Public License v3.0. It is derived from
earlier GPL-3.0 code, whose original copyright notices are retained in the relevant
source files as the license requires. See [LICENSE](SetEdit-master/LICENSE).
