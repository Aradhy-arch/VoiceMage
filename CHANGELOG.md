# Changelog

## 2.14.1

### Changed
- The speech model is now built into the mod jar. VoiceMage no longer downloads anything: no first-launch download, no internet connection needed (the jar is larger, around 65 MB)

### Removed
- The first-launch download of the speech model (and its checksum and retry logic)

## 2.14.0

### Added
- Microphone picker: `/voicemic`, `/voicemic <number>`, `/voicemic default` (saved in `config/voicemage-client.json`)
- Microphones that cannot record at 16 kHz mono (many USB headsets and laptops) now work: audio is converted automatically
- Config option `freezeSingleTarget` (default `true`)
- Temporary walls, ice cages, clones and polymorph animals are cleaned up after a crash

### Changed
- Freeze now freezes only the creature you aim at. Set `freezeSingleTarget` to `false` for the old area effect
- Breaking a temporary block (ice, Earth Wall) removes it cleanly: no water, no drops
- Muting and unmuting voice recognition is instant (the speech model stays loaded)

### Fixed
- Frozen creatures no longer suffocate in their own ice
- Ice from Freeze no longer leaves water behind when broken or melted
- A broken `voicemage.json` is backed up instead of overwritten; `/voicemage reload` keeps the old settings
- Typos in the Explosion chant text ("skys", "frome")
