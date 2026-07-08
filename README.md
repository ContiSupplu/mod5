# 🏰 The Royal Kennel

A Fabric mod for **Minecraft 1.21.11** made for filming: a medieval, parchment-and-timber
"pet atelier" for your tamed dog. Sneak + right-click your dog and the camera glides out of
your body and slowly orbits the hound while you restyle it **live** — every change appears on
the actual dog in the world, mid-shot.

## What it does

- **Summon the atelier** — sneak + **right-click your tamed dog with an empty hand**.
- **Cinematic camera** — the camera eases over to the dog and slowly orbits it the whole time
  the menu is open. The dog is framed center-right, clear of the UI panel.
- **The dog stays put** — it stands posed for the fitting (its pathfinding is frozen each
  tick) and goes back to whatever it was doing when you close the menu.
- **Bloodline** — cycle through all 9 vanilla wolf breeds (Pale, Ashen, Black, Chestnut,
  Rusty, Snowy, Spotted, Striped, Woods), each with a lore epithet.
- **Headwear** — Crown of the Realm, Mage's Cap, or Knight's Helm, fitted to the head and
  turning with it.
- **Collar hue** — all 16 dye colors with a live swatch.
- **Title & name** — type a name (it appears over the dog as you type), or press
  *"Bestow a Noble Name"* for a random one (Sir Barksalot, Duke Fluffington, …).
- **Seal the Decree** — closes the charter with a burst of hearts over the dog. 💕

Hats are saved with the dog and visible to everyone nearby — your dog keeps its crown
while running around after the makeover (great for the outro shot).

## Building

You need **Java 21** (same as Minecraft 1.21.11). Then:

```bash
./gradlew build        # jar lands in build/libs/royal-kennel-1.0.0.jar
```

Drop the jar in your `mods/` folder together with **Fabric Loader ≥ 0.16** and
**Fabric API** for 1.21.11.

To test straight from the repo without installing anything:

```bash
./gradlew runClient
```

> **If dependency resolution fails** on the first build: the pinned Fabric versions in
> `gradle.properties` (`loader_version`, `fabric_version`) were set without network access.
> Grab the current numbers for 1.21.11 from <https://fabricmc.net/develop> and paste them in —
> that page shows the exact three lines used here.

## Camera controls (while the atelier is open)

- The camera **orbits the dog slowly on its own** — one lap is roughly a 40s short.
- **Left-drag** anywhere on the world (not the panel) to steer it yourself: drag
  sideways to circle the dog, up/down to raise or lower the shot. Taking the reins
  pauses the auto-orbit.
- **Scroll wheel** zooms in and out.
- The **"✦ Camera" chip** (top right) toggles between *Orbiting* and *Held*.

## Filming tips (for the 40-second short)

- Groom in an open, pretty spot — the orbiting camera doesn't dodge walls (by design, it's tiny).
- Press **F1 before opening the menu**: the HUD disappears but the atelier UI still renders,
  so you get a clean parchment-over-world shot.
- The dog's name floats above it and updates live while you type — good for a beat in the edit.
- Every breed/hat/collar change pops green sparkles on the dog; *Seal the Decree* pops hearts.
- Puppies wear the hats too, at 55% scale.

## Tuning

All the "cinematography" constants (orbit distance, speed, framing offset, glide time,
drag/zoom feel) are at the top of `client/ClientGroomingSession.java`. Every hat
offset/size lives in `client/AccessoryRenderer.java` — plain numbers, safe to nudge.

## Troubleshooting

This was written against Mojang's official mappings for 1.21.11 without a compiler on hand,
so if Mojang shuffled a name late in the 1.21.x cycle you may hit an isolated compile error.
The risky spots are deliberately small and contained:

| Symptom | Where | Fix |
|---|---|---|
| `setVariant`/`getVariant` not found on `Wolf` | `GroomingSessions.java` | Check the method name on `Wolf` in your IDE (variant setter changed packages in 1.21.5; it lives in `net.minecraft.world.entity.animal.wolf`). |
| `matrixStack()`/`consumers()` missing | `AccessoryRenderer.render` | Fabric's `WorldRenderContext` accessor names moved; use the equivalents your Fabric API version offers. |
| Camera never moves | `CameraMixin` | The `setup` inject didn't apply — check the mixin name against `net.minecraft.client.Camera` in your mappings. |

Everything else (networking, attachments, events, the screen) uses long-stable Fabric API surface.

## Not goals

Made as a private toy for one video — no config, no other pets, no localization, no wall-avoidance
on the camera. Dogs only, as requested. 🐕
