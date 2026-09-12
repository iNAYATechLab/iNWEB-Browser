#!/usr/bin/env bash
# =============================================================================
# iNWEB Browser — authoring-sandbox session bootstrap
#
# The authoring sandbox is ephemeral: only /home/user persists between
# sessions; installed system packages do not. This script reinstalls the
# per-session toolchain so a fresh session can continue work immediately.
# (Credentials such as the GitHub CLI auth config DO persist in ~/.config.)
# =============================================================================
set -euo pipefail

GH_VERSION="2.100.0"

echo "[bootstrap] git / python3 ..."
command -v git >/dev/null || { echo "ERROR: git missing" >&2; exit 2; }
command -v python3 >/dev/null || { echo "ERROR: python3 missing" >&2; exit 2; }

echo "[bootstrap] GitHub CLI ..."
if ! command -v gh >/dev/null 2>&1; then
  cd /tmp
  curl -sL --max-time 120 -o "gh.tar.gz" \
    "https://github.com/cli/cli/releases/download/v${GH_VERSION}/gh_${GH_VERSION}_linux_amd64.tar.gz"
  tar xzf gh.tar.gz
  mv "gh_${GH_VERSION}_linux_amd64/bin/gh" /usr/local/bin/gh
  cd -
fi
gh --version | head -1
gh auth setup-git 2>/dev/null || true

echo "[bootstrap] Python tooling (PyYAML) ..."
python3 -c 'import yaml' 2>/dev/null || {
  apt-get update -qq >/dev/null 2>&1 || true
  apt-get install -y -qq python3-yaml >/dev/null 2>&1
}
python3 -c 'import yaml; print("pyyaml ready")'

echo "[bootstrap] OK"
