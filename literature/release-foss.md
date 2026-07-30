# FOSS Release

FOSS channel ships **beta and stable** builds via GitHub Releases for package
`com.sayemshafayet.orglfoss`. Obtainium can track those releases.

## Release steps

1. Update `version.properties` if needed.
2. Commit and tag the exact release commit (`vX.Y.Z...`).
3. Build the signed APK:
   - `make release-foss`
4. Verify the signed APK:
   - `make verify-foss`
5. Generate the checksum:
   - `make checksum-foss`
6. Print the signing certificate details if needed:
   - `make cert-foss`
7. Collect the release artifacts:
   - `.local/apk/orgl-foss-release-<version>.apk`
   - `.local/apk/orgl-foss-release-<version>.sha256`
8. Create the GitHub Release from the matching tag.
9. Upload the APK and checksum file to the GitHub Release.
10. Publish release notes / changelog.
11. Keep the GitHub Release APK filename as `orgl-foss-release-<version>.apk`
    so Obtainium’s filter in [`obtainium.json`](../obtainium.json) keeps matching.
    User-facing Obtainium setup lives in the root README (import that JSON).
