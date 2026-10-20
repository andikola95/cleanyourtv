# cleanyourtv (TCL fork)

Debloat an Android TV / Google TV over Wi-Fi and replace its home screen with a clean, Apple TV style launcher.
No root, nothing uninstalled, and everything can be undone.

This is a fork of [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv), set up on a TCL Smart TV Pro
(Google TV 12) with a Formuler box on HDMI 1 and a Sony soundbar on eARC. Upstream has the full documentation,
the device lists for 100+ other TVs and the background; this README covers the basics and what this fork adds.

## What this fork adds

- **Inputs in the dock:** a `dock_apps` entry such as `input:HDMI 1` is a tile that switches straight to that input.
- **Power-on input:** `power_on_input` switches the TV to an input every time it turns on, from standby or a full
  boot. Home still shows the launcher.
- **Soundbar button:** `audio_reset_keycodes` turns a remote button (here Netflix) into "reconnect the soundbar". It
  switches the TV's audio output to the speakers and back, the fix when the TV drops an ARC/eARC soundbar.
- **Survives TCL's boot manager:** TCL firmware sometimes refuses to start the launcher's remote-key services at
  boot. The launcher notices and restarts them itself, so the remote buttons keep working after a reboot.
- **[`examples/tcl-g10-4k-gb/`](examples/tcl-g10-4k-gb/):** the debloat list, restore script, launcher config and
  notes for this TV.

## How it works

1. **Back up** the installed packages and settings.
2. **Disable** telemetry, ads, promos and preloaded apps with `pm disable-user`. Nothing is uninstalled.
3. **Install** the launcher from `launcher/` and make it the default home screen.

## Setting up a TV

You need a computer on the same network as the TV, Android platform-tools (`adb`), JDK 17 or 21 and the
Android SDK (platform 34). Gradle does not run on Java 25.

1. **On the TV:** Settings → System (or Device Preferences) → About → press OK on **Build** seven times. Then
   Developer options → turn on **USB debugging**, and **Network debugging** if the TV shows it.
2. **With a coding assistant:** open this repo in Claude Code (or similar), paste [`PROMPT.md`](PROMPT.md) with your
   TV model and accept "Allow USB debugging?" on the TV. It backs up, debloats in small batches, builds the
   launcher and checks each step with screenshots from the TV.
3. **Or by hand**, with a list from `examples/`:

```bash
python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt
TV_HOST=<tv ip> tools/disable.sh examples/<your tv>/debloat.txt
TV_HOST=<tv ip> INPUT_KEY_SERVICE=1 HOME_KEY_SERVICE=1 launcher/build-and-install.sh
```

On Google TV, check `examples/tcl-g10-4k-gb/README.md` before disabling the setup wizard: if it owns the Wi-Fi
network, the TV drops off Wi-Fi.

## Configuring the launcher

Edit [`launcher/app/src/main/res/values/config.xml`](launcher/app/src/main/res/values/config.xml) and run
`build-and-install.sh` again. The keys this fork uses:

| Key | Example | What it does |
|---|---|---|
| `dock_apps` | `input:HDMI 1`, `com.spotify.tv.android` | Dock tiles in order: package names or inputs |
| `fill_dock` | `false` | Leave free dock slots empty instead of filling them with grid apps |
| `hidden_apps` | `com.tcl.dashboard` | Packages never shown as tiles |
| `power_on_input` | `input:HDMI 1` | Input to switch to whenever the TV turns on; empty for none |
| `audio_reset_keycodes` | `4062` | Remote buttons that reconnect the soundbar |
| `audio_reset_setting`, `audio_reset_speaker`, `audio_reset_soundbar` | `tcl_device_out_type`, `2`, `16` | The audio output setting and its speaker and soundbar values (TCL Google TV) |
| `settings_key_keycodes` | `83` | Remote buttons that open Settings (the TCL gear button) |

The remote keys, `power_on_input` and the soundbar button need `HOME_KEY_SERVICE=1` at install, which also grants
the launcher the permissions they use. To find another TV's values, diff `adb shell settings list global` before
and after switching the audio output in Settings, and watch `adb logcat | grep KeyEvent` while pressing a button.

## Undoing it

`disable.sh` saves the TV's state in `~/.cleanyourtv/` before changing anything. Restore replays it:

```bash
TV_HOST=<tv ip> tools/restore.sh examples/<your tv>/debloat.txt com.google.android.apps.tv.launcherx
```

Single apps can be re-enabled with `TV_HOST=<tv ip> tools/tv.py 'pm enable <package>'`, or in the TV's
Settings → Apps.

When you're done, switch off USB debugging on the TV, revoke the debugging authorisations and restart it: network
ADB on port 5555 is unencrypted.

## Licence

MIT, see [`LICENSE`](LICENSE). Based on [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv). The Outfit
typeface is under the SIL Open Font License, see `launcher/FONT-LICENSE-OFL.txt`.
