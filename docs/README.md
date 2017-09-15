This page is Minecraft Freedom Launcher's release notes. For the code, issues, pull requests or Wiki, visit [Repository's home page](https://github.com/Energy0124/MCFreedomLauncher).

# Newest version : v2.0.0-prerelease-03
# Recommended version : v1.5.2.03

## Release notes :

### v2.0.0-prerelease-03 Changes :
**Do NOT use this pre-release version unless you're a developer and you want to contribute to the project.**
#### Advanced changes (for developers) :
- (FIX) Applied fix for game launch JVM arguments.
- (FIX) Modified some exception catching.
- (ENHANCEMENT) Reorganized some code.

### v2.0.0-prerelease-02 Changes :
**Do NOT use this pre-release version unless you're a developer and you want to contribute to the project.**
- (ENHANCEMENT) Miscellaneous improvements.
#### Advanced changes (for developers) :
- Ensured profiles can't be saved when deprecated JVM arguments are inputted (even if non functional).
- (ENHANCEMENT) Unified style of JOptionPanes.
- (ENHANCEMENT) Updated LauncherConstants to only contain strings and methods to convert strings to URIs or URLs.
- (ENHANCEMENT) Modified Strings, URIs and URLs in LauncherConstants to be less redundant.
- (ENHANCEMENT) Updated LauncherConstants. Field names are now alphabetically sorted, and naming scheme is updated for easier managements.
- (ENHANCEMENT) Updated "Support" text button to use normal font rather than small gray font.
- (ENHANCEMENT) Removed unnecessary code.
- (ENHANCEMENT) Optimized some imports.
- (ENHANCEMENT) De-suppressed warnings to make debugging easier in the future.
- (ENHANCEMENT) Replaced even more magic numbers.
- (ENHANCEMENT) General code improvements.
- (ENHANCEMENT) Updated pom.xml to skip tests as there is none.
- (ENHANCEMENT) Minor changes and refinements.

### v2.0.0-prerelease Changes :
**Do NOT use this pre-release version unless you're a developer and you want to contribute to the project.**
- (ENHANCEMENT) Miscellaneous improvements.
#### Advanced changes (for developers) :
- (NEW) Decompiled official launcher 1.6.73-j with Procyon as new code base. Fixed a lot of unintentional errors by not updating on previous code.
- (FIX) Fixed some exception handlings.
- (ENHANCEMENT) Used Lambda expression and method references.
- (ENHANCEMENT) Properly added support for "Launcher Updates" tab.
- (ENHANCEMENT) Removed obsolete classes (SharedLauncherConstants, SidebarGridForm and StatusPanelForm).
- (ENHANCEMENT) Simplified some unnecessary code.
- (NEW) Added buttons (Forgot Migrated Email, Support, Minecraft Blog).
- (ENHANCEMENT) Added proper Java version check and resources check.
- (ENHANCEMENT) Added a forcefullyShutdown method and removed it from SwingUserInterface class.
- (ENHANCEMENT) Simplified and modified some strings.
- (ENHANCEMENT) Replaced a lot of magic numbers (magic constants) with readable values.
- (NEW) Notified users with a simple JOptionPane when exceptions of fake authentication are caught.
- (ENHANCEMENT) Expanded LauncherConstants to contain more Strings, URLs and URIs.
- (NEW) Updated default JVM arguments to boost performance and to not use deprecated features.
- (NEW) Updated profile editor to not accept the use of deprecated JVM arguments.
- (FIX) Fixed macOS app dock icon missing.
- (ENHANCEMENT) Removed deprecated methods.
- (ENHANCEMENT) Replaced code using deprecated methods from dependencies.
- (ENHANCEMENT) Replaced some Guava related code with native Java solutions.
- (NEW) Removed installation directory selection on first run as the option overlaps with Minecraft's implementation found in the profile editor.
- (ENHANCEMENT) General code improvements.
- (ENHANCEMENT) Updated pom.xml to use newer versions of dependencies.
- (ENHANCEMENT) Miscellaneous feature improvements.
- (BUG) Premium mode does not work (Authentication exception : message is null).
- (BUG) Launcher Logs tab only output logs when the launcher is run through IDE.
- (BUG) Progress bar does not show progress when downloading Minecraft assets.

### v1.5.2.03 Changes :
- (ENHANCEMENT) Graphics refinements.
- (ENHANCEMENT) Miscellaneous improvements.

### v1.5.2.02 Changes :
- (NEW) Added releases tab. You can now check for updates conveniently.
- (ENHANCEMENT) Miscellaneous improvements.

### v1.5.2.01 Changes :
- (NEW) Premium mode is now working! Try it out if you own a premium account.
- (ENHANCEMENT) Minor bug fixes and miscellaneous improvements.

### v1.5.2 Changes :
- (NEW) Updated base version to 1.5.2.
- (FIX) Removed launcher version check. Hopefully it'll work forever.
- (ENHANCEMENT) Miscellaneous improvements.
