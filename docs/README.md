This page is Minecraft Freedom Launcher's release notes. For the code, issues, pull requests or Wiki, visit [Repository's home page](https://github.com/Energy0124/MCFreedomLauncher).

# Newest version : v2.0.0-prerelease
# Recommended version : v1.5.2.03

## Release notes :

### v2.0.0-prerelease Changes :
**Do NOT use this pre-release version unless you're a developer and you want to contribute to the project.**
- Miscellaneous improvements.
#### Advanced changes (for developers) :
- Decompiled official launcher 1.6.73-j with Procyon as new code base. Fixed a lot of unintentional errors by not updating on previous code.
- Fixed some exception handling.
- Used Lambda expression and method references.
- Properly added support for "Launcher Updates" tab.
- Removed obsolete classes (SharedLauncherConstants, SidebarGridForm and StatusPanelForm).
- Simplified some unnecessary code.
- Added buttons (Forgot Migrated Email, Support, Minecraft Blog).
- Added proper Java version check and resources check.
- Added a public static forcefullyShutdown method and removed it from SwingUserInterface class.
- Simplified and modified some strings.
- Replaced a lot of magic numbers (magic constants) with readable value.
- Notified users with a simple JOptionPane when exceptions of fake authentication are caught.
- Expanded LauncherConstants to contain more Strings, URL and URI.
- Updated default JVM arguments to boost performance and to not use deprecated features.
- Updated profile editor to not accept the use of deprecated JVM arguments.
- Fixed macOS app dock icon missing.
- Removed deprecated methods.
- Replaced code using deprecated methods from dependencies.
- General code improvements.
- Miscellaneous feature improvements.
- (BUG) Premium mode does not work (Authentication exception : message is null).
- (BUG) Launcher Logs tab only output logs when the launcher is run through IDE.
- (BUG) Progress bar does not show progress when downloading Minecraft assets.

### v1.5.2.03 Changes :
- Graphics refinements.
- Miscellaneous improvements.

### v1.5.2.02 Changes :
- Added releases tab. You can now check for updates conveniently :smiley:.
- Miscellaneous improvements.

### v1.5.2.01 Changes :
- Premium mode is now working! Try it out if you own a premium account :smile:.
- Minor bug fixes and miscellaneous improvements.

### v1.5.2 Changes :
- Updated base version to 1.5.2.
- Removed launcher version check. Hopefully it'll work forever :stuck_out_tongue:.
- Miscellaneous improvements.
