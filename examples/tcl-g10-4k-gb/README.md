# TCL Smart TV Pro (G10, Google TV 12)

Living room TV: TCL Smart TV Pro, device G10, product G10_4K_GB, Realtek rtd6748, firmware STT2.230203.001 AS16
(TCL software V8-0008T01-LF1v534.002065). Formuler Z11 P on HDMI 1, Sony HT-G700 soundbar on HDMI 3 (eARC).

What is set up here:

- 42 packages disabled (`debloat.txt`): TCL and Samba TV tracking, TCL promos, store demo, voice assistant, Alexa,
  YouTube, Netflix, TCL Live, firmware updates and Android leftovers. Nothing is uninstalled.
- The launcher from `launcher/` as Home, with the stock Google TV launcher and setup wizard disabled.
  Dock: HDMI 1, Wholphin, Spotify. TCL HOME, Quick panel and Settings are hidden from the grid.
- The TV switches to HDMI 1 every time it turns on (`power_on_input`).
- The remote's Netflix button reconnects the soundbar (`audio_reset_keycodes`).
- The gear button opens Settings and the Input button the input menu (key services, `HOME_KEY_SERVICE=1` and
  `INPUT_KEY_SERVICE=1`).

`config.xml` here is a copy of the launcher config this TV was built with. Copy it over
`launcher/app/src/main/res/values/config.xml` before building for this TV.

## Build and install the launcher

```bash
cp examples/tcl-g10-4k-gb/config.xml launcher/app/src/main/res/values/config.xml
TV_HOST=<tv ip> INPUT_KEY_SERVICE=1 HOME_KEY_SERVICE=1 launcher/build-and-install.sh
```

Gradle 8.9 does not run on Java 25: point `JAVA_HOME` at a JDK 17 or 21 first.

## Undo everything

```bash
TV_HOST=<tv ip> examples/tcl-g10-4k-gb/restore.sh
```

This re-enables every package in `debloat.txt` (Prime Video and YouTube Music stay disabled, as they shipped) and
gives Home back to the Google TV launcher. It reads the record `tools/recovery.py` saved before the first change,
in `~/.cleanyourtv/`; keep that folder. The record is per IP: if the TV gets a new address, copy
`recovery-<old ip>-*.json` to the same name with the new IP.

## Re-enable one app

```bash
TV_HOST=<tv ip> tools/tv.py 'pm enable com.netflix.ninja'
```

Package names are in `debloat.txt`, one per line with what each does.

## Change the dock

Edit `dock_apps` in `config.xml` (package names, or `input:HDMI 2` for an input) and run the build command
again. Without a rebuild: hold OK on the Settings pill and choose reorder, or push a theme whose `config.json`
sets `dock_apps` (themes/README.md).

## Change the wallpaper

Put a `wallpaper.jpg` next to a `config.json` in a theme folder and push it:

```bash
TV_HOST=<tv ip> tools/theme.sh themes/<your theme>
```

`tools/theme.sh --reset` goes back to the plain black background.

## Firmware quirks found on this set

- The Wi-Fi network joined during first setup belongs to the setup wizard (setupwraith), and Android deletes it
  when the wizard is disabled. It was forgotten and re-joined in Settings before disabling the wizard.
- TCL's boot manager (`[TclAppBoot] forbid bind service`) sometimes refuses to start the launcher's key services
  at boot, and resets `APP_AUTO_START` to ignore at every boot, so `appops set` does not stick. The launcher
  rebinds them itself when Home first shows (`KeyServices`), using `WRITE_SECURE_SETTINGS` from
  build-and-install.sh.
- The audio output is the global setting `tcl_device_out_type`: 2 = internal speaker, 16 = HDMI ARC/eARC. TCL's
  system service applies a change on its own (System Audio Mode, SPDIF mode), so writing 2 and then 16 is the
  same as switching the output to the speakers and back in Settings. The TV sometimes falls back to 2 by itself.
- The remote's Netflix and YouTube buttons send keycodes 4062 and 4065, the gear button 83 (KEYCODE_NOTIFICATION).
- Chromecast was kept, but the setup wizard is disabled, and on other Google TV sets that breaks casting after a
  reboot ("Untrusted device"): the Cast receiver reads its credentials from setupwraith. Not tested here yet. If
  casting fails, see docs/TROUBLESHOOTING.md, "Casting fails with Untrusted device".
- Screenshots of HDMI come back empty; check `dumpsys activity activities | grep mResumedActivity` instead.
- Location is off, so the Wi-Fi pill shows "Wi-Fi" instead of the network name.

## When you are done with ADB

Settings > System > Developer options: switch off USB debugging (and Network debugging if shown), then
"Revoke USB debugging authorisations", and restart the TV. Check from the computer that `nc -vz <tv ip> 5555`
fails.
