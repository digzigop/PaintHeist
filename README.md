# PaintHeist
PaintHeist game project by 404 Tulog Productions for BukSU BSIT 2A batch S.Y 2026-2027 

Members :
D'Vaughn Gray V. Babanto
Leomar Dave U. Sumaga
Julia Ysabelle S. Seprado

## Run

Requires a JDK (Java 8 or newer). From the repository root in PowerShell:

```powershell
$build = Join-Path $env:TEMP 'paintheist-build'
New-Item -ItemType Directory -Force $build
javac -d $build src\*.java
java -cp $build Main
```

## Heist controls

- **WASD / Arrow keys:** Move
- **Shift:** Sprint (faster, but louder and easier to spot)
- **Ctrl:** Sneak (slower movement)
- **E:** Hold to copy a painting; tap to hide, use a security switch, or search a suspicious bookcase
- **F:** Swap a completed copy for the original
- **Q:** Throw a noise distraction in the direction you're facing (5-second recharge)
- **P:** Pause or resume; **R** restarts while paused
- **Esc:** Pause, then return to the main menu

Walk into light props to push them; sprint to move heavy carts. Pushing makes
noise, and props block guard and camera sight lines. Watch laser warning flashes
and cross only while the beams are inactive. Cameras need sustained sight of
you to raise an alarm; walls, props, hiding spots, and the timed security switch
can break their tracking. Approach a suspicious bookcase or search it with
**E** to reveal a secret passage, then walk into either entrance to use it.

Choose an unlocked museum from the level-selection screen. Complete a heist to
unlock the next museum; scores, stars, best times, and rewards are tracked for
the current game session. Copy every target artwork in the selected museum,
steal its original, and reach an exit.
Guards patrol, investigate sounds, chase visible players, search last-seen
locations, and return to their routes. Walls block movement and sight; hiding
spots conceal the player. Copies can be inspected and may eventually be
identified, increasing suspicion.

## Museums

1. **The Classic Gallery (Easy):** one target, a short patrol, a sweeping
   camera, and a noisy crate.
2. **The Grand Museum (Medium):** connected halls, a camera, pushable cover,
   multiple guards, and alternate exits.
3. **The Royal Exhibition (Hard):** rotating cameras, a warning-based laser
   cycle, a security switch, movable props, and a servants' passage.
4. **The Museum of Ancient Art (Hard):** multiple timed lasers, movable cover,
   broken sight lines, and a hidden passage through the pillar gallery.
5. **The National Art Museum (Very Hard):** several cameras, offset laser
   cycles, sprint-pushable cover, and a conservation passage.

## Play-test checklist

1. Start a heist and check walking, diagonal speed, sprint noise, and collision
   against walls, display cases, and props.
2. Stand in a camera cone and break line of sight before its detection meter
   completes. Confirm that walls and moved props block its view.
3. Push a light prop, sprint-push a cart, and confirm the resulting sound
   notification attracts nearby guards. Press **Q** and check its cooldown.
4. Observe a laser warning, cross while inactive, then test the alarm by
   walking into an active beam. Verify the security switch still disables
   lasers and cameras temporarily.
5. Search a suspicious bookcase with **E**, then walk through both ends of the
   revealed passage.
6. Hold **E** near each target painting until copying completes, then press
   **F**. Approach a replacement and press **E** to inspect its quality.
7. Sprint or copy near a guard, break line of sight, and use **E** at a marked
   wardrobe or cabinet to hide and then leave.
8. Swap all target paintings and reach a marked exit. Confirm that completing
   a museum unlocks the next one; also test pause, resume, restart, suspicion
   escalation, and the game-over screen.
