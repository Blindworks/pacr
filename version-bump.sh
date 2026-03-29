#!/usr/bin/env bash
#
# Bumps the version in both pom.xml and package.json.
# Usage: ./version-bump.sh <major|minor|patch>
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
POM="$SCRIPT_DIR/backend/pom.xml"
PKG="$SCRIPT_DIR/frontend/package.json"

if [[ $# -ne 1 ]] || [[ ! "$1" =~ ^(major|minor|patch)$ ]]; then
  echo "Usage: $0 <major|minor|patch>"
  exit 1
fi

PART="$1"

# Extract current version from pom.xml (first <version> after <artifactId>smart-trainingsplan-backend)
CURRENT=$(grep -A1 '<artifactId>smart-trainingsplan-backend</artifactId>' "$POM" \
  | grep '<version>' | sed 's/.*<version>\(.*\)<\/version>.*/\1/' | tr -d ' ')

if [[ -z "$CURRENT" ]]; then
  echo "Error: Could not read version from pom.xml"
  exit 1
fi

IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT"

case "$PART" in
  major) MAJOR=$((MAJOR + 1)); MINOR=0; PATCH=0 ;;
  minor) MINOR=$((MINOR + 1)); PATCH=0 ;;
  patch) PATCH=$((PATCH + 1)) ;;
esac

NEW="$MAJOR.$MINOR.$PATCH"

echo "Bumping version: $CURRENT -> $NEW"

# Update pom.xml
sed -i "s|<version>$CURRENT</version>|<version>$NEW</version>|" "$POM"

# Update package.json
sed -i "s|\"version\": \"$CURRENT\"|\"version\": \"$NEW\"|" "$PKG"

echo "Updated:"
echo "  $POM -> $NEW"
echo "  $PKG -> $NEW"
