#!/usr/bin/env bash
set -euo pipefail

git config core.hooksPath scripts/git-hooks

echo "Git hooks installed."
echo "Current hooks path: $(git config core.hooksPath)"