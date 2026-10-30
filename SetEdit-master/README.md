# SetEdit

A modern, Material 3 Settings Database Editor for Android.

> Looking for the latest build? See the [project README](../README.md) or grab the
> signed APK from the [latest release](https://github.com/NETO-DEV66/setedit-master/releases/latest/download/SetEdit-4.2-release.apk).

[<img src="https://img.shields.io/badge/license-GPLv3-5B4CDB" alt="GPLv3" height="28" />](LICENSE)

**WARNING:** We do not guarantee that problems caused by the improper use of this utility can be fixed, and we are unable to help you with any such problems. We support only this app, not your device's system software. Settings Database Editor (SetEdit) is invaluable if you need it, but if you're not careful you're very likely to mess something up.

By default, for your protection, Android prevents you from modifying the **SECURE** and **GLOBAL** tables. If you have Android Jellybean or later, you can remove this protection from an ADB shell using the following command:
```shell
pm grant io.github.netodev66.setedit android.permission.WRITE_SECURE_SETTINGS
```
On earlier versions, you can only remove this protection on a rooted device by installing SetEdit to the system partition.

Both `WRITE_SETTINGS` and `WRITE_SECURE_SETTINGS` permissions are optional if you do not need to edit any items.

### Android 14 and Up

SetEdit targets API 34, so it installs and launches normally on Android 14 and newer
without any bypass flags.

Starting with Android 15, the platform also hides most `system`, `secure` and
`global` keys from apps that target a modern API and rejects many writes. SetEdit
reads and writes these tables through a **privileged shell** so every table stays
editable:

- **Root** is used automatically when the device is rooted.
- **Shizuku** works on non-rooted devices: install and start
  [Shizuku](https://shizuku.rikka.app/), then open SetEdit's overflow menu →
  **Grant privileged access** → **Grant**.

Both routes run `settings get/put/delete` as the shell user (uid 2000), which the
`SettingsProvider` exempts from its target-SDK checks — the same behaviour as
`adb shell settings`.

### License

GNU General Public License v3.0 — see [LICENSE](LICENSE).

### Credits

Maintained by [NETO-DEV66](https://github.com/NETO-DEV66). Original GPL-3.0 copyright
notices are retained in the relevant source files as the license requires.
