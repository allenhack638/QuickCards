# Changelog

All notable changes to this project are documented in this file.

## [2.4.0] - 2026-10-27

- Security: copied values are marked sensitive on Android 13+ so the system clipboard preview
  no longer renders the card, and a copy whose timer was lost to process death is cleared the
  first focused read after process death. The clipboard stays usable while switching apps to paste.
- Security: the expiry/CVV notification is off by default (enabling it still asks to unlock)
  and posts on a new silent, low-importance channel; upgrading installs get the old channel
  removed. It no longer appears as a heads-up over other apps.
- Security: dialogs, sheets and popups now apply `filterTouchesWhenObscured` and, on Android
  12+, hide overlay windows, so confirmation dialogs cannot be tapjacked.
- Security: a backup's network rules are bounded on import (`lengths` can no longer expand a
  huge range), names are capped, and `parseLengths` itself only ever allocates card lengths.
- Security: the autofill card list cancels when its activity stops, so it no longer survives
  screen-off with decrypted cards and fills on one tap after unlock.
- Security: the instant-copy window uses the monotonic clock, so changing the device time
  cannot extend it.
- Changed: editing a card keeps its imported network override, variant and name-on-card
  instead of silently rewriting them.
- Changed: the backup password field uses a password keyboard with autocorrect off.
- Changed: the daily reminder alarm is re-armed after an app update and after a clock or time
  zone change, not only after a reboot.
- Changed: `targetSdk`/`compileSdk` 36 (Android 16) and AGP 8.9.2 for Google Play.
- Added: a bundled Privacy policy screen in Settings > About; the public copy lives at
  `docs/privacy.html`. The app still has no network permission.
- Removed: the EmojiCompat startup initializer, whose default configuration can fetch a font
  through Google Play services, to keep every asset bundled and the app offline.
- Docs: corrected the clipboard and database-version claims in `docs/MAINTAINING.md`, updated
  the settings defaults, and added `docs/PLAY_RELEASE.md` for the Play upload.

- Removed: the Use system colours setting; QuickCards always uses its own palette.
- Changed: network logos sit level with the bank name on cards, and the Card networks list is tighter with no tile in the light theme.
- Changed: fewer Settings (pin limit 10, tags per card 10, one notification switch, tag and archived search always on); "backup password" replaces "passphrase".
- Fixed: picking a backup file no longer locks the app; the empty Cards screen no longer jumps when adding a card; bottom sheets clear the navigation bar.
- Changed: Card number and Expiry use the text keyboard (suggestions off) so the keyboard's scan-text tool is available; CVV keeps the masked number pad.
- Changed: consistent dialog and sheet spacing, smoother bottom-bar transitions.
- Changed: the app asks to unlock as soon as it opens; redesigned lock screen with a full-width Unlock button and the app version.
- Changed: the expiry and CVV notification only shows the values (no Copy buttons); sheets close when you tap outside them; tighter Settings and Manage rows; the splash shows the rounded logo tile.
- Changed: smoother screen transitions with no overlapping text; a card morphs into Edit; pinned and archived cards slide into place; copying gives press feedback and a haptic.
- Changed: envelope encryption (a Keystore key wraps an in-memory data key) and off-main-thread decryption; opening screens and Add card no longer stall. Existing cards are re-wrapped automatically on first launch.
- Added: Share a card from its menu, with a choice to include the CVV. The clipboard and the expiry/CVV notification now keep their timers when you switch apps, and the keyboard shows the copied value.
- Changed: the app appears in Recents; Settings headings and dialog rows are clearer; holder chips press cleanly; snappier screen transitions.
- Changed: card network labels can be up to 15 characters; the badge grows to fit.
- Added: built-in card networks show their official mark on a standard plate on the card, in Manage and in the card form.
- Added: autofill in other apps' checkout forms, after the fingerprint prompt, with a Settings switch.
- Added: add a card by holding it to the phone (NFC reads the number and expiry; the CVV is still typed), with a Settings switch on phones with NFC.
- Added: expiry reminders 30 days ahead and payment reminders 3 days before a card's optional due day, each with a Settings switch; payment reminders repeat daily until you tap Paid.
- Added: a "Bill due" strip on the card during the payment reminder window, with Mark paid and Undo.
- Fixed: at large font sizes on narrow phones the card's expiry and CVV no longer get cut off.
- Fixed: backups now keep each card's payment due day and long network labels; restoring no longer clears or shortens them.
- Fixed: turning on "Show expiry and CVV after copying" asks to unlock again, and a hidden CVV setting from older versions or backups keeps the notification off.
- Fixed: autofill only fills visible fields on the same site, names the app or site in the prompt and no longer mistakes bank-account fields for card fields; a misbehaving NFC card can no longer crash the app.
- Changed: a card number can exist only once. A backup with the same card twice imports only the first copy, and duplicates left by older versions are removed on upgrade (tags and notes are kept).
- Changed: Settings descriptions are shorter, one line each, in plain language.
- Added: autofill lists "Search all N cards…" first, then your pinned and most recently used cards with their network logo; after unlocking, search by number, bank, holder, tag or note. On Android 9+, typing the first or last digits narrows the suggestions.
