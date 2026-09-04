# Conglomerate Buyer Portraits

This folder contains portrait artwork for contacts used by the Stage 8 **Conglomerate Buyers Service**.

## Preferred naming

Use sequential WEBP filenames:

- `buyer-0001.webp`
- `buyer-0002.webp`
- ...
- `buyer-1000.webp`

WEBP is preferred for mobile download size. PNG may be used temporarily while artwork is being prepared, but production integration should favour the WEBP copy.

## Canonical generation reference

Use:

`docs/Progression Stages/Stage 8/BuyerAndShipImageDirectory.html`

The BUYERS tab defines all 1,000 stable contact identities and provides the role, company, business type, commercial tier, home market, reputation level, resource interests, assigned ship and portrait-generation description for every buyer.

Buyer N uses `buyer-NNNN.webp` and is paired with Buyer Ship N.

## Assignment rules

Buyer identities do not depend on portraits. The game will assign available portraits deterministically when a new game's buyer pool is seeded.

- Use available portraits without replacement first so early/visible buyers are as visually distinct as possible.
- Reuse is allowed when the portrait pool is smaller than the buyer pool.
- Existing saves must preserve their assigned portrait key when more artwork is added later.
- If an assigned image is missing, the UI must show the buyer's name/initials instead; a missing portrait must never break buyer/contact or ship-event screens.

The gameplay design is documented in:

`docs/Progression Stages/Stage 8/ConglomerateBuyersService.md`