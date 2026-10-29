#!/bin/bash
# SecurePlus - GitHub Push Automation Script (Pure Android App / APK Code Only)

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

echo "=== Initializing Git Repository ==="
[ -d .git ] || git init
git config user.name "AI Studio Developer" 2>/dev/null || true
git config user.email "developer@aistudio.google.com" 2>/dev/null || true
git branch -M main

echo "=== Excluding Backend Cloud Rules from Git ==="
git rm --cached firebase-blueprint.json firebase.json firestore.rules firestore.test.js 2>/dev/null || true

echo "=== Staging Pure Android Studio Project Files ==="
git add .

echo "=== Creating Commit (Pure Client APK Code) ==="
git commit -m "feat: Pure Android Studio APK client project (backend rules excluded)" || echo "Working tree clean, no new changes to commit."

echo "=== Configuring Remote URL ==="
if git remote | grep -q "^origin$"; then
    git remote set-url origin "$REPO_URL"
else
    git remote add origin "$REPO_URL"
fi

echo "=== Pushing Android Studio App Code to GitHub (main) ==="
git push -u origin main

echo ""
echo "=========================================================="
echo " ✅ Android Studio App code successfully pushed to GitHub!"
echo " (Backend cloud files were completely excluded)"
echo " Repository URL: $REPO_URL"
echo "=========================================================="
