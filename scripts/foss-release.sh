#!/usr/bin/env bash
# Build, verify, and publish a FOSS GitHub Release for com.sayemshafayet.orglfoss.
# Requires .local/changelog.txt (release notes under ### Notes).
# Uses set -e so gh release create never runs if an earlier step fails.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

CHANGELOG=".local/changelog.txt"
PACKAGE_ID="com.sayemshafayet.orglfoss"
ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"

die() {
  echo "error: $*" >&2
  exit 1
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "missing command: $1"
}

version_field() {
  local key="$1"
  local allow_empty="${2:-}"
  local value
  value="$(grep -E "^${key}=" version.properties | cut -d= -f2- | tr -d '\r')"
  if [[ -z "$value" && "$allow_empty" != "1" ]]; then
    die "missing $key in version.properties"
  fi
  printf '%s' "$value"
}

[[ -f "$CHANGELOG" ]] || die "missing $CHANGELOG — write release notes there before publishing"
[[ -s "$CHANGELOG" ]] || die "$CHANGELOG is empty"

require_cmd make
require_cmd git
require_cmd gh
require_cmd sha256sum

[[ -f keystore-foss.properties ]] || die "missing keystore-foss.properties"

MAJOR="$(version_field VERSION_MAJOR)"
MINOR="$(version_field VERSION_MINOR)"
PATCH="$(version_field VERSION_PATCH)"
PRERELEASE="$(version_field VERSION_PRERELEASE 1)"
BUILD="$(version_field VERSION_BUILD)"

if [[ -n "$PRERELEASE" ]]; then
  VERSION_NAME="${MAJOR}.${MINOR}.${PATCH}-${PRERELEASE}+${BUILD}"
else
  VERSION_NAME="${MAJOR}.${MINOR}.${PATCH}+${BUILD}"
fi

TAG="v${VERSION_NAME}"
TITLE="${VERSION_NAME} (FOSS)"
APK=".local/apk/orgl-foss-release-${VERSION_NAME}.apk"
SHA_FILE=".local/apk/orgl-foss-release-${VERSION_NAME}.sha256"

if [[ -n "$(git status --porcelain)" ]]; then
  die "working tree is dirty — commit or stash before releasing"
fi

if git rev-parse "$TAG" >/dev/null 2>&1; then
  [[ "$(git rev-parse "${TAG}^{}")" == "$(git rev-parse HEAD)" ]] \
    || die "tag ${TAG} exists but does not point at HEAD"
fi

echo "==> FOSS release ${VERSION_NAME}"
echo "    tag:  ${TAG}"
echo "    apk:  ${APK}"

echo "==> Building signed FOSS APK"
make release-foss

echo "==> Verifying APK"
make verify-foss

echo "==> Writing checksum"
make checksum-foss

[[ -f "$APK" ]] || die "expected APK missing: $APK"
[[ -f "$SHA_FILE" ]] || die "expected checksum missing: $SHA_FILE"

APK_SHA256="$(awk '{print $1; exit}' "$SHA_FILE")"
[[ "$APK_SHA256" =~ ^[0-9a-fA-F]{64}$ ]] || die "could not parse APK SHA-256 from $SHA_FILE"

BUILD_TOOLS_DIR="$(printf '%s\n' "${ANDROID_HOME}"/build-tools/* | sort | tail -n1)"
APKSIGNER="${BUILD_TOOLS_DIR}/apksigner"
[[ -x "$APKSIGNER" ]] || die "apksigner not found under ${ANDROID_HOME}/build-tools"

# --verbose includes key algorithm/size; plain --print-certs does not.
CERT_OUT="$("$APKSIGNER" verify --verbose --print-certs "$APK" 2>&1)"
CERT_SHA256="$(printf '%s\n' "$CERT_OUT" | awk -F': ' '/certificate SHA-256 digest:/{print $2; exit}' | tr -d '[:space:]')"
KEY_ALG="$(printf '%s\n' "$CERT_OUT" | awk -F': ' '/key algorithm:/{print $2; exit}' | tr -d '[:space:]')"
KEY_BITS="$(printf '%s\n' "$CERT_OUT" | awk -F': ' '/key size \(bits\):/{print $2; exit}' | tr -d '[:space:]')"

[[ "$CERT_SHA256" =~ ^[0-9a-fA-F]{64}$ ]] || die "could not parse signing cert SHA-256"
[[ -n "$KEY_ALG" && -n "$KEY_BITS" ]] || die "could not parse signing key algorithm/size"

NOTES_FILE="$(mktemp)"
trap 'rm -f "$NOTES_FILE"' EXIT

{
  cat <<EOF
## FOSS

Package: \`${PACKAGE_ID}\`

### Verify

* APK SHA-256: \`${APK_SHA256}\`
* Signing cert SHA-256: \`${CERT_SHA256}\`
* Key: ${KEY_ALG} ${KEY_BITS}

### Notes

EOF
  cat "$CHANGELOG"
  echo
} >"$NOTES_FILE"

if git rev-parse "$TAG" >/dev/null 2>&1; then
  echo "==> Tag ${TAG} already on HEAD"
else
  echo "==> Creating annotated tag ${TAG}"
  git tag -a "$TAG" -m "$TITLE"
fi

echo "==> Pushing HEAD and tag"
git push origin HEAD
git push origin "refs/tags/${TAG}"

GH_ARGS=(release create "$TAG" --title "$TITLE" --notes-file "$NOTES_FILE" "$APK" "$SHA_FILE")
if [[ -n "$PRERELEASE" ]]; then
  GH_ARGS+=(--prerelease)
fi

echo "==> Creating GitHub Release ${TAG}"
gh "${GH_ARGS[@]}"

echo "==> Done: ${TAG}"
echo "    https://github.com/iShafayet/OneRetroGameLauncher/releases/tag/${TAG//+/%2B}"
