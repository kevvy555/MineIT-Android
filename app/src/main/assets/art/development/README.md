# Building artwork

MineIT keeps the high-resolution source artwork and the runtime atlas separate.

Each development family uses this structure:

```text
assets/art/development/<family>/
  originals/
    <family>-l1.png
    <family>-l2.png
    <family>-l3.png
    <family>-l4.png
    <family>-l5.png
  <family>-levels-256.webp
```

Supported families:

- `housing`
- `industry`
- `quarry`
- `mine`
- `deep-mine`
- `rig`
- `farm`
- `ranch`
- `bio-harvester`
- `algae-facility`

## Runtime atlas

The game map uses `<family>-levels-256.webp`.

- 5 horizontal frames, L1 through L5 from left to right
- 256×256 pixels per frame
- 1280×256 pixels per atlas
- transparent background
- WebP quality 92

The original PNGs remain available for future high-detail building views, panels and previews. They are not loaded for every map tile.

## Rebuilding

Uploading or replacing an image under an `originals/` directory automatically triggers `.github/workflows/rebuild-building-atlases.yml`. The workflow runs `tools/build-building-atlases.py` and commits any changed high-resolution atlases back to the same branch.

The previous `<family>-levels.webp` files are legacy low-resolution atlases. The v5.9.1 renderer no longer uses them; they can be removed after the high-resolution rollout is verified.

## Current rollout status

All 10 building families now have five source PNGs and a generated 1280×256 high-resolution runtime atlas on `feature/high-res-building-atlases`.
