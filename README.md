# Exe-tacticals

Modular tactical guns for **Minecraft Forge 1.20.1** (Java 17).
Exe-tacticals is a fork of [GunsCraft Reforged (GCR)](https://github.com/JustARandomGuyNo512/GCR_P01) by Sheridan,
based on its `forge-1.20.1` branch, with no third-party library dependencies (no LDLib2, no jMonkeyEngine).

## Build

```
gradlew.bat build          # jar -> build/libs/exe_tacticals-<version>.jar
gradlew.bat runClient      # dev client
gradlew.bat runServer      # dev dedicated server
```

Only Forge is required at runtime. Oculus/Embeddium are optional: shader compatibility is detected at runtime.

## License

Exe-tacticals is free software licensed under the **GNU General Public License v3.0 or later (GPL-3.0-or-later)**,
the same license as GunsCraft Reforged.

Original work: Guns Craft Reforged (GCR), Copyright (C) 2026 Sheridan. Any modified or redistributed version must
comply with the GPL and retain the original copyright notices and license information.

### Third-party assets

The 3D models, model meshes, textures, animations, sounds and other associated assets shipped with GCR are
**not original works of the GCR author** (model resources: https://sketchfab.com/DU1701). The GCR author obtained
permission to use them; that permission does **not** extend to third parties, and the GPL does **not** grant
permission to copy, redistribute, modify, extract or reuse these assets. Verify that you have permission from the
respective creators before publicly distributing a build that contains them.

GCR is provided **without warranty**, to the extent permitted by applicable law.
