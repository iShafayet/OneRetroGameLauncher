# Assumptions and Limitations

This document records deliberate constraints, historical baggage, and channel-level
assumptions that should not be "fixed" accidentally during routine refactors.

## Package identities

- **Google Play package:** `com.sayemshafayet.onereogamelauncher`
- **FOSS package:** `com.sayemshafayet.orglfoss`

### Permanent Play package typo

The Google Play package name contains a permanent typo: `onereo` instead of
`oneretro`.

This typo is **intentional to preserve continuity** with the already-approved Play
listing, closed beta track, and future production updates. Changing the Play
`applicationId` would create a brand-new Play app and strand the existing listing.

Implications:

- Do **not** rename the Play `applicationId` to fix spelling.
- Kotlin package names may remain as-is; the install/update identity is the
  `applicationId`.
- When documenting channel/package differences, call out the typo explicitly so
  future maintainers do not "correct" it by mistake.

## Multi-channel distribution assumption

ORGL is distributed as two separate install identities:

- **Play** uses Google Play App Signing and the Play package above.
- **FOSS** is intended for GitHub Releases, Obtainium, and F-Droid under the
  `com.sayemshafayet.orglfoss` package.

Because these are different Android packages:

- users cannot in-place update from Play to FOSS or FOSS to Play;
- app-private storage does not migrate automatically between them;
- SAF folder permissions must be granted again after switching packages.

Portable ORGL data in the user-selected ORGL directory remains the preferred path
for moving artwork and other ORGL-owned files across channels.

## Release-safety default

Local developer commands should default to the **FOSS** flavor/package. Play
builds should require explicit intent (for example `make bundle-play`) to reduce
human error when cutting releases.
