# KigoCHEDU TV Launcher

A clear glass take on the CleanYourTV launcher for Google TV / Android TV, kept on the
`kigochedu-glass-launcher` branch of this fork. Everything else (the TV clean-up guide, tools and the
tested TV lists) is the original [CleanYourTV](https://github.com/Gathin23/cleanyourtv) project, MIT licence.

## What is different

- **Fixed dock:** one row of apps stays at the bottom; Down / Up fade it to the next or previous batch instead
  of scrolling the screen. "Apps scroll up" brings back the original layout.
- **App backdrop:** the selected app's own artwork fills the screen above the row, unblurred, for every app.
  Or a **custom wallpaper**: the TV's wallpaper collection, a picture on the TV (Downloads, USB), or one found on
  Google Images / Google Drive in the TV's browser.
- **Clear glass look:** smaller round glass buttons, a glass dock band and clear glass dropdowns.
- **Icon styles:** Original (each app's artwork), Tinted, Clear and Dark, chosen on the TV.
- **Header:** the clock in the corner, with the greeting and the date beside it.
- **Connections** button: Wi-Fi (with the network name), Ethernet and Bluetooth.
- **Settings** button: the TV settings' main pages (Network, Accounts, Apps, Display & Sound, Remotes,
  Privacy, Accessibility, System) and Launcher settings.
- **Launcher settings:** icon size (4–8 per row), icon style, names under apps, fixed or scrolling dock,
  background, clock format, and a **Save** button: changes only stick once saved.

## Install it on a TV

1. Download this repository and switch to the `kigochedu-glass-launcher` branch.
2. Open it in a coding assistant (Claude Code or similar) on a computer on the same Wi-Fi as the TV, and say:
   *"Clean up my TV and install the launcher. My TV is a &lt;brand and model&gt; at &lt;IP address&gt;."*
3. The assistant follows [PROMPT.md](PROMPT.md): it backs up the TV, asks which apps you use, switches off
   only the rest, then builds and installs this launcher as the home screen.

Everything can be undone; see [docs/restore.md](docs/restore.md). Defaults for the dock and other launcher
choices are in `launcher/app/src/main/res/values/config.xml`.
