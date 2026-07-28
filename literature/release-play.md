# Play Release

## Package and signing

- Package name stays `com.sayemshafayet.onereogamelauncher`.
- Do not change the Play `applicationId`; it would create a new Play app.
- Play uses Google Play App Signing. Local builds use the upload key from `keystore-play.properties`.

## Build

1. Update `version.properties` if needed.
2. Build the Play App Bundle:
   - `make bundle-play`
3. Optional local verification:
   - `make verify-play`

Artifacts:

- `.local/bundle/orgl-play-release-<version>.aab`
- `.local/apk/orgl-play-release-<version>.apk` if `make release-play` is also run

## Console flow

1. Open the existing Play app listing.
2. Create a new release in the intended track.
3. Upload the `.aab`.
4. Add release notes.
5. Complete any required Play declarations.
6. Roll out to the chosen track.

## Guardrails

- Release commands require an explicit flavor suffix (`release-play`, `bundle-play`, etc.).
- Debug commands default to FOSS (`build`, `run`, `compile`).
- Keep Play release notes and policy declarations aligned with the live app behavior.
