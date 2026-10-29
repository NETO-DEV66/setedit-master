<div align="center">

<img src="docs/assets/icon.png" alt="SetEdit icon" width="112" height="112" />

# SetEdit

**Settings Database Editor — view and edit Android system settings directly.**

A modern, Material 3 Settings Database Editor for Android.

[![Latest release](https://img.shields.io/github/v/release/NETO-DEV66/setedit-master?label=release&color=5B4CDB)](https://github.com/NETO-DEV66/setedit-master/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/NETO-DEV66/setedit-master/total?color=5B4CDB)](https://github.com/NETO-DEV66/setedit-master/releases)
[![Min SDK](https://img.shields.io/badge/Android-5.0%2B%20(API%2021)-5B4CDB)](https://developer.android.com/about/versions/lollipop)
[![License](https://img.shields.io/badge/license-GPLv3-5B4CDB)](LICENSE)
[![Website](https://img.shields.io/badge/website-live-5B4CDB)](https://neto-dev66.github.io/setedit-master/)

[**Download APK**](https://github.com/NETO-DEV66/setedit-master/releases/latest/download/SetEdit-3.2-release.apk) ·
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
| **Smooth animations** | Spring-based row entrances, press feedback that settles with a bounce, and animated dialogs. |
| **Fresh app icon** | An adaptive sliders mark with monochrome support and a matching TV banner. |
| **Table switcher** | Move between `system`, `secure`, `global`, and other tables from the toolbar. |
| **Search** | Filter long setting lists instantly. |
| **Add, edit, delete** | Create new keys, edit values, and remove entries (subject to permission). |
| **Shortcuts & automation** | Export actions as shortcuts and run them from other apps. |
| **Android TV** | A leanback surface for the big screen. |
| **Lean build** | R8 and resource shrinking keep the signed release APK small and quick to start. |

## Download

Grab the latest signed APK:

**[⬇ SetEdit-3.2-release.apk](https://github.com/NETO-DEV66/setedit-master/releases/latest/download/SetEdit-3.2-release.apk)** (~1.8 MB)

| | |
|---|---|
| Package | `io.github.netodev66.setedit` |
| Version | 3.2 (versionCode 11) |
| Requires | Android 5.0 (API 21) or newer |
| Size | ~1.8 MB |

Verify the download:

```shell
sha256sum SetEdit-3.2-release.apk
# 0f18db13c2b07acef142cd1a4a91b148dd51973a31af8be8a9d305ff7f2647cd
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

### Android 14 and up

SetEdit targets API 22 on purpose — API 23 (Android M) and later stop it from
changing some settings without root or ADB. Android 14 added a block on installing
apps that target API 22 or lower, so install with the bypass flag:

```shell
adb install --bypass-low-target-sdk-block SetEdit-3.2-release.apk
```

Or, from a rooted shell:

```shell
pm install --bypass-low-target-sdk-block SetEdit-3.2-release.apk
```

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
│   ├── src/main/java/io/github/netodev66/setedit/   # activities, adapters, utils
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
