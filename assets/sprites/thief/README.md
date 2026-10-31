# Thief sprite atlas

These transparent PNGs are extracted from the existing `Painted Thief Sprite
Sheet.png` character artwork. Frames are aligned to a common cell size and
retain the original character design.

- Frame size: **80 x 128 px**
- Frames per animation and direction: **6**
- Directions in each animation PNG: **front, back, left, right** (one row each)
- Animation PNG layout: **6 columns x 4 rows** (`480 x 512 px`)
- Combined atlas layout: **6 columns x 20 rows** (`480 x 2560 px`)
- Combined atlas row order: idle, walk, run, sneak, jump; within each action:
  front, back, left, right.
- Short idle loops repeat their source poses. Other directions with fewer than
  six source poses hold the last pose in the remaining cell.

`atlas.json` contains the exact row order and source frame counts.

## Extracting frames in Java

For example, to load the six-frame front-facing walk row:

```java
BufferedImage sheet = ImageIO.read(
    new File("assets/sprites/thief/walk.png"));
BufferedImage frame = sheet.getSubimage(
    frameIndex * 80, 0, 80, 128);
```

In the combined atlas, the front-facing walk row is row 4 (zero-based), so use
`frameIndex * 80` for X and `4 * 128` for Y. Use `Graphics2D.drawImage` with the
frame image to render it; the background is transparent.
