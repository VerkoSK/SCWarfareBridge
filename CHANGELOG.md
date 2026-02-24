# Changelog — SC-SuperB Bridge

All notable changes to SC-SuperB Bridge are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [1.1.2] — 2026-02-24

### Changed
- Mod renamed from **SC Warfare Bridge** to **SC-SuperB Bridge**

---

## [1.1.1] — 2026-02-24

### Changed

- `resistanceFactor` default `0.35 → 0.70` — doubles the effective destruction radius for all weapons
- `maxDestroyChance` default `0.65 → 1.0` — blocks at the epicenter are now guaranteed to be destroyed

### Result

| Weapon | Effective radius before | Effective radius after |
|---|---|---|
| Tank AP shell (~4) | ~1.4 blocks | ~2.8 blocks |
| Tank HE shell (~10) | ~3.5 blocks | ~7.0 blocks |
| GBU-57 (~22) | ~7.7 blocks | ~15.4 blocks |
| Nuke (~30) | ~10.5 blocks | ~21.0 blocks |

---

## [1.1.0] — 2026-02-24

### Added

- **Server-authoritative config** — config type changed from `COMMON` to `SERVER`
  - Stored in `serverconfig/scwarfarebridge-server.toml` on the server
  - Automatically synced to all connecting clients, clients cannot override it
- **Forge update checker** — `updateJSONURL` added to `mods.toml`, points to `update.json` on GitHub
  - Notifies in the mod list when a newer version is available
- **Direct-hit AP shell handler** — listens on `ProjectileHitEvent.HitBlock` via runtime reflection
  - Covers the case where AP/APFSDS shells hit reinforced blocks directly (no explosion fallback needed)

### Fixed

- AP shells had no effect on reinforced blocks
  - Root cause: AP shells hitting hardness=-1 blocks fall to `causeExplode()` with a small radius (~3–5), which was below the old `minExplosionPower = 8.0` threshold
  - Fix: lowered `minExplosionPower` default from `8.0` to `3.0`

---

## [1.0.0] — 2026-02-24

### Added

- Initial release
- Hooks into `ExplosionEvent.Detonate` to run a secondary damage pass on nearby SecurityCraft reinforced blocks
- Destruction scales dynamically with explosion radius — heavy ordnance devastates, small arms have no effect
- Zero compile-time dependency on SecurityCraft or Superb Warfare — all detection via registry names and runtime reflection
- Public API (`SCWarfareBridgeAPI`) for other mods to query and break reinforced blocks with weapon-power scaling
- SecurityCraft listed as optional dependency in `mods.toml`
