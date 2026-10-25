# Changelog

All notable changes to this project are documented in this file.

## [Unreleased]

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
