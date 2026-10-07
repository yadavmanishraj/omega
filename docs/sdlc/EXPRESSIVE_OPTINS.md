# Material 3 Expressive — Opt-In Ledger

**Pinned material3 version: `1.5.0-alpha29`** (over Compose BOM `2026.09.00`,
which alone maps the 1.4.0 stable). Exact pin, no version ranges — see
`gradle/libs.versions.toml` and M3_EXPRESSIVE_SPEC §0 (Option E).

## Rules

- Every `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` site in the
  codebase is recorded in the table below, in the same change that adds it.
- Expressive APIs are consumed ONLY through `:core:designsystem` (theme,
  tokens) and `:core:ui` (components). Feature modules never import an
  expressive API directly (wrapper discipline, spec §0).
- **Any bump of the pinned alpha is a deliberate, changelog-reviewed
  change**: re-read the material3 release notes for every alpha between the
  old and new pin, re-check every site in this ledger against the rename /
  deprecation list, and update this file in the same commit.

## Sites

| # | File | Declaration | Expressive APIs used | Notes |
|---|------|-------------|----------------------|-------|
| 1 | `core/designsystem/src/main/java/com/manishraj/saavnmusic/ui/theme/Theme.kt` | `OmegaShapes` | `Shapes` expanded slots: `largeIncreased`, `extraLargeIncreased`, `extraExtraLarge` | Values continue the OmegaRadius scale (20/32/48dp). |
| 2 | `core/designsystem/src/main/java/com/manishraj/saavnmusic/ui/theme/Theme.kt` | `OmegaTypography` | `Typography` 30-style constructor with the 15 `…Emphasized` styles | Twins defined in Wave 0; consumed by screens in later waves only. |
| 3 | `core/designsystem/src/main/java/com/manishraj/saavnmusic/ui/theme/Theme.kt` | `SaavnTheme` | `MaterialExpressiveTheme`, `MotionScheme.expressive()` | Color-scheme resolution unchanged from the pre-expressive theme. |

`MaterialExpressiveTheme` itself was promoted out of the experimental marker
in 1.5.0-alpha18 and `MotionScheme` graduated in alpha15; the opt-ins above
are retained deliberately while any expressive API is used in the file
(RESEARCH_2 §8), so a future alpha that re-gates any of these APIs fails
loudly at exactly these recorded sites instead of silently changing
behavior.
