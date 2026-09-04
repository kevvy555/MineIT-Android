# Resource artwork

MineIT keeps the high-resolution source artwork and the map runtime atlas separate.

Each resource family retains its generated source PNGs under an `Originals/` directory. The current source images are 1254×1254 and remain available for future detail views, discovery cards, codex screens and other high-detail presentation.

## Runtime atlas

The colony map uses one shared atlas:

- `resource-atlas-256.webp`
- 40 resource frames
- 256×256 pixels per frame
- 8 columns × 5 rows
- 2048×1280 atlas
- transparent background
- WebP quality 92

`resource-atlas-256.json` records each resource frame and its original source path.

The atlas is preloaded from `index.html` and rendered through `js/ui/resource-icons-v597.js`, reducing the map from dozens of individual resource-image requests to one image request.

## Background handling

The original source PNGs are never modified. During atlas generation, only a conservative dark neutral background connected to an outer edge is removed for opaque source images. Existing alpha is preserved unchanged. No black-pixel filtering or cutout processing happens in the runtime renderer.

## Rebuilding

Uploading or replacing a PNG under a resource `Originals/` directory automatically triggers `.github/workflows/rebuild-resource-atlas.yml`.

The workflow runs `tools/build-resource-atlas.py`, regenerates the WebP atlas and JSON metadata, rebases on the current branch if necessary, and commits the rebuilt atlas back to the same branch.
