---
name: QuickCards
description: Daylight v1.1. A calm, light-first card vault: jewel-colour card art on quiet neutral chrome, one red action per screen, and the card number one tap from the clipboard.
colors:
  brand: "#C8202B"
  on-brand: "#FFFFFF"
  on-art-danger: "#FFCBC2"
  on-art-chip: "rgba(0,0,0,.30)"
  light-bg: "#F4F6F9"
  light-surface: "#FFFFFF"
  light-surface-hi: "#FFFFFF"
  light-tonal: "#EEF1F5"
  light-tonal-2: "#E3E7ED"
  light-line: "#E3E7ED"
  light-outline: "#7A8392"
  light-ink: "#0D1117"
  light-ink-2: "#4F5866"
  light-ink-3: "#9AA1AC"
  light-ink-ph: "#6B7482"
  light-accent: "#1F4FE0"
  light-danger: "#B42318"
  light-danger-bg: "#FCEDEB"
  light-ok: "#1B7F4B"
  light-warn: "#9A5B00"
  light-select: "#0D1117"
  light-on-select: "#FFFFFF"
  light-inverse: "#151A22"
  light-on-inverse: "#F2F4F7"
  light-inverse-accent: "#A9BFFF"
  light-scrim: "rgba(13,17,23,.36)"
  light-state: "rgba(13,17,23,.08)"
  dark-bg: "#0B0D11"
  dark-surface: "#14171D"
  dark-surface-hi: "#1A1E25"
  dark-tonal: "#1C2028"
  dark-tonal-2: "#262B34"
  dark-tonal-hi: "#2A2F39"
  dark-tonal-hi-2: "#343A45"
  dark-line: "#232831"
  dark-outline: "#6B7482"
  dark-ink: "#F2F4F7"
  dark-ink-2: "#9AA3B0"
  dark-ink-3: "#4F5764"
  dark-ink-ph: "#808895"
  dark-accent: "#8FAEFF"
  dark-danger: "#FF8A7A"
  dark-danger-bg: "#3A1714"
  dark-on-danger: "#1A0503"
  dark-ok: "#6FD3A0"
  dark-warn: "#F2B45C"
  dark-select: "#F2F4F7"
  dark-on-select: "#0B0D11"
  dark-inverse: "#E6E9EE"
  dark-on-inverse: "#0D1117"
  dark-inverse-accent: "#1F4FE0"
  dark-scrim: "rgba(0,0,0,.6)"
  dark-state: "rgba(242,244,247,.10)"
  hue-navy: "#0B1D57 → #2B4EC2"
  hue-ocean: "#04283F → #0F5D8B"
  hue-teal: "#032F36 → #0A616B"
  hue-forest: "#04362C → #0B644D"
  hue-moss: "#172509 → #43611F"
  hue-olive: "#22240A → #565C1D"
  hue-burnt-orange: "#3F1A03 → #8C4409"
  hue-rust: "#3A1208 → #983A1C"
  hue-crimson: "#3E0709 → #A3202A"
  hue-wine: "#46081C → #A81A43"
  hue-magenta: "#3A0A31 → #9B2580"
  hue-plum: "#2A0E3D → #6E2C9A"
  hue-violet: "#1F0D45 → #683AC0"
  hue-indigo: "#140F4A → #4338B8"
  hue-slate: "#141B26 → #3E4C61"
  hue-graphite: "#2A2D33 → #535963"
typography:
  f5:
    fontFamily: "Geist"
    fontSize: "24sp"
    lineHeight: "32sp"
    fontWeight: 600
    letterSpacing: "-0.02em"
  f4:
    fontFamily: "Geist"
    fontSize: "20sp"
    lineHeight: "28sp"
    fontWeight: "500-600"
  f3:
    fontFamily: "Geist"
    fontSize: "15sp"
    lineHeight: "20sp"
    fontWeight: "400-600"
  f2:
    fontFamily: "Geist"
    fontSize: "13sp"
    lineHeight: "20sp"
    fontWeight: "400-600"
  f1:
    fontFamily: "Geist"
    fontSize: "11sp"
    lineHeight: "16sp"
    fontWeight: "500-600"
  mono:
    fontFamily: "Geist Mono"
    fontSize: "f4 / f5"
    fontWeight: 500
    letterSpacing: "0.04em"
    fontFeature: "tnum"
rounded:
  xs: "4dp"
  sm: "8dp"
  md: "12dp"
  lg: "20dp"
  xl: "28dp"
  full: "999dp"
spacing:
  s1: "4dp"
  s2: "8dp"
  s3: "12dp"
  s4: "16dp"
  s5: "20dp"
  s6: "24dp"
  s8: "32dp"
  s10: "40dp"
  s12: "48dp"
  s16: "64dp"
  hit: "48dp"
  field: "48dp"
  bar: "64dp"
  row: "56dp"
  row-2: "64dp"
  nav: "80dp"
components:
  button-filled:
    backgroundColor: "{colors.brand}"
    textColor: "{colors.on-brand}"
    rounded: "{rounded.full}"
    minHeight: "36dp (lg 44dp)"
    elevation: "0"
  button-tonal:
    backgroundColor: "{colors.light-tonal}"
    textColor: "{colors.light-ink}"
    rounded: "{rounded.full}"
    minHeight: "36dp (lg 44dp)"
  button-outlined:
    border: "1dp {colors.light-outline}"
    textColor: "{colors.light-ink}"
    rounded: "{rounded.full}"
    minHeight: "36dp (lg 44dp)"
  button-text:
    textColor: "{colors.light-accent}"
    rounded: "{rounded.full}"
    minHeight: "36dp"
  text-field:
    backgroundColor: "{colors.light-surface}"
    border: "1dp {colors.light-outline}"
    rounded: "{rounded.md}"
    minHeight: "48dp"
  card-tile:
    width: "328dp (phone)"
    rounded: "{rounded.lg}"
    padding: "16dp"
  card-hero:
    minHeight: "207dp (ISO ID-1 at 328dp)"
    rounded: "{rounded.lg}"
  status-chip:
    backgroundColor: "{colors.on-art-chip}"
    textColor: "#FFFFFF"
    typography: "{typography.f1}"
    rounded: "{rounded.xs}"
    minHeight: "20dp"
  copy-pill:
    rounded: "{rounded.full}"
    minHeight: "32dp"
    minWidth: "88dp"
  group:
    backgroundColor: "{colors.light-surface}"
    rounded: "{rounded.lg}"
  list-row:
    minHeight: "56dp (two-line 64dp)"
    padding: "8dp 16dp"
  banner:
    rounded: "{rounded.lg}"
    minHeight: "56dp"
  sheet:
    backgroundColor: "{colors.light-surface-hi}"
    rounded: "{rounded.xl}"
    sideInset: "24dp"
  dialog:
    backgroundColor: "{colors.light-surface-hi}"
    rounded: "{rounded.xl}"
    padding: "24dp"
    maxWidth: "560dp"
  snackbar:
    backgroundColor: "{colors.light-inverse}"
    textColor: "{colors.light-on-inverse}"
    rounded: "{rounded.md}"
    minHeight: "48dp"
  nav-bar:
    height: "80dp"
    indicator: "56 × 32dp"
  nav-rail:
    width: "80dp"
---

# Design System: QuickCards

**Visual spec:** `prototype/inspo/10-daylight.html` (Daylight v1.1, locked). Open one frame with `?shot=<id>&theme=light|dark` (`&fs=2` previews 200% text). Its Foundations, Colour, Adaptive and Components sections are the visual source of truth; this file is the reference summary. Behaviour stays `prototype/v2.html` until the app ships Daylight. Decisions per round: `docs/agents/design/daylight/ROUND2_DECISIONS.md`, `ROUND8_OWNER.md`; strings to change: `docs/agents/design/daylight/STRINGS_PLAN.md` (local).

## Overview

**Daylight.** Quiet neutral chrome, light first, with the card art as the only colour that belongs to content.

| Trait | Rule |
|---|---|
| Ground | Light: cool grey background, white groups. Dark: near-black with tonal steps. Never a black (OLED) theme. |
| Colour | Card art carries hue; chrome is neutral ink. Brand red = one filled CTA per screen. Accent blue = text buttons, links, caret, focus. |
| Copy model | Home tile Copy pill copies the number only; tap a card = Card details (every value + Copy, hold number/CVV to reveal); hold a card = card actions sheet. |
| Feedback | In-place "Copied" + TalkBack announcement for single-value copies; snackbar only for Undo, Clear, errors and actions without in-place feedback. |
| Controls | Stock Material 3 shapes on the token scale; filled Material Symbols Rounded icons in lists and rows. |
| Motion | Three durations; nothing moves on its own. |

## Colors

### Roles (ratios measured; "vs" = the background it is checked on)

| Token | Role | Light | Ratio | Dark | Ratio | Check |
|---|---|---|---|---|---|---|
| `bg` | App background | #F4F6F9 | · | #0B0D11 | · | |
| `surface` | Groups, fields, detail rows | #FFFFFF | · | #14171D | · | |
| `surface-hi` | Sheets, dialogs, menus | #FFFFFF | · | #1A1E25 | · | |
| `tonal` / `tonal-2` | Tonal buttons, copy discs / pressed, selected | #EEF1F5 / #E3E7ED | · | #1C2028 / #262B34 | · | on raised surfaces dark uses `tonal-hi` #2A2F39 / #343A45 |
| `line` | Hairlines | #E3E7ED | · | #232831 | · | |
| `outline` | Field and control borders | #7A8392 | 3.83 | #6B7482 | 3.80 | vs surface ≥ 3 |
| `ink` | Text, icons | #0D1117 | 17.48 | #F2F4F7 | 17.65 | vs bg ≥ 7 |
| `ink-2` | Secondary text, row icons | #4F5866 | 7.19 | #9AA3B0 | 7.04 | vs surface ≥ 4.5 |
| `ink-3` | Disabled (exempt) | #9AA1AC | · | #4F5764 | · | |
| `ink-ph` | Placeholder text | #6B7482 | 4.72 | #808895 | 5.02 | vs surface ≥ 4.5 |
| `accent` | Text buttons, links, caret, focus | #1F4FE0 | 6.46 | #8FAEFF | 8.26 | vs surface ≥ 4.5 |
| `brand` | Filled CTA (white text) | #C8202B | 5.68 | #C8202B | 5.68 | vs on-brand ≥ 4.5 |
| `danger` / `danger-bg` | Errors, destructive / error banner tint | #B42318 / #FCEDEB | 6.57 | #FF8A7A / #3A1714 | 7.84 | vs surface ≥ 4.5 |
| `ok` | Success | #1B7F4B | 5.02 | #6FD3A0 | 9.83 | vs surface ≥ 4.5 |
| `warn` | Warnings (Expired in the form, Not set up) | #9A5B00 | 5.43 | #F2B45C | 9.79 | vs surface ≥ 4.5 |
| `select` / `on-select` | Switch on, checkbox, radio, selected chip | #0D1117 / #FFFFFF | 17.48 | #F2F4F7 / #0B0D11 | 17.65 | vs bg ≥ 3 |
| `inverse` / `on-inverse` | Snackbar surface / text | #151A22 / #F2F4F7 | 15.85 | #E6E9EE / #0D1117 | 15.55 | ≥ 7 |
| `inverse-accent` | Snackbar action | #A9BFFF | 9.63 | #1F4FE0 | 5.30 | vs inverse ≥ 4.5 |
| `scrim` | Behind sheets and dialogs | rgba(13,17,23,.36) | · | rgba(0,0,0,.6) | · | |
| `state` | Pressed state layer | ink 8% | · | ink 10% | · | |

### On card art

| Token | Value | Use |
|---|---|---|
| white | #FFFFFF | Primary text and the network mark on art |
| `t2` | white 80% | Secondary text on art (≥ 4.5:1 on every hue) |
| `on-art-chip` | black 30% | The one status-chip fill on art |
| `on-art-danger` | #FFCBC2 | Expired date and the Expired chip ring (7.78:1 on graphite) |

### Named rules

- One filled brand-red button per screen. Destructive confirms use `danger`, and nothing else on that dialog is red.
- Selection is neutral (`select`), never brand or accent.
- Placeholders use `ink-ph`, never `ink-3` (which is the exempt disabled tone).
- Every colour comes from a token; card art may use white and white alphas.

## Typography

Geist 400/500/600 and Geist Mono 500 (numbers only), bundled in `res/font` (no downloadable fonts, AGENTS rule 1). Units are sp. Tracking: −0.02em titles, +0.04em numbers, +0.12em masked dots.

| Token | Size / line | Weight | Use | Compose |
|---|---|---|---|---|
| `f5` | 24 / 32 | 600 | Screen title, hero number | `headlineSmall` |
| `f4` | 20 / 28 | 500–600 | Tile number, sheet and dialog title, nested bar title | `titleLarge` |
| `f3` | 15 / 20 | 400–600 | Body, row title, bank, field value | `bodyLarge` |
| `f2` | 13 / 20 | 400–600 | Secondary, labels, buttons, chips, group labels | `bodyMedium` |
| `f1` | 11 / 16 | 500–600 | Captions, nav labels, help, tags, status chips | `labelSmall` |
| mono | f4 / f5 | 500 | Card number, expiry, CVV, masked dots | Geist Mono, `tnum` |

## Layout

- **Spacing:** 4dp grid, tokens `s1` 4 · `s2` 8 · `s3` 12 · `s4` 16 · `s5` 20 · `s6` 24 · `s8` 32 · `s10` 40 · `s12` 48 · `s16` 64. Paddings, margins and gaps use only these (or 0).
- **Defaults:** screen edge `s4` · tile and row inner `s4` · list gap `s3` · section gap `s6` · icon-to-text `s1`–`s2` · sheet side inset `s6` · editor block rhythm `s3`.
- **Sizes:** touch target 48 (invisible hit areas) · field 48 · top bar 64 · row 56 / two-line 64 · nav bar 80 · icons 24 (bars, rows), 20 (menus, fields, banners), 18 (pills, discs), 16 (pin mark).
- **Phone frame:** 360 × 780 reference; card tile 328 wide; hero 207 tall (ISO ID-1). On Card details the hero fills the rows' content width (380 on a 412 phone) at the ISO ratio, so card and rows share both edges; the column cap (600) caps it on wider windows.
- Controls size from content: buttons, chips, segments, pills, status chips and fields use a minimum height, never a fixed one, so 200% text grows them.

### Alignment rules

| Rule | Value |
|---|---|
| Group label left edge = the rounded group's outer left edge (Settings, Manage, sub-pages, tablet two-pane) | padding-left 0 |
| Status chip: one slot on the subtitle line, right-aligned, vertically centred; its right edge = the network mark's artwork right edge = the Copy pill's right edge (tile, hero, form preview, adaptive) | Δ 0 px |
| Leading checkbox / radio to its label | `s3` 12 · box 20 · hit 48 |
| Inline field action (e.g. "Add") lives inside the field as a trailing text button; an action outside a field matches its height and radius | field 48 |
| Masked number: compact surfaces (tile, row, sheet header, dialog, notification, autofill) "••0366"; hero, detail rows and form preview "••••0366". Hide card numbers off: tile shows the grouped number in mono f3, hero in mono f4, the Number row in mono f3; all else stays masked | 2 / 4 dots + last 4 |
| Pin mark sits after the bank name on the tile and hero | `push_pin` filled 16, white 90% |
| Editors: one vertical rhythm between every block (preview, field, field row, segmented control, checkbox list, banner); a checkbox group is a plain list of 48 rows with the box on the field edge, no container | `s3` 12 |

## Elevation & Depth

| Level | Use | Light | Dark |
|---|---|---|---|
| `e1` | Groups, fields | 1–2 px soft shadow + 1dp `line` ring | 1dp `line` ring only |
| `e2` | Menus, autofill | soft 24 px shadow | tone (`surface-hi`) + 6% white ring |
| `e3` | Sheets, dialogs | soft 48 px shadow | tone + 6% white ring |
| art glow | Card art | coloured shadow, card hue at 45% | same, deeper |

Dark theme lifts with tone (`surface` → `surface-hi`); shadows only under floating layers. Filled buttons have no shadow.

## Shapes

| Token | Value | Use |
|---|---|---|
| `r-xs` | 4 | Checkbox, mini art, status chips |
| `r-sm` | 8 | Tags, colour swatches |
| `r-md` | 12 | Fields, menus, snackbar |
| `r-lg` | 20 | Card art, groups, banners |
| `r-xl` | 28 | Sheets, dialogs, notifications |
| `r-full` | pill | Buttons, chips, Copy pill |

Nested shapes are concentric: inner radius = outer radius − inset (`nestedCorners` in `Shape.kt`).

## Card Art

### Hues

Sixteen colours, keys = the `form_band_*` keys stored on cards. Each is a 135° gradient from the deep stop `c1` to the light stop `c2`; white text is ≥ 7:1 on `c2` (AAA for digits). A custom colour must pass the same 7:1 guard ("Too light. Pick a darker shade." disables "Use colour").

### Pattern follows colour

Deterministic, never random. Each colour has one pattern, assigned along the palette order so neighbouring hues differ. Same colour = same pattern; changing a card's colour changes its pattern.

| # | Colour | c1 → c2 | White on c2 | Pattern |
|---|---|---|---|---|
| 1 | Navy | #0B1D57 → #2B4EC2 | 7.07 | rings |
| 2 | Ocean | #04283F → #0F5D8B | 7.10 | rosette |
| 3 | Teal | #032F36 → #0A616B | 7.15 | contours |
| 4 | Forest | #04362C → #0B644D | 7.13 | lines |
| 5 | Moss | #172509 → #43611F | 7.07 | weave |
| 6 | Olive | #22240A → #565C1D | 7.13 | contours |
| 7 | Burnt orange | #3F1A03 → #8C4409 | 7.13 | rosette |
| 8 | Rust | #3A1208 → #983A1C | 7.10 | lines |
| 9 | Crimson | #3E0709 → #A3202A | 7.51 | weave |
| 10 | Wine | #46081C → #A81A43 | 7.26 | lines |
| 11 | Magenta | #3A0A31 → #9B2580 | 7.07 | rings |
| 12 | Plum | #2A0E3D → #6E2C9A | 8.43 | weave |
| 13 | Violet | #1F0D45 → #683AC0 | 7.11 | rosette |
| 14 | Indigo | #140F4A → #4338B8 | 8.41 | contours |
| 15 | Slate | #141B26 → #3E4C61 | 8.71 | rings |
| · | Graphite (and every expired card) | #2A2D33 → #535963 | 7.05 | lines |
| · | Custom | from hue + lightness | ≥ 7 | FNV-1a 32-bit of the hex (light stop, upper case, no #) mod 5 over [rings, lines, contours, rosette, weave] |

Compose: `QcCardArt.patternFor(band)` / `patternFor(hex)`.

### Layers

1 linear 135° c1 → c2 · 2 radial white 20% at top-left · 3 stroke pattern 1dp white 4–16%, halved over the lower half (≤ 8% under the digits; on the hero it fades to 25% under the number band) · 4 grain 14% overlay · 5 glow shadow in the hue · 6 inner edge light/dark 1dp.

### Bank default colours

Every bank has a default colour; graphite is never a default. Picking a bank in Add card applies its colour unless the card's colour was chosen by hand; the override stays with the card, and the "Bank colour" chip in the Colour sheet resets it.

| Bank | Colour | Bank | Colour |
|---|---|---|---|
| HDFC Bank | Navy | AU Small Finance Bank | Burnt orange |
| SBI Card | Ocean | Federal Bank | Olive |
| ICICI Bank | Rust | Bandhan Bank | Moss |
| Axis Bank | Wine | Punjab National Bank | Forest |
| Kotak Mahindra Bank | Crimson | Canara Bank | Teal |
| IndusInd Bank | Plum | Union Bank of India | Slate |
| RBL Bank | Violet | American Express | Ocean |
| IDFC FIRST Bank | Magenta | HSBC | Crimson |
| Bank of Baroda | Rust | Standard Chartered | Teal |
| Yes Bank | Indigo | Citibank | Navy |
| Chase | Indigo | Capital One | Slate |
| Bank of America | Wine | Barclays | Ocean |
| Wells Fargo | Burnt orange | DBS Bank | Crimson |

Aliases (hdfc, sbi, state bank of india, amex, citi …) resolve to the same bank (`BankCatalog.aliases`).

**Auto-pick for a new bank:** palette order = navy, ocean, teal, forest, moss, olive, burnt orange, rust, crimson, wine, magenta, plum, violet, indigo, slate (graphite excluded). Count the colours used by every bank in the list (built-in and user, one entry per bank). Pick the first colour in that order used by none ("Not used by another bank"); if all are used, pick the least-used colour, ties to the earlier one.

### Expired and empty

- **Expired** (after the last day of the expiry month): the card renders in graphite with its lines pattern, the stripes fade above the digits band, the date turns `on-art-danger`, and the status chip reads "Expired". Text keeps full contrast; every action keeps working.
- **Empty form preview:** graphite, "New card", "••••0000", "MM/YY".
- **Empty vault sample:** a faded jewel card (pattern visible), "Your bank", "••0000".

### Network marks

| Rule | Value |
|---|---|
| Tone | White on card art (white fills knock out). On surfaces full colour in both themes, except wordmarks unreadable on dark (Visa, RuPay, Diners, Discover, Mir), which go white there. Box marks (Amex, JCB, UnionPay) stay full colour on every surface; Mastercard and Maestro are always full colour, on art too. |
| On the card | 56 × 28 canvas, logo in 48 × 20; the artwork's right edge sits on the content edge. No plate. |
| Lists, fields, autofill | Fixed 40 × 24 slot, the logo's own bounds fitted (24 tall, wide marks up to 40 wide), same for all ten |
| Mini card | 20 × 12 |
| Never | Bank logos |

Drawables: `net_<key>` (colour) plus `net_<key>_mono` for the eight white-on-art networks (Visa, RuPay, Amex, Diners, Discover, JCB, Mir, UnionPay).

## Components

### Card tile (Home)

Header grid: row 1 bank (+ pin) | network mark; row 2 type · holder | status chip. Then the note (Notes on cards on: ≤ 2 lines, ellipsis, nothing else), then the number "••0366" and expiry with the Copy pill on the right. Tap = Card details; hold (500 ms, or the context-menu key) = white 10% pressed layer, then the card actions sheet. The tile and the pill are separate focus targets. A due bill adds a strip "Due in N days" + "Mark paid" at the foot of the card: 32 below the digits row, an even 8 gap to the card's sides and bottom (the strip bleeds 16 − 8 into the tile padding), min 48, white 12% with a white 20% ring. Corners are concentric with the card: strip radius = `r-lg` 20 − the real gap 8 = 12, so the band stays 8 along the sides, around both bottom curves and along the bottom. Mark paid sits 4 inside the strip (40 visible, 48 hit) at 12 − 4 = 8.

### Copy pill

Pill, min 32 tall, min 88 wide, 48 hit. Idle: copy icon + "Copy" (`f2`/500) on white 14% with a white 26% inner ring. Copied: white fill, text in the card hue, check + "Copied", polite announcement ("Card number copied"). No snackbar.

### Status chip

One component for Check, Expires soon, Expired, Last used, Archived (and "Archived · reason"; inside Manage › Archived the chip is the reason alone, and the card keeps its own colour): text only, no icon, `f1`/600 white on `on-art-chip`, 1dp white 30% inner ring (Expired: `on-art-danger` ring), `r-xs`, min height 20. One chip per card, priority Archived > Expired > Check > Expires soon > Last used. Same slot and style on tiles, the details hero, Archived, adaptive panes and the gallery. Check reads "Check details" to TalkBack.

### Card details

Hero card (4 dots, status chip, pin) then a group of rows: Number, Expiry, CVV, Holder, Note, each with a Copy disc. Copied row: value tinted in the hue, filled check disc, "Copied" left of the disc (shown only where it costs the value no line; values otherwise run to the disc). Holding Number or CVV reveals it for 5 s with a 2dp draining line under the row (the only timed line). Bar: back, Edit, ⋮ (card actions). Invisible level-1 heading "HDFC Bank card ending 0366".

### Buttons

| Variant | Look | Use |
|---|---|---|
| Filled | Brand red, white text, no shadow | The one primary action per screen |
| Tonal | `tonal` fill, ink text (steps to `tonal-2` on the app background) | A lone sheet footer action (Done) |
| Outlined | 1dp `outline` (3.8:1), ink text, transparent | Secondary in sheet footers (Cancel beside a filled primary) |
| Text | Accent text | Dialog actions, inline actions, banner actions |
| Text danger | `danger` text | Destructive dialog confirm |
| Disabled (any variant) | Content ink 38%, container/outline ink 12%, no shadow, no state layer, disabled semantics | Save until valid, Export until rules pass, Erase until ticked, Pin at the limit |

Min height 36 (large 44), `r-full`, pressed = currentColor 12% layer. Full-width footer buttons may wrap. A disabled primary in an editor shows one `f2` `ink-2` line above it naming what is missing.

### Inputs

Outlined field, min 48, `r-md`, 1dp `outline`; focus 2dp accent; error = `danger` outline + help line tied to the field; warning = help in `warn` only (never blocks). Placeholders in `ink-ph`, mono for number, expiry and CVV. Password, CVV and PIN fields mask with real bullets (Geist Mono, same size as masked digits). Picker fields (Holder, Bank, Type, Colour) are buttons that open the picker sheet ("Holder, Diya Mehta, double-tap to choose"). Search field: only the outer pill shows state; no inner border or ring. Keyboard: number, expiry, CVV and due day show Next (number → expiry → CVV → due day → note); the note shows Done.

### Lists and groups

`group` = `surface`, `r-lg`, `e1`; rows min 56 (two-line 64), 16 side padding, hairline between rows, leading icon 24 `ink-2`, chevron on rows that open a sheet or page. Group labels `f2`/600 `ink-2` on the group's outer edge. Danger row (Erase all data) stands alone in `danger`.

### Icons

Material Symbols Rounded, weight 400, grade 0, optical size 24, one family. **Filled (FILL 1)** for every list, row and leading icon in Settings, Manage, sheets, dialogs, banners and pickers; outlined for top-bar actions and in-field icons. Navigation bar per M3: selected filled, unselected outlined. Row icons `ink-2`.

| Row | Icon | Row | Icon |
|---|---|---|---|
| Theme | contrast | Instant copy | touch_app |
| Hide card numbers | visibility_off | Expiry reminders | event_upcoming |
| Notes on cards | notes | Payment reminders | payments |
| Tags on cards | sell | Export backup | upload |
| Add cards by tapping | contactless | Import cards | download |
| Expired cards | event_busy | Version | info |
| Expires soon | hourglass_empty | Privacy policy | shield |
| Clear clipboard | content_paste_off | Erase all data | delete |
| Expiry and CVV notification | notifications | Holders | person |
| Autofill | password | Banks | account_balance |
| App lock | lock | Networks | credit_card |
| Lock now | lock_clock | Types | style |
| Auto-lock | timer | Tags | sell |
| Lock when you leave | phonelink_lock | Archived | inventory_2 |

### Overlays

| Overlay | Use it for | Examples |
|---|---|---|
| Bottom sheet | Choosing or acting: picking from a list, acting on an object | Card actions, holder/bank/type/colour pickers, filters, tags, import choice, every single choice (Theme, Expired cards, Expires soon, Clear clipboard, Notification time, Auto-lock, Instant copy), Help |
| Dialog | Interrupting to confirm or acknowledge; the decision blocks the flow | Delete, erase, discard changes, NFC off, key unavailable, not a backup, missing columns, share, archive, export/import password |
| Snackbar | Transient feedback after an action, at most one action | Undo, Clear, Try again |
| Inline banner | A persistent screen state the user can fix | No screen lock, formats repaired, custom network rules |

- **Sheet:** handle zone · 24 side inset · text-only title block (bank + "••0366", then type · holder; never a card image) · content · footer via `QcSheetActions`: full-width tonal when alone, filled primary + outlined secondary when paired, 24 below the content, 16 above the gesture area. Sheets hug their content: no fixed or min heights, no spacers. Single-choice sheets are radio rows that apply on tap and close (no footer). Every sheet closes by handle drag, scrim and Back.
- **Dialog:** 24 padding · title · 16 · body · 24 · text actions right; no icon; destructive confirm in `danger`; checkbox lines follow the 12 gap. Max height = window − 2 × (status bar + 24); title and actions fixed, body scrolls with hairlines. Expanded windows: 560 wide.
- All overlays go through `QcDialog` / `QcOverlays` (AGENTS rule 4).

### Banner

One component for every inline banner: tonal container (`danger-bg` for errors, where only the icon is tinted and the text stays ink), `r-lg`, min 56, one row vertically centred: icon 20 · one line `f3`/600 · trailing text-button action(s) or a 48 close icon button ("Dismiss"). At 200% text the actions drop below, right-aligned; text never wraps away from its icon.

| Banner | Text | Actions |
|---|---|---|
| No screen lock | "Set a screen lock to copy" | "Settings" (TalkBack "Open screen lock settings") |
| Repair | "3 fixed · 1 to check" | "Review" + close icon "Dismiss" |
| Custom network rules | "Custom rules" | "Reset" (TalkBack "Reset Visa to standard rules") |

### Snackbar

M3 snackbar on `inverse`: one line of text, left-aligned, no icon, no image, optional text action in `inverse-accent`. Min 48, 16 inset, `r-md`, full width − 16 each side, 8 above the nav bar or gesture area. Shown 3 s (4.5 s with TalkBack); one with an action (Undo, Clear, Restart) always 4.5 s so the action can be reached. Used for Undo (Card deleted, Card archived, Card restored, Paid), Clear (Details copied, Note copied), errors (Try again), "Number pasted", "Update ready" + Restart, "Copying locked". Never for a single-value copy.

### Notifications

Android template only: small app icon, app · time, title "HDFC Bank ••0366", text, action buttons where the notification has them. No card thumbnails. Copy notification: "Expiry 12/29 · CVV 381" (the card's real values, in the shade only), no action buttons; it is hidden entirely on the lock screen. Reminders: "Due 22 Nov" with a "Paid" action; "Expires in 30 days"; on the lock screen "QuickCards" / "Card reminder"; a tap opens that card's details once the app is unlocked. Small icon tinted brand red.

### Navigation and bars

Top bar 64: screen title `f5` (level-1 heading) or back + `f4` title for nested screens; actions are 48 icon buttons. Home bar: title, search, filter, +. Bottom bar 80 on `bg` with a `line` hairline above, three items (Cards, Manage, Settings): 56 × 32 indicator in `tonal-2` behind the selected (filled) icon, `f1` label in `ink` (600) when selected, `ink-2` (500) otherwise. Settings bar has an ⓘ that opens Help.

### Selection controls

Switch, checkbox, radio in `select` / `on-select`; the whole row toggles; 48 hit. Filter chips lead with a check 18 when selected; holder chips are single-select (no check). The Filter sheet: title "Filter" with Clear all (accent text button) in its header, then each group's label (`f2` 600, `ink-2`) over wrapping 32 chips (8 apart, `surface` with a `line` outline; `select` fill when on); only Status chips show their count (72%); the lone footer action "Show N cards" is tonal. A disabled list row dims as a whole (icon, text, trailing control) to 38%, and one `f2` `ink-2` line under its group says why (App lock off: "Turn on App lock to use Auto-lock and Lock when you leave").

### Colour picker

Bottom sheet "Colour": live preview card on top, "Bank colour" chip in the header (on while the card follows its bank), 4-column grid of 64 × 40 card swatches (`r-sm`, pattern hint, name `f1` `ink-2` below), selected = 2dp gap + 2dp ink ring + check; the bank's default swatch carries a white bank glyph; last tile "Custom" opens hue and lightness strips (a tick marks the 7:1 limit), Hex and Recent, with "Use colour"; its ← and system Back return to the grid. Footer "Done".

### Empty states and tips

Empty vault: faded sample card, "Add your first card", one line, full-width filled "Add card" at the content bottom, "Import" and "Tap a card" (NFC phones) as equal tonal buttons in one row; search hidden, + kept, nav kept. First-run tips: 3 pages with Skip and page dots, last page filled "Add card". No matches: title, "Try a bank, holder, tag or digits", "Clear search". Loading: Home keeps its chrome (title, search, +, nav) over three static `tonal` blocks in the tile shape (`r-lg`, no shimmer); Add/Edit shows the form labels over `tonal` field blocks and the Save footer, off, until the form is ready. Never an empty-vault look while loading.

### Motion

| Token | Value | Use |
|---|---|---|
| `dur-state` | 150 ms | Copy → check + "Copied", press layers, switch |
| `dur-nav` | 200 ms | Screen fade + 16dp slide, sheet and dialog in |
| `dur-toast` | 3000 ms | Snackbar on screen (4.5 s with TalkBack, and always for a snackbar with an action) |
| `ease` | cubic-bezier(.2, 0, 0, 1) | Emphasized decelerate |

Nothing moves on its own: no tilt, particles, rolling digits or flips. Reduced motion (targeted, not a global kill): no slides, no switch-thumb travel, crossfades stay. Masking is instant.

### Accessibility

- Every screen title is a level-1 heading; sheet and dialog titles and section labels level 2 (labels inside a sheet level 3).
- Copy announces "Card number copied" / "Expiry copied" / "CVV copied" (polite). The pill is named "Copy HDFC Bank card number ending 0366".
- Tile: "HDFC Bank, ending 0366, Due in 2 days"; long-press label "Card actions"; TalkBack custom action for card actions; note custom action "Copy note".
- Every switch, chip and icon target ≥ 48 × 48. A disabled row says why in its own line.

## Adaptive

| Class | Width | Navigation | Cards | Card details | Add / Edit | Settings |
|---|---|---|---|---|---|---|
| Compact | < 600 | Bottom bar | 1 column of tiles | Full screen, stacked | Full screen | 1 pane, sub-pages push |
| Medium | 600–839 | Rail | 2-lane staggered grid | List-detail: list pane 328 \| detail | Dialog 560 | 1 pane, max 600 |
| Expanded | ≥ 840 | Rail | List pane 360 of tiles | Detail pane: card \| rows, max 840 | Dialog 560 | Two-pane: groups 360 \| page |
| Height < 480 | phone landscape | Rail | Holder chips move into the top bar | Card left, rows right (56) | Full screen, column max 600; Save rides on the keyboard | 1 pane |

- One navigation at a time: never bar + rail. Rail 80 wide, items top-aligned, same 56 × 32 indicator; + stays in the list pane's top bar.
- Staggered grids keep list order in reading and focus order; each tile goes to the shorter lane.
- Foldables: book posture = list | detail, each pane ends at the hinge; tabletop = card on the top half, rows and actions below; hinge zone 24, nothing crosses it. Read the hinge from the window, never the device model; a separating hinge forces two panes even at medium width.
- Max widths (Dimens): rail 80 · list 360 · list (medium, fold) 328 · page 600 · dialog 560 · detail 840.
- With the keyboard open, Save rides on the IME and the focused field, its help line and the next field row scroll into view above it (re-checked as the keyboard settles; a short viewport keeps the focused field's top).

## Compose mapping

| Design | Compose |
|---|---|
| `bg` / `surface` / `surface-hi` | `colorScheme.background` / `surface` / `surfaceContainerHigh` |
| `tonal` / `tonal-2` | `surfaceContainer` / `surfaceContainerHighest` |
| `ink` / `ink-2` | `onSurface` / `onSurfaceVariant` |
| `outline` / `line` | `outline` / `outlineVariant` |
| `accent` | `primary` (text buttons, focus, caret) |
| `brand` | `QcColors.brand` (filled CTA only) |
| `danger` / `danger-bg` | `error` / `errorContainer` |
| `inverse` / `on-inverse` / `inverse-accent` | `inverseSurface` / `inverseOnSurface` / `inversePrimary` |
| `select` / `on-select` | `QcColors.select` (Switch, Checkbox, RadioButton colors) |
| `warn` | `QcColors.warn` (supportingText, `isError = false`) |
| `ink-ph` | TextField placeholder colour |
| `f1` … `f5` | `labelSmall`, `bodyMedium`, `bodyLarge`, `titleLarge`, `headlineSmall` (Geist via `res/font`) |
| `s1` … `s16` | `QcSpacing.s1` … `s16` (Dp) in `Dimens.kt` |
| `r-xs` … `r-xl`, `r-full` | `Shapes` extraSmall … extraLarge, `CircleShape` |
| `e1` / `e2` / `e3` | tonalElevation 0 / 2 / 3 dp + `Modifier.shadow` (light only) |
| Card hues | `QcCardArt.hues` (c1, c2, glow, pattern); `QcCardArt.patternFor(band \| hex)` |
| Pattern strokes | Canvas `drawCircle` / `drawPath`, 1.dp |
| Grain | Bundled 160 × 160 noise bitmap, `BlendMode.Overlay` 14% |
| Network mark tone | `Image(net_x)` or `Image(net_x_mono, colorFilter = ColorFilter.tint(White))`; list slot `Box(40 × 24.dp)` + `ContentScale.Fit` |
| Icons | Material Symbols Rounded, filled (`Icons.Rounded.*`) via `QcIcons.kt` |
| Snackbar | `SnackbarHost` + `Snackbar` (no icon), shape `r-md`, inverse colours |
| Sheet / dialog / menu | `QcModalBottomSheet` / `QcDialog` / `QcDropdownMenu` |
| Sheet footer | `QcSheetActions`: full-width `FilledTonalButton` / `Button` + `OutlinedButton`, 24.dp sides (screens 16.dp) |
| Durations | `tween(150)` / `tween(200, EmphasizedDecelerate)` |
| Long-press tile | `Modifier.combinedClickable(onLongClick, onLongClickLabel = "Card actions")` + `HapticFeedbackType.LongPress` |
| Copy announcement | `announceForAccessibility` or `LiveRegionMode.Polite` |
| Headings | `Modifier.semantics { heading() }`; details `paneTitle` |
| Picker fields | `Modifier.clickable(role = Role.DropdownList, onClickLabel = "Choose")` opening `QcPickerSheet` |
| Min sizes | `defaultMinSize` / `heightIn(min = …)`, `minimumInteractiveComponentSize()` |
| Last used / Recent | `CardEntity.lastCopiedAt`; Recent = top 3 by `lastCopiedAt` |
| Launcher shortcuts | Add card, Search: static `res/xml/shortcuts.xml`; Tap to add: dynamic via `ShortcutManagerCompat`, NFC phones only |
| Tips | `HorizontalPager(3)` + DataStore flag `tipsSeen` |
| Window class | `core/ui/adaptive/QcWindow.of(width, height, hinge)`: 600 / 840 width, 480 height (no adaptive library; minSdk 26) |
| Bar ↔ rail | `QcNavBars.kt`: `QcNavigationBar` below 600, `QcNavigationRail` (80, on `bg`, items top-aligned, 64 tall) from 600; both draw the same 56 × 32 indicator item from `QcTheme` tokens |
| List-detail, two-pane Settings | `ListDetailPanes(listWidth, hinge)`; medium opens `card/details` beside the list, so Back pops the detail pane; `SettingsScreen(twoPane)` |
| Hinge | `rememberHinge()`: WindowManager `FoldingFeature` (vertical, separating); panes end at a zone of max(24, hinge) |
| Staggered grid | `LazyVerticalStaggeredGrid` |
| Insets | `WindowInsets.safeDrawing`, `imePadding()`, `bringIntoViewRequester` |

## Do's and Don'ts

### Do:

- Take every colour, size, radius, type style and icon from the tokens and `core/ui/theme`.
- Keep one filled brand-red button per screen; make destructive confirms `danger`.
- Derive status, order, Last used and pattern from data; never pass them by hand.
- Put status in the one status-chip slot, by priority.
- Use a bottom sheet for every choice and every action on an object; use a dialog only to confirm or acknowledge.
- Give every error a fixing action and every sheet, step and editor a way out.
- Check both themes, 200% text and the adaptive classes against the visual spec.

### Don't:

- Don't add a black (OLED) theme, a plate behind network marks, or bank logos.
- Don't show a snackbar for a single-value copy, or an icon or image in a snackbar.
- Don't put an icon in a status chip or in a dialog, or a card image in a sheet header.
- Don't use random patterns, or a pattern that ignores the colour.
- Don't use radio dialogs, bare "Done" text, fixed sheet heights or spacers.
- Don't mix filled and outlined icons within rows.
- Don't fix a control's height; use minimum heights.
- Don't draw a ⋮ on the tile; hold opens card actions.
