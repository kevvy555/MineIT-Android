# MineIT Android Migration — Resource Architecture Direction

**Purpose:** guide the native architecture during migration without implementing the planned resource-economy overhaul.

The source for this direction is the September 2026 MineIT Resource Economy Discovery. That discovery proposes a later coordinated overhaul covering resource plausibility, extractor specialisation, generator/spacecraft fuel compatibility, refining, manufacturing and mature-colony economics.

## Migration decision

The resource overhaul is **not** part of the web-to-Android parity migration.

During migration we will:

- preserve current player-visible resource behaviour by default;
- improve resource architecture where this does not materially change gameplay;
- avoid hard-coding assumptions that would force another architectural rewrite when the resource overhaul begins;
- defer new resource names, balance, recipes, refined outputs, manufactured products and propulsion/fuel redesign until their own approved implementation work.

This follows the general migration rule: **structural improvement is encouraged; behavioural redesign is deferred.**

## Findings from the initial resource discovery that matter architecturally

The current web model exposes several structural limitations that should not be copied literally into Kotlin:

1. Broad categories currently carry too much semantic meaning: any `Fuel` can power uses that should eventually differ, any `Ore` can satisfy generic Industry requirements, and any `Build` material can satisfy generic construction requirements.
2. Resource-to-extractor compatibility is duplicated across multiple services instead of having one authoritative source.
3. Inventory and trade are strongly coupled to the current category/name catalogue.
4. Industry currently consumes generic Ore and applies processing effects without producing explicit transformed goods; the later overhaul intends actual outputs/recipes.
5. The planned long-term economy needs to represent raw resources, refined materials and manufactured goods without requiring separate inventory systems for each.
6. Resource quality is durable gameplay information and later may affect nutrition, energy or processing yield, not only sale value.

The migration should address the architecture behind these limitations without changing the present rules prematurely.

## Native resource model principles

### Stable identity first

Every stored resource should be addressed by a stable `ResourceId`. Display names and categories are metadata, not identity.

Renaming a resource later must not require rewriting every inventory, ship cargo, buyer or save reference by display string.

### Generic inventory entries

The durable inventory model should be conceptually capable of storing:

```text
ResourceId
quantity
quality / quality bands
```

Current Food/Build/Fuel/Ore behaviour can be expressed through resource definitions and rules rather than four unrelated storage implementations.

This does **not** mean removing the four current categories during migration. They remain current gameplay categories until an approved overhaul changes them.

### Category is data, not the whole rule

A resource may retain a current category such as Food, Build, Fuel or Ore, but future compatibility must not be inferred solely from that broad category.

The domain should be able to ask explicit questions such as:

- can this resource be extracted by this facility?
- can this resource satisfy this current construction requirement?
- can this resource be consumed by this generator?
- can this resource be loaded into this future propulsion system?
- can this resource be used as an input to this future recipe?

During parity migration, answers preserve the web rules unless a clear bug is deliberately corrected.

### One authoritative compatibility source

Resource/extractor compatibility must eventually have one canonical owner shared by:

- world/discovery rules;
- construction eligibility;
- extraction/collection;
- UI descriptions and error messages;
- tests.

Do not reproduce the current duplicated compatibility maps in multiple native services.

### Future processing classification without future products

The model may expose a broad lifecycle/classification concept such as raw/refined/manufactured if it genuinely simplifies the permanent schema, but migration must not invent the later catalogue.

It is sufficient for all current resources to map to their present semantics while leaving the representation extensible.

### Future recipes are a separate system

Do not implement recipe engines, new refined outputs or manufacturing orders merely because the future model will need them.

The resource representation should simply avoid preventing a later relationship of:

```text
raw inputs → recipe/process → refined output → manufacturing → goods/assets
```

### Quality remains first-class

Do not flatten current quality bands during migration. Preserve them in save/import/parity fixtures and model them so later quality effects can evolve without another inventory rewrite.

## Explicit migration non-goals

The following remain deferred until after native parity unless separately approved:

- finalising resource renames/merges/removals;
- defining the proposed refined-material catalogue;
- defining Components, Industrial Machinery, Control Systems or Ship/Habitat Assemblies;
- changing planetary resource distribution;
- changing extractor gameplay/facilities;
- replacing generic Industry consumption with production orders;
- changing generator fuel compatibility;
- introducing Propellant/Fusion Fuel;
- changing food nutrition or fuel-energy balance;
- changing buyer demand to refined/manufactured groups;
- rebalancing prices, yields or progression.

## Migration acceptance checks for resource architecture

Before the resource/domain migration is considered clean:

- [ ] resource identity is not based on display names;
- [ ] inventory is not implemented as four unrelated category-specific stores;
- [ ] quality bands survive import/save round trips;
- [ ] compatibility logic has a clear canonical owner or migration path to one;
- [ ] current four-category gameplay can be reproduced exactly enough for parity;
- [ ] future resource classifications/recipes can be added without replacing the root inventory model;
- [ ] no unapproved new resource economy behaviour was introduced.
