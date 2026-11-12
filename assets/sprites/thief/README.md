# Thief sprites

The game loads the thief's transparent sprite strips from this directory:

- `Idle.png`
- `Front Walk.png`
- `Back.png`
- `Left Walk.png`
- `Right Walk.png`

Each image contains four animation frames arranged horizontally. The game
splits each strip into four equal-width frames and repeats the final frame as
needed to fit its six-frame animation slots. PNG transparency is preserved.
When both project files and packaged resources are available, the game loads
the current files from this directory first. Idle frames are used while the
thief is stationary; the matching directional walk strip is used while moving.
