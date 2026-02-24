# SC Warfare Bridge

A lightweight Minecraft Forge compatibility mod that lets military explosions from [Superb Warfare](https://github.com/Mercurows/SuperbWarfare) and all its addons damage [SecurityCraft](https://www.curseforge.com/minecraft/mc-mods/security-craft) reinforced blocks.

Drop a single `.jar` into your `mods/` folder — no modified versions of SecurityCraft or Superb Warfare required.

## How it works

SecurityCraft reinforced blocks normally have `Float.MAX_VALUE` blast resistance, making them completely immune to all explosions. SC Warfare Bridge hooks into Forge's explosion event system and runs a secondary damage pass against nearby reinforced blocks based on the explosion's power.

**Destruction formula:**
```
effectiveRadius = explosion_radius × resistanceFactor
chance_per_block = maxDestroyChance × (1 − distance / effectiveRadius)
```

This means:
- Heavier ordnance (bombs, missiles, nukes) tears through reinforced walls in a wide radius.
- Lighter weapons (grenades, small charges) barely scratch the surface — or have no effect at all.
- Even at ground zero, reinforced blocks still have a chance to survive, reflecting their toughened nature.

### Damage scaling examples (default settings)

| Weapon | Explosion radius | Effective radius | Effect |
|---|---|---|---|
| Grenade / pistol | < 8 | — | **No effect** (below threshold) |
| Tank shell | ~10 | ~3.5 blocks | Small breach |
| GBU-57 bunker buster | 22 | ~7.7 blocks | Major breach |
| AGM-158 cruise missile | 22 | ~7.7 blocks | Major breach |
| Nuclear bomb (center) | 30 | ~10.5 blocks | Devastating |

## Compatibility

- **Minecraft**: 1.20.1
- **Forge**: 47.2.0+
- **Required**: None — works standalone
- **Enhanced by**: [SecurityCraft](https://www.curseforge.com/minecraft/mc-mods/security-craft) (the mod this targets)
- **Works with**: [Superb Warfare](https://github.com/Mercurows/SuperbWarfare) and any addon that creates vanilla-style Forge explosions — including [AshVehicle](https://github.com/AshViper/AshVehicle) and others

## Installation

1. Install Minecraft Forge 1.20.1 (47.2.0+)
2. Install SecurityCraft and Superb Warfare (or any military mod)
3. Drop `scwarfarebridge-x.x.x.jar` into your `mods/` folder
4. Launch the game

## Configuration

After first launch, edit `config/scwarfarebridge-common.toml`:

```toml
[general]
    # Minimum explosion radius to affect reinforced blocks (default: 8.0)
    # Smaller explosions have zero effect — prevents pistols/grenades from chipping walls.
    minExplosionPower = 8.0

    # Resistance multiplier (default: 0.35)
    # effectiveRadius = explosion_radius × this_value
    # Lower = harder to destroy. 1.0 = same as normal blocks.
    resistanceFactor = 0.35

    # Max destruction probability at ground zero (default: 0.65)
    # Even at the epicenter, blocks aren't guaranteed to break.
    maxDestroyChance = 0.65
```

## For mod developers

SC Warfare Bridge exposes a public API that other mods can use for direct block-breaking (e.g., penetrating projectiles that bypass the explosion system):

```java
import dev.scwarfarebridge.api.SCWarfareBridgeAPI;

// Check if a block is reinforced
boolean reinforced = SCWarfareBridgeAPI.isReinforced(serverLevel, blockPos);

// Attempt to break with weapon-power scaling
// Returns true if the block was reinforced (whether or not it broke)
boolean wasReinforced = SCWarfareBridgeAPI.tryBreakReinforced(serverLevel, blockPos, weaponPower);
```

## Building from source

```bash
git clone https://github.com/YOUR_USERNAME/SCWarfareBridge.git
cd SCWarfareBridge
./gradlew build
```

Output JAR will be in `build/libs/`.

## License

MIT
