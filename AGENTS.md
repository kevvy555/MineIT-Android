# MineIT Android AI Development Contract

This file is the authoritative repository-level development guidance for AI coding agents working on MineIT Android.

## Before writing code

1. Read this `AGENTS.md` in full before making code changes.
2. Read the relevant implementation and tests before deciding where a change belongs.
3. Identify the canonical owner of behaviour before adding production code.
4. For substantial architectural changes, explain the intended design first unless the user has already approved the work.

## Architecture

- `app/src/main/java/.../domain/` owns gameplay rules and immutable game models.
- `app/src/main/java/.../data/` owns persistence and external data adapters.
- `app/src/main/java/.../ui/` owns Jetpack Compose presentation and transient view state.
- UI renders state and dispatches intent; it must not become the source of truth for gameplay.
- Keep dependencies directional: UI -> domain/data contracts; domain must not depend on Android UI APIs.
- Prefer immutable state and explicit state transitions.
- Do not create versioned, duplicate, `new`, `old`, `copy`, temporary, or compatibility production implementations.

## Development principles

- Apply SOLID pragmatically; prefer KISS and YAGNI over speculative abstraction.
- Keep changes small and coherent.
- Use descriptive names and explicit error handling.
- Keep Android-specific concerns at the application boundary where practical.
- Mobile-first interaction, accessibility, and performance are requirements.

## Testing

- Gameplay/domain changes require unit or domain regression coverage.
- Bug fixes should reproduce the failure in a test when practical.
- UI interaction changes should add Compose/instrumentation coverage when unit tests cannot prove behaviour.
- Do not weaken architecture or regression tests merely to obtain a green build.
- Run focused tests and the full required CI suite before declaring significant work complete.

## Definition of done

A change is complete only when the requested behaviour is implemented in its canonical owner, relevant tests pass, boundaries remain clean, no duplicate implementation is introduced, and documentation is updated when project state materially changes.
