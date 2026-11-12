# MyFit — Visual identity

## Concept

MyFit exists to show progress. The symbol is an **"M" drawn as a single progress line**. The line rises, dips and rises again above the first peak, ending in a **highlighted green dot**, like the latest data point on a chart. It works as both an initial and a chart, without resorting to dumbbells or silhouettes.

The interface follows the same principle: graphite neutrals, firm typography and **a single accent color**, used only where there is progress or a positive action.

## Symbol and wordmark

- 24-unit grid. Line `M3,19 L8,9 L12,14.5 L16.74,8.18`, 2.6 stroke with round caps and joins.
- Dot: center (19.5, 4.5), radius 2.3, separated from the line by a 1-unit gap.
- Single source of the geometry: `ui/components/MyFitMark.kt` (`MarkGeometry`). The `ic_launcher_foreground`, `ic_launcher_monochrome` and `ic_stat_myfit` (notification icon) vectors use the same numbers.
- Wordmark: `myfit` in lowercase, Geist SemiBold, −0.04 em letter spacing, to the right of the symbol.
- App icon: `#0B0B0C` background, `#F5F5F4` line, `#47F55E` dot; monochrome version for themed icons (Android 13+).
- Minimum symbol size: 16 dp.

## Colors

| Token | Dark | Light | Usage |
|---|---|---|---|
| `background` | `#0B0B0C` | `#FAFAF9` | screen background |
| `surface` | `#141416` | `#FFFFFF` | cards |
| `surfaceRaised` | `#1C1C1F` | `#F2F2F0` | controls, dialogs, navigation indicator |
| `outline` | `#2A2A2E` | `#E2E2DF` | thin borders (1 dp) |
| `outlineStrong` | `#3A3A40` | `#CBCBC7` | secondary button borders |
| `textPrimary` | `#F5F5F4` | `#0B0B0C` | primary text |
| `textSecondary` | `#A1A1A8` | `#5A5A62` | labels, metadata |
| `textTertiary` | `#6B6B73` | `#8E8E95` | disabled, inactive icons |
| `accent` | `#47F55E` | `#15803D` | primary action, progress, completed sets |
| `onAccent` | `#0B0B0C` | `#FFFFFF` | text on green |
| `danger` | `#F2555A` | `#CD2B31` | destructive actions, errors |
| `warning` | `#F5A524` | `#B45309` | warnings |

Contrast is enforced by `ColorContrastTest`: primary text ≥ 7:1, secondary ≥ 4.5:1, text on green ≥ 4.5:1, green on background ≥ 3:1.

**Green rule:** only for progress (rest ring, completed set, positive load change), the screen's primary action and action links. Body measurement differences use a neutral color, because the app doesn't judge whether going up or down is good.

## Typography

Geist (SIL OFL 1.1, bundled in `res/font`, license in `docs/licenses/GEIST_OFL.txt`), in Regular, Medium, SemiBold and Bold weights.

| Style | Size / weight | Usage |
|---|---|---|
| `display` | 32 SemiBold, −0.03 em | tab titles |
| `headline` | 24 SemiBold | full-screen titles |
| `title` | 17 SemiBold | card and list titles |
| `body` / `bodyStrong` | 15 Regular / Medium | body text, buttons |
| `label` | 13 Medium | labels, chips |
| `caption` | 12 Regular | metadata |
| `overline` | 11 SemiBold, uppercase, +0.1 em | section headers |
| `numericHero` | 72 Medium, tabular figures | rest timer |
| `numericLarge` | 30 SemiBold, tabular figures | highlighted values, steppers |
| `numeric` | 15 Medium, tabular figures | inline numbers |

Numbers always use tabular figures (`tnum`), so they don't "jump" while the timer runs. Geist Mono was tested and discarded: its decimal comma is too wide and its zero is slashed.

## Shape and spacing

- 4 dp grid; 20 dp horizontal screen margin; content capped at 640 dp on large screens.
- Corners: 10 dp (small), 16 dp (cards), 24 dp (dialogs); pill-shaped buttons and chips.
- Flat cards: 1 dp border, no shadow.
- Touch targets: 48 dp minimum, and 56 dp on workout controls (steppers).

## Motion

150–250 ms, standard easing, no bounce. Animate only what communicates state: rest progress, content entering and leaving, selection.

## Components

`ui/components`: `MyFitMark`, `MyFitWordmark`, `MyFitCard`, `PrimaryButton`, `SecondaryButton`, `TextAction`, `StatTile`, `NumberStepper`, `RestRing`, `SectionHeader`, `EmptyState`, `FilterChipRow` / `MyFitChip`, `ConfirmDialog`, `MyFitTopBar`, `TabHeader` / `TabScreen`.
Visual catalog: `ComponentCatalogScreenshotTest` (the PNGs are written to `app/build/outputs/roborazzi/`).

## Do / avoid

- Do: plenty of negative space, one primary action per screen, large numbers readable from a distance.
- Avoid: gradients, shadows, illustrations of people or dumbbells, more than one accent color, colored icons.
