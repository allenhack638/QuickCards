# Changelog

All notable changes to this project are documented in this file.

## [2.8.0] - 2026-11-25

- Added: Best today. Give a credit card its statement day (on your bill as 'statement date') and due day, and the card that gives you the most days to pay for a purchase made today shows a "Best today" chip. Tap it to see the bill dates and how many days you get.
- Added: tap any status chip (Best today, Expires soon, Expired, Check, Last used, Archived) to see why it is there and what to do next.
- Changed: status chips are slimmer and match the Copy button on the card.
- Added: backups and the import spreadsheet carry the statement day; older backups and sheets still import and keep the statement days already on your phone.
- Fixed: the "Due in N days" strip now sits evenly inside the card's rounded corners.

## [2.7.1] - 2026-11-22

- Changed: after you copy a card number, the notification shows that card's expiry and CVV directly (no copy buttons). It still disappears after the notification time, never appears on the lock screen, and can be turned off in Settings › Copying.
- Changed: the Filter sheet uses chips, the bottom bar matches the new design, and App lock rows dim with a short note when App lock is off.
- Fixed: autofill network logos stay readable when the app and system themes differ.

## [2.7.0] - 2026-11-22

- Changed: a full redesign, Daylight. Cards get jewel colours with a pattern per colour, banks have a default colour, and every screen is cleaner with less text, in light and dark.
- Changed: tap a card to open its details; hold it for actions (share, archive, delete, pin). Copy on a card copies the number; expiry and CVV are one tap away in the copy notification, which is now on by default for new installs, shows masked values and never appears on the lock screen.
- Added: tablet, foldable and landscape layouts; tapping a reminder opens that card.
- Fixed: the app locks again when you come back from sharing after the grace time, and revealed values clear when you switch cards or the app locks.

## [2.6.1] - 2026-11-15

- Changed: the Import cards dialogs are easier to read. The two import choices and the three ways to get the sheet are proper list rows with icons; the check screen shows the file, a count list (ready, need fixing, already in QuickCards, new holders) and one line per problem beside each row number; Proceed and Import stay visible on large font sizes; a sheet whose cards are all already saved says so.

## [2.6.0] - 2026-11-15

- Added: Settings › Import cards can add many cards at once from a spreadsheet. Download the Excel sheet or simple CSV from the app, or use the public Google Sheet at surety.money/quickcards/sheet; every row is checked (number, expiry, CVV, network match, bank, card type) and the app lists what is ready, what needs fixing and what is already in QuickCards before anything is saved. Duplicates are skipped and you are reminded to delete the file afterwards.
- Changed: the card form requires a card type (Credit, Debit or Prepaid); the None option is gone and existing cards without a type are asked for one on their next edit.
- Added: a built-in list of 26 Indian and international credit-card banks is available in the Bank picker and the spreadsheet; missing banks are added on upgrade without touching your own banks.

## [2.5.1] - 2026-11-15

- Fixed: cards on a custom network are checked against that network (network edits, imports, and the CVV field).

## [2.5.0] - 2026-10-31

- Fixed: card number and expiry open the number pad and stay on it; the keyboard no longer switches to letters after four digits.
- Fixed: when a built-in network's rules were changed (for example Visa set to a 4-digit CVV), the card form names it and offers Reset to standard; saved card values are never rewritten.
- Fixed: restoring a backup keeps the slate card colour and checks restored cards against the backup's own networks.
- Added: Google Play update prompt; a new version downloads in the background and a Restart button installs it. It asks the Play Store app on the phone and adds no permission.
- Added: a network can allow a blank CVV, and Maestro does by default; sharing a card without a CVV hides Include CVV.
- Changed: editing a built-in network warns when it differs from the standard rules and offers Reset to standard.
- Tests: property-based tests generate thousands of inputs per run for card numbers, expiry, CVV, smart paste and backup round trips; Espresso 3.7.0 so UI tests run on Android 15+.

## [2.4.0] - 2026-10-27

- Security: copied values are marked sensitive on Android 13+ so the system clipboard preview
  no longer renders the card, and a copy whose timer was lost to process death is cleared on the
  first focused read after process death. The clipboard stays usable while switching apps to paste.
- Security: the expiry/CVV notification is off by default on a fresh install (a 2.3 upgrade
  with no stored switch is migrated to on once; enabling it still asks to unlock) and posts on
  a new silent, low-importance channel; upgrading installs get the old channel removed. It no longer appears as a heads-up over other apps.
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
