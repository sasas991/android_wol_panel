# Wol Panel


A simple Wake-on-LAN panel for Android. Manage your machines and wake them over your LAN.

## Screenshots
<table>
  <tr>
    <td><img src="screenshot1.png" width="400"><br><sub>Main Page</sub></td>
    <td><img src="screenshot2.png" width="400"><br><sub>Device add page</sub></td>
  </tr>
</table>

## Features

- Device list, saved across restarts
- **Wake** button sends a Wake-on-LAN magic packet
- Online/offline status per device
- Auto-checks status every 15s, plus manual refresh

## Adding a device

Tap **+** and fill in:

- **Name** - any label
- **MAC address** - e.g. `AA:BB:CC:DD:EE:FF`
- **Host / IP** - used for the status check, e.g. `192.168.1.50`
- **Broadcast address** - e.g. `192.168.1.255`
- **WoL port** - usually `9`
- **Ping TCP port** - an open port for accurate status (e.g. `22`, `80`, `3389`)

## Build

Open in Android Studio and press Run, or:

```
./gradlew :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/`.

For a release build:

```
./gradlew :app:assembleRelease
```

This produces `app/build/outputs/apk/release/app-release-unsigned.apk`. The APK
is deliberately left unsigned — F-Droid builds from source and signs with its
own key.

## Privacy

The app has no server side. It only talks to the devices you enter yourself, on
your own network: a UDP magic packet to wake them and a TCP connect to check
whether they are up. No analytics, no tracking, no ads, no proprietary
dependencies — everything it links against is Apache-2.0 licensed AndroidX,
Compose and Kotlin.

## F-Droid

Store listing text, icon and screenshots live in
`fastlane/metadata/android/en-US/`. The build recipe to submit to
[fdroiddata](https://gitlab.com/fdroid/fdroiddata) is in
`fdroid/com.sasas991.wolpanel.yml`.

## License

[GPL-3.0](LICENSE)
