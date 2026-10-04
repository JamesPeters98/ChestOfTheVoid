# Chest of the Void Hytale plugin

Adds a soulbound, 63-slot inventory that each player can open from any Chest of the
Void. The items belong to the player, so breaking a chest does not drop them.

Version 1.2.0 targets **Hytale 0.6.8, release patchline**, checked on 4 October 2026.
Update 7 is currently on the pre-release patchline and is not a verified target.
The manifest deliberately uses `=0.6.8` rather than claiming compatibility with
every future server version.

## Build and run

Install a Java 25 JDK and update Hytale through the launcher. This project compiles
against the installed server JAR; it does not need to decompile the server.
The Hytale Gradle plugin is pinned to 0.8.1.

The existing installation path defaults to `G:\Games\Hytale`. To use another
installation, put `hytale.install_dir=C:/path/to/Hytale` in your user-level
`~/.gradle/gradle.properties`, or pass `"-Phytale.install_dir=C:/path/to/Hytale"`.

```powershell
# Set this to your Java 25 JDK if your default Java is older.
$env:JAVA_HOME = 'C:\path\to\jdk-25'
.\gradlew.bat build
```

The distributable is `build/libs/VoidStorage-1.2.0.jar`. Replace the previous
VoidStorage JAR in the server's `mods` folder, or in the launcher's
`UserData/Mods` folder for singleplayer, then restart the server/game. Install
only the main JAR, not the `-sources.jar`.

`build` runs six regression tests for save compatibility,
inventory isolation/cloning, legacy chest migration, and the packaged manifest.
Sentry is disabled for development runs. Check server startup manually after
installing the JAR: confirm that `JamesPeters:VoidStorage` is enabled, its asset
pack loads, and the server reaches `Universe ready`.

## Verification and remaining gameplay checks

The build and six regression tests pass, and the isolated 0.6.8 server loads and
enables the plugin, reaches `Universe ready`, then shuts down successfully.
Strict `--validate-assets` reports invalid paths for bundled Hytale instance
definitions, including `Defaults/Default` and `Basic`. The same failure occurs
on a vanilla server with no plugin installed; no VoidStorage asset errors were
reported during startup.

In-client gameplay has not been exercised automatically. Before using an existing
world, keep a backup and check opening two chests as the same player, separate
inventories for two players, reopening after reconnect/restart, breaking an open
chest, and accessing a chest placed by version 1.1.0. These checks cover the
client UI and real-world migration behavior that unit tests and a boot check
cannot establish.
