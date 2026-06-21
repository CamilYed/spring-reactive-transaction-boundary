$ErrorActionPreference = "Stop"

git config core.hooksPath scripts/git-hooks

Write-Host "Git hooks installed."
Write-Host "Current hooks path: $(git config core.hooksPath)"