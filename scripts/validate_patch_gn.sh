#!/usr/bin/env bash
# Validate the GN files the iNWEB patches add to the Chromium tree.
#
# Why this exists: hop 31 spent its whole window on a gn gen error —
# `sources = glob([...])` written inside a template invocation block,
# which GN rejects with "Unknown function". That error is invisible to
# every local gate we had: it is not a syntax error, so `gn format`
# passes it, and it only appears once the real build runs. A hop costs
# 40-95 minutes, so a rule this cheap to check mechanically must be.
#
# Two checks, both on files whose path contains "inweb" (our namespace
# inside the Chromium tree, so upstream files are never flagged):
#   1. gn format --dry-run  — real syntax/parse check, when a gn binary
#      is available (set INWEB_GN, or put `gn` on PATH). Skipped loudly
#      when absent, since a silent skip is how false greens are born.
#   2. function calls inside a template invocation block — GN forbids
#      them; the fix is to resolve the call at file scope and pass the
#      result in.
#
# Usage: scripts/validate_patch_gn.sh [tree-directory]

set -o pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TREE="${1:-${INWEB_TREE:-$ROOT/chromium/src}}"

if [ ! -d "$TREE" ]; then
  echo "[patch-gn] tree not found: $TREE"
  echo "[patch-gn] pass a tree directory, or set INWEB_TREE"
  exit 1
fi

FILES="$(find "$TREE" -path '*inweb*' \( -name '*.gn' -o -name '*.gni' \) | sort)"
if [ -z "$FILES" ]; then
  echo "[patch-gn] no iNWEB GN files found under $TREE"
  exit 1
fi

STATUS=0
COUNT=0

# ---- 1: gn format (parse + style), when a gn binary is available ----------
GN_BIN="${INWEB_GN:-$(command -v gn || true)}"
if [ -n "$GN_BIN" ] && [ -x "$GN_BIN" ]; then
  while IFS= read -r f; do
    COUNT=$((COUNT + 1))
    OUT="$("$GN_BIN" format --dry-run "$f" 2>&1)"
    RC=$?
    # gn format exit codes: 0 = already canonical, 2 = would be
    # reformatted (a style difference, which our generated files can
    # legitimately have), anything else = it could not parse the file.
    if { [ "$RC" -ne 0 ] && [ "$RC" -ne 2 ]; } || printf '%s' "$OUT" | grep -q '^ERROR'; then
      echo "[patch-gn] GN SYNTAX ERROR in ${f#"$TREE"/}:"
      printf '%s\n' "$OUT"
      STATUS=1
    elif [ "$RC" -eq 2 ]; then
      echo "[patch-gn] note: ${f#"$TREE"/} is not canonically formatted (style only)"
    fi
  done <<< "$FILES"
  echo "[patch-gn] gn format clean on $COUNT iNWEB GN file(s) [$GN_BIN]"
else
  echo "[patch-gn] SKIPPED gn format: no gn binary (set INWEB_GN)"
fi

# ---- 2: no function calls inside a template invocation block --------------
# GN allows assignments, conditionals and a small set of builtins there.
# Anything else that looks like a call (`name(`) is rejected by gn gen.
while IFS= read -r f; do
  python3 - "$f" <<'PY' || STATUS=1
import re, sys

path = sys.argv[1]
# Statements GN permits inside an invocation block.
OK_STMT = ("if", "foreach", "else", "assert", "not_needed", "#", "}")
# Builtins that legitimately appear as calls inside conditions.
OK_CALL = ("defined", "getenv", "get_path_info", "rebase_path", "string_join")

depth = 0
invocation_depth = None
bad = []
with open(path, encoding="utf-8") as fh:
    for n, raw in enumerate(fh, 1):
        line = raw.split("#")[0].rstrip()
        stripped = line.strip()
        if invocation_depth is not None and depth > invocation_depth:
            if stripped and not stripped.startswith(OK_STMT):
                for name in re.findall(r"\b([a-z_][a-z0-9_]*)\s*\(", line):
                    if name not in OK_CALL:
                        bad.append((n, stripped))
                        break
        opens = line.count("{") - line.count("}")
        # A template invocation: name("target") {  — track the body depth.
        if re.match(r'^[a-z_][a-z0-9_]*\(".*"\)\s*\{\s*$', stripped):
            depth += opens
            invocation_depth = depth - 1
            continue
        if invocation_depth is not None and depth == invocation_depth and stripped == "}":
            invocation_depth = None
        depth += opens

if bad:
    print(f"[patch-gn] FUNCTION CALL INSIDE TEMPLATE BLOCK in {path}:")
    for n, text in bad:
        print(f"  {n}: {text}")
    print("[patch-gn]   gn gen rejects this ('Unknown function'); resolve the call at")
    print("[patch-gn]   file scope into a variable and pass the variable in.")
    sys.exit(1)
PY
done <<< "$FILES"

if [ "$STATUS" -eq 0 ]; then
  echo "[patch-gn] template-block rule clean on iNWEB GN file(s)"
fi
exit "$STATUS"
