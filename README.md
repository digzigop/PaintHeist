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
- **E:** Hold to copy a painting; tap near a hiding spot to hide or leave
- **F:** Swap a completed copy for the original
- **P:** Pause or resume; **R** restarts while paused
- **Esc:** Pause, then return to the main menu

Copy all three named artworks, steal the originals, and reach the marked exit.
Guards patrol, investigate sounds, chase visible players, search last-seen
locations, and return to their routes. Walls block movement and sight; hiding
spots conceal the player. Copies can be inspected and may eventually be
identified, increasing suspicion.

## Play-test checklist

1. Start a heist and check walking, diagonal speed, sprint noise, and collision
   against walls and display cases.
2. Hold **E** near each framed painting until copying completes, then press
   **F**. Approach a replacement and press **E** to inspect its quality.
3. Sprint or copy near a guard, break line of sight, and use **E** at a marked
   wardrobe or cabinet to hide and then leave.
4. Swap all three paintings and reach the lower-left exit. Also test pause,
   resume, restart, suspicion escalation, and the game-over screen.
