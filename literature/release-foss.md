# FOSS Release

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
11. Update the website with:
    - release link
    - SHA-256 checksum
    - certificate fingerprint (when needed)
12. Use that exact GitHub Release APK for Obtainium / IzzyOnDroid / reproducible F-Droid workflows.
