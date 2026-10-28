#!/bin/bash
# SecurePlus Frontend - GitHub Push Automation Script

set -e

if [ -z "$1" ]; then
    echo "=========================================================="
    echo " Error: GitHub Repository URL is required!"
    echo " Usage:"
    echo "   ./push_to_github.sh https://github.com/<USERNAME>/<REPO>.git"
    echo "=========================================================="
    exit 1
fi

REPO_URL="$1"

echo "=== Initializing and Preparing Git Repository ==="
[ -d .git ] || git init
git branch -M main

echo "=== Staging Frontend Files ==="
git add .

echo "=== Creating Initial Commit ==="
git commit -m "feat: Decoupled SecurePlus Frontend with Jetpack Compose, AES-256 and Zero-Trust Cloud architecture" || echo "Working tree clean, no new changes to commit."

echo "=== Configuring Remote URL ==="
if git remote | grep -q "^origin$"; then
    git remote set-url origin "$REPO_URL"
else
    git remote add origin "$REPO_URL"
fi

echo "=== Pushing to GitHub (main branch) ==="
git push -u origin main

echo ""
echo "=========================================================="
echo " ✅ SecurePlus Frontend successfully pushed to GitHub!"
echo " Repository URL: $REPO_URL"
echo "=========================================================="
