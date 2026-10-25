# Changelog

All notable changes to this project are documented in this file.

## [Unreleased]

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
