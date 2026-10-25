# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Primary user: the phone's owner, managing payment cards for themselves and for other people they look after (several card holders, for example family members) on a personal Android phone. The recurring situation is needing a specific card's number, expiry, CVV, or note quickly — during an online checkout or while standing at a payment terminal, often one-handed — and getting back out. The job is retrieval and action, not account management or financial tracking.

Distribution is a signed APK published on GitHub Releases, not through an app store.

## Product Purpose

QuickCards is a local-first vault for the user's payment cards. Card details that would otherwise live in a wallet, photos, or a notes app are stored encrypted, opened instantly, and copied in a few taps. Success means the fastest possible path from opening the app to the exact value the user needs, with nothing leaving the device.

## Positioning

Fast daily card access on a device-local vault for one or several card holders. The differentiator is the combination: retrieval speed as the primary design goal (copy straight from the card list), while every sensitive value stays encrypted at rest, masked on screen, and gated behind one biometric or device-credential unlock that opens a short, visible instant-copy window. A cloud wallet cannot truthfully claim the offline guarantee; a plain notes app cannot claim authenticated, time-boxed reveals.

## Operating Context

- Android phone, `minSdk 26`; both orientations are permitted.
- Used in short, high-frequency sessions: open, find a card, copy a number or expiry, leave.
- The app opens to an app lock screen (configurable, on by default) and asks for the fingerprint or screen lock straight away; the Unlock button re-opens the prompt after a cancel, and the lock screen shows the app version. The app locks again after a configurable period without use (10 seconds to Never, default 1 minute) and when the user leaves the app (configurable, on). Opening the system file picker for an import or export, or the share sheet, does not count as leaving. The app appears in Recents like any other app (its preview is blanked by FLAG_SECURE in release builds).
- Copied values stay on the clipboard for the chosen clear time even after leaving the app, and are not hidden from the keyboard's clipboard preview; Clear or a new copy removes them earlier.
- One successful BiometricPrompt opens an instant-copy window (30 s, 1 min default, 2 min); inside it copy, reveal, edit and delete need no further prompt; export, import, erase and switching a protection off always ask again. The window and the app's inactivity lock both count idle time only: any touch, scroll or typing restarts them, so the app never locks or re-prompts while it is being used. There is no lock-state chip or countdown on the Cards screen; the times stay configurable in Settings and Lock now remains there.
- Card numbers are masked to the last four digits in the network's grouping on every screen except Edit (configurable). Tap copies the full value; a 450 ms hold reveals the number or CVV for five seconds.
- Card details can be scanned with the keyboard's own scan tool where the keyboard offers it: pasted or scanned text fills the number and expiry, never the CVV, and no camera permission is used.
- A card past the last day of its expiry month renders as a neutral graphite pass with an outlined "Expired" chip; it stays fully usable (copy, hold to reveal, edit, pin, archive, delete). A card expiring within the chosen 1, 2 or 3 months (default 1) shows a plain "Expires soon" label and is otherwise unchanged. Archived cards live in Manage › Archived cards, and a card's note can show on its pass. Settings › Card list holds those three choices.
- CVV is always masked by default and re-masks five seconds after a reveal.
- After a card number is copied, an optional heads-up notification shows that card's expiry and CVV for 30 seconds (configurable time; hidden on the lock screen, kept in the shade while the user switches apps). It has no buttons; values are copied from the card.
- Data moves between installs only through the user-initiated encrypted export/import in Settings, protected by a backup password.
- No network is required or used.

## Capabilities and Constraints

Confirmed functionality:

- Home is a list of "Pass Stub" cards: every card's number, expiry, CVV and name are one-tap copy targets on the list itself.
- Launcher, themed, notification, splash and lock-screen marks use the Stub Lock logo.
- The ten built-in networks (Visa, Mastercard, American Express, RuPay, Diners Club, Discover, JCB, Maestro, UnionPay, Mir) show their official full-colour mark on a standard white plate on the card, in Manage and in the card form; custom networks keep the text label.
- Card create and edit with card holder, card number, expiry, CVV, bank, optional card type, tags, card colour (fifteen curated bands or a custom colour that passes the white-text contrast guard) and note. The card holder is the only name: the pass shows the holder's name in capitals (the old "name on card" and "card name" fields are gone from the form; their stored values are kept but unused). The network always follows the number's first digits (longest prefix match) and shows only as a badge in the number field and on the pass; there is no network field or manual override. A number that matches no network shows no badge and is checked with the generic rule (12-19 digits, Luhn).
- Card details can be scanned with the keyboard's own scan tool where the keyboard offers it (Card number and Expiry use the text keyboard, with suggestions off, so the scan tool is available; CVV keeps the masked number pad); pasted or scanned text is cleaned and fills number and expiry (never CVV). No camera permission. A single input event that inserts two or more characters is parsed through the normal number rules (grouping, network detection, limits, validation); an expiry fills an empty Expiry field. Typing one character at a time is unchanged, junk text keeps the field's digit filter, and a fill shows "Card number filled from the pasted text" or "Card number and expiry filled from the pasted text".
- Legacy card data is repaired non-destructively, once on upgrade and on every import. Numbers keep digits only; expiry accepts MM/YY, M/YY, MM//YY, MM-YY, MM.YY, MM YY, MMYY, MM/YYYY, MMYYYY, YYYY-MM and YY/MM (only when the first part is over 12 and the second is a month); CVV keeps digits only at the network's length. Anything unparseable keeps its value and is flagged instead of failing. The first repair shows one dismissible Cards banner ("Fixed the format of N cards. M need a quick check.") with Review and Dismiss; Review turns on a "Needs check (M)" filter. Flagged cards show "Check details" on the pass and the field's error in Edit. Before values are kept encrypted for 30 days; a fixed field shows "Changed from …" with Undo. Past expiry dates are valid (expired cards are real data).
- Expired and expires-soon cards: a card is expired after the last day of its expiry month (local date) and renders as a neutral graphite pass — band, sheen, body and edge — with one outlined "Expired" chip and its `event_busy` icon on the card-number label row; text keeps full contrast and copy, hold-to-reveal, edit, pin, archive and delete all keep working. A card expiring within 1, 2 or 3 months (default 1) shows a plain "Expires soon" label only and does not change order. Settings › Card list › Expired cards chooses Keep in place, Move to the end (default) or Hide from the list; pinned expired cards stay pinned, and hidden cards still appear in search and when the Expired filter is on. The filter sheet has a Status group (Expired, Expires soon) with counts.
- Archive: a card's ⋮ sheet offers Archive above Delete. The dialog "Archive this card?" explains "It moves to Manage › Archived cards. You can copy, edit or restore it there." and takes an optional 40-character reason with the helper "For example: Card closed". Archiving unpins the card, shows "Card archived" with Undo, and moves it to Manage › Archived cards: a screen with the same passes, the search field "Search archived cards" (last 4, bank, holder, network, type, tags, note, reason), the empty state "No archived cards", the no-results state, and Restore, Edit, Copy details and Delete in each ⋮. Restore shows "Card restored". Archived cards also appear in the main search, labelled Archived. Archive state and reason travel in backups.
- Notes on the pass: Settings › Card list › Show notes on cards (default on) shows the note under the tags in body-small, up to two lines, with a 48dp More / Less text button only when it overflows (accessible names "Show full note" and "Show less"); expanding affects that card only and resets when leaving the screen. A 500 ms hold copies the note ("Note copied"). Notes stay searchable, appear in the ⋮ sheet header, and are shown in Archived cards.
- Card formats: 12 to 19 digit numbers with per-network grouping and masking (Visa 13/16/19, Mastercard 16, American Express 15 with 4-digit CVV, RuPay 16, Diners Club 14-19, Discover, JCB, Maestro, UnionPay without Luhn, Mir), Luhn check unless the network disables it.
- Managed lists in a Manage tab: card holders, banks, card networks (rules editable, custom networks allowed), card types and tags. Names are unique per list; renaming an item that cards use asks for confirmation and lists those cards; deleting an item in use is blocked until its cards are deleted.
- Card numbers are unique across all holders. Duplicates are rejected on add and edit, and matched on import, using a Keystore-backed HMAC hash.
- Search over digits, bank, holder, network, type, note and tags; filters by holder, bank, network, type, tag and status (Expired, Expires soon); holder chips on the home screen.
- Settings and card-menu behaviour: Theme is a normal Settings row opening a radio dialog (no segmented control); the card More sheet offers Copy details, Share (unlock first, then a dialog asking whether to include the CVV, off by default, then the system share sheet), Edit, Pin/Unpin and Delete; Settings rows that open a dialog end in a chevron; pinned cards stay first in pin order, carry a pin mark and are limited to 10; at the limit Pin is disabled in the card menu with a short note to unpin a card (no dialog, nothing is unpinned automatically) and are kept in backups; primary buttons stay brand red even with system colours, Save is disabled until an edit changes something; every input enforces its limits while typing; Card holder, Bank and Card type all use one bottom-sheet picker with search and "+ Add".
- Version is shown in Settings › About as "2.0.0" (the versionName; the internal versionCode is not shown).
- Settings: theme, system colours, hide card numbers, Card list (expired placement, expires-soon window, notes on cards), clipboard clear time, show expiry and CVV after copying (with its time), tags (show), app lock, inactivity lock time, lock on leave, instant-copy window, lock now.
- AES-256-GCM field encryption at rest for card number, expiry and CVV, and for holder, bank and note metadata, with keys in the Android Keystore.
- AES-256-GCM export protected by a backup password (PBKDF2-HMAC-SHA256, 600k iterations). Backup format 2 contains all cards (including archive state and reason and the needs-check flags), all managed lists and, optionally (on by default), all settings including the Card list choices. Import runs the legacy normaliser, shows a review ("Formats fixed: N · Need a check: M", new/changed/same cards, new list items, settings), then updates saved cards with the backup's details (default) or keeps them, and restores settings when ticked. Removed settings keys in older files are ignored silently. Format 1 backups from earlier versions stay importable.
- Pitch-black dark theme and light theme; ISO ID-1 card proportions (85.60 × 53.98 mm); neutral issuer marks.
- Adaptive navigation rail and multi-column card grid; book-fold layouts split the grid around a separating hinge.
- Optional Material You dynamic colors on Android 12+; brand red remains the default.
- Three-tab navigation (Cards, Manage, Settings); bottom sheets for filters, card actions, bank picker and tags; exposed dropdown menus; one app-level snackbar host.
- `FLAG_SECURE` on every activity; screenshots and screen recording are blocked.
- R8-minified signed release APK.
- Jetpack Compose with Material Design 3; package `com.quickcards.app`.

Durable constraints:

- Offline-only and camera-free: no network permission, no camera permission, no sync, no telemetry. The merged debug and release manifests are verified to contain no INTERNET, ACCESS_NETWORK_STATE or CAMERA permission.
- Encryption is mandatory. Biometrics are mandatory for sensitive actions; opening the app is gated by the app lock when it is on (default).
- Existing data must survive changes: Room migrations must be non-destructive, and older card exports must remain importable.
- `minSdk 26` must not be raised.
- Screenshot/screen-recording prevention is mandatory and implemented with `FLAG_SECURE` on every activity.

Open decisions:

- Distribution artifact: a signed APK on GitHub Releases is confirmed; store distribution is not. Signing secrets live only in CI repository secrets and a git-ignored local file (see `keystore/README.md`).
- Official issuer brand logos are not licensed or bundled. Neutral network labels are used until approved assets are supplied.

## Brand Commitments

Product name: QuickCards. Brand red (#C8202B) is the action colour across light and dark themes, used at most once per screen. The logo is the Stub Lock mark (a white card split into a body and a torn-off stub, with number bars and a padlock, on brand red); source files are in `docs/brand/stub-lock/`, with adaptive and monochrome layers. Icons are Material, one family throughout. Visual source of truth: DESIGN.md and prototype/timeline.html (v2 prototype in prototype/v2.html).

## Evidence on Hand

- Working Android codebase, including the security layer, migrations, and instrumented tests.
- Clickable v2 prototype, design system and decision timeline in `prototype/` (open `prototype/timeline.html`).
- `README.md` documents the current build, security model, and test commands; `docs/MAINTAINING.md` holds the architecture and change recipes.
- No customer testimonials, benchmarks, press, pricing, or usage data. Future work must not fabricate any.

## Product Principles

1. Speed to the needed value wins: minimize open → find card → copy.
2. Nothing leaves the device; offline is a guarantee, not a degraded mode.
3. Sensitive values are masked by default; reveals are deliberate, authenticated, and time-boxed.
4. Existing card data and export compatibility are inviolable across changes.
5. Follow native Android and Material 3 expectations; express brand through theme and detail, not custom controls.
