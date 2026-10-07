# Teste-jue — Godot runtime

Godot 4.7.2 stable is now part of the repository as the target engine foundation.

The project is intentionally source/configuration based. Godot editor binaries and generated .godot data are not committed.

Current foundation:
- Native 3D scene and camera.
- Mobile-compatible renderer configuration.
- Procedural arena, portal and enemy placeholders.
- Real-time movement and combat loop.
- Offline save/load.
- Day/night lighting cycle.
- Android export preset.
- CI validation/export path.

The existing native Android/OpenGL implementation remains during the migration so the current playable path is not discarded. New gameplay/visual systems can now be implemented in Godot and validated independently.

Planned Godot systems:
Rooftop/HQ, Portal, Expeditions, Rifts, Versus, story campaign, weapons, abilities, bosses, monsters, wardrobe, upgrade cores, contracts, events, daily goals, crafting, inventory, audio/VFX, statistics and mobile performance budgets.
