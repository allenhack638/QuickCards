# Security policy

QuickCards stores payment card details, so security reports are welcome and taken
seriously.

## Supported versions

Only the **latest release** on
[GitHub Releases](https://github.com/allenhack638/QuickCards/releases) receives security
fixes. Please reproduce on the latest release before reporting.

## Reporting a vulnerability

Report privately through GitHub's private vulnerability reporting: open the repository's
**Security** tab and choose **Report a vulnerability**. Do not open a public issue,
discussion or pull request for a suspected vulnerability.

Please include:

- the app version (Settings › About) and Android version / device;
- what an attacker can do and what access they need (physical access, another app, a
  backup file, etc.);
- steps to reproduce, using **made-up test cards only**.

**Never include real card data** — no real card numbers, expiry dates, CVVs, backup files
made from a real vault, or screenshots of real cards. Use test numbers such as
`4111 1111 1111 1111`.

You can expect an acknowledgement within a few days. Once a fix is released, the advisory
will be published and you will be credited unless you prefer otherwise.

## Scope

In scope: the Android app in this repository, its at-rest encryption, app lock and
authentication gates, clipboard and notification handling, and the encrypted backup
format.

Out of scope: a fully compromised or rooted device, a device without a screen lock,
debug builds (they deliberately allow screenshots), and the HTML prototype under
`prototype/`.
