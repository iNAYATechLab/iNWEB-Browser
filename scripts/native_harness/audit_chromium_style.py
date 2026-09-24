#!/usr/bin/env python3
"""iNWEB native code — Chromium style-plugin + include-path audit.

Reproduces locally (heuristic, tuned to our code style) the checks that
burned real 30-minute build hops (hop 15: NoDestructor static_assert;
hop 16: chromium-style out-of-line dtor + shim-invented include path):

  1. find-bad-constructs ctor/dtor rules (chromium.org style-checker
     errors page): complexity points per class member — templated
     member = 10, non-POD member = 3, integral member = 1 (constructor
     score only), templated base = 9; score >= 10 => complex.
       complex + no declared ctor          -> out-of-line ctor needed
       complex + no declared dtor          -> out-of-line dtor needed
       complex + ctor/dtor `= default` or body IN HEADER -> inline-body
       error; the definition must live in the .cc (may be `= default`
       there, optionally `inline`).
     Evidence-calibrated: std::atomic<T> members do NOT trip the real
     plugin (InwebNotificationPolicy compiled clean in hop 16 with an
     std::atomic member and no declared ctor), so they score 0.
     NOTE the real plugin fires per-TU on ODR-used ctors/dtors, so a
     complex class whose using TU has not compiled yet stays silent —
     this audit is deliberately stricter: it flags the class itself.
  2. "virtual methods with non-empty bodies shouldn't be declared
     inline" (empty `{}` and `= default` / `= delete` are fine).
  3. Include-path honesty: every quoted #include in production sources
     must be an iNWEB-internal header or on the verified upstream
     allowlist (scripts/native_harness/upstream_includes.txt). Hop 16
     caught exactly this bug: net/base/registry_controlled_domains.h
     was a shim-invented path (real: …/registry_controlled_domain.h).

Exit 0 = clean; 1 = violations printed. Unittest sources are skipped
for the style rules (testonly targets are not built by
chrome_public_apk) but their includes are still audited.
"""
from __future__ import annotations

import os
import re
import sys

HARNESS_ROOT = os.path.dirname(os.path.abspath(__file__))
SRC_ROOT = os.path.normpath(os.path.join(HARNESS_ROOT, "..", "..", "src", "native"))
ALLOWLIST_PATH = os.path.join(HARNESS_ROOT, "upstream_includes.txt")

TEMPLATE_TYPES_10 = (
    "std::vector", "std::map", "std::unordered_map", "std::multimap",
    "std::set", "std::unordered_set", "std::deque", "std::list",
    "std::pair", "std::tuple", "std::optional", "absl::optional",
    "std::function", "std::unique_ptr", "std::shared_ptr", "std::weak_ptr",
    "base::RepeatingCallback", "base::OnceCallback", "base::WeakPtrFactory",
    "base::NoDestructor", "raw_ptr", "raw_ref", "base::flat_map",
    "scoped_refptr", "std::array",
)
NONPOD_TYPES_3 = (
    "std::string", "std::wstring", "base::string16", "GURL", "base::Lock",
    "base::Time", "base::TimeTicks", "base::Value", "base::Version",
    "url::Origin", "net::HttpRequestHeaders", "FilterOptions",
)
ZERO_TYPES = ("std::atomic", "std::string_view", "base::StringPiece")

NON_MEMBER_STARTS = (
    "using", "typedef", "friend", "template", "enum", "class", "struct",
    "operator", "return", "static_assert", "namespace", "public:",
    "private:", "protected:", "explicit", "virtual", "static", "const",
    "constexpr", "override", "final", "}", "{", "//", "/*", "*",
    "DISALLOW", "WEB_CONTENTS", "MOJO",
)


def strip_line_comment(line: str) -> str:
    idx = line.find("//")
    return line if idx < 0 else line[:idx]


def strip_templates(text: str) -> str:
    out = []
    depth = 0
    for ch in text:
        if ch == "<":
            depth += 1
        elif ch == ">":
            depth = max(0, depth - 1)
        elif depth == 0:
            out.append(ch)
    return "".join(out)


def member_points(type_text: str, for_ctor: bool) -> int:
    t = type_text.strip()
    for zt in ZERO_TYPES:
        if t.startswith(zt):
            return 0
    for tt in TEMPLATE_TYPES_10:
        if t.startswith(tt):
            return 10
    for np in NONPOD_TYPES_3:
        if t.startswith(np):
            return 3
    if for_ctor:
        if t.endswith("*") or t.endswith("&"):
            return 1
        if re.fullmatch(r"(const\s+)?(int|uint32_t|uint64_t|int64_t|size_t|"
                        r"bool|float|double|char|unsigned|long|short|"
                        r"int8_t|uint8_t|int16_t|uint16_t)(\s+\w+)?", t):
            return 1
    return 0


CLASS_RE = re.compile(r"^\s*(class|struct)\s+(\w+)\s*(?::[^{]*)?\{\s*$")


def find_classes(lines):
    """Yield (name, header_lineno, body_lines) with body EXCLUDING nested
    classes (naive: nested class bodies are kept out by depth re-entry
    scanning — our headers never nest classes)."""
    i = 0
    n = len(lines)
    while i < n:
        m = CLASS_RE.match(strip_line_comment(lines[i]))
        if not m:
            i += 1
            continue
        depth = 1
        body = []
        j = i + 1
        while j < n and depth > 0:
            seg = strip_line_comment(lines[j])
            depth += seg.count("{") - seg.count("}")
            if depth > 0:
                body.append((j + 1, seg))
            j += 1
        yield m.group(2), i + 1, body
        i = j


def audit_class(name: str, hline: int, body, base: str, problems: list) -> None:
    """Walk class body at member-declaration level (skip method bodies)."""
    depth = 0            # inside a method body
    members = []
    ctor_decl = ctor_inline = dtor_decl = dtor_inline = False
    move_inline = move_decl = copy_deleted = False
    virt_bad = []
    for lineno, seg in body:
        if depth == 0:
            opens = seg.count("{")
            if opens:
                depth += opens - seg.count("}")
                continue  # method body start; consumed below via depth walk
        else:
            depth += seg.count("{") - seg.count("}")
            continue
        text = seg.strip()
        if not text:
            continue
        plain = strip_templates(text)
        # move ctor / move assign FIRST (they also match the ctor shape)
        if re.match(r"^%s\s*\(\s*%s\s*&&" % (re.escape(name), re.escape(name)), plain):
            move_decl = True
            if "= default" in text:
                move_inline = True
            continue
        if re.match(r"^%s\s*&\s*operator=\s*\(\s*%s\s*&&" % (re.escape(name), re.escape(name)), plain):
            move_decl = True
            if "= default" in text:
                move_inline = True
            continue
        if re.match(r"^%s\s*\(\s*const\s+%s\s*&" % (re.escape(name), re.escape(name)), plain) \
                and "= delete" in text:
            copy_deleted = True
            continue
        # constructor?
        if re.match(r"^(explicit\s+|constexpr\s+)*%s\s*\(" % re.escape(name), plain):
            if "= delete" in text:
                continue
            ctor_decl = True
            if "= default" in text:
                ctor_inline = True
            continue
        if re.match(r"^~%s\s*\(" % re.escape(name), plain):
            if "= delete" in text:
                continue
            dtor_decl = True
            if "= default" in text:
                dtor_inline = True
            continue
        # virtual method with inline body?
        if "{" in seg and "}" in seg and ("virtual" in text or "override" in text):
            m = re.search(r"\{\s*(.*?)\s*\}", text)
            if m and m.group(1).strip() and m.group(1).strip() != "}":
                virt_bad.append((lineno, text[:70]))
            continue
        # member variable?
        if plain.endswith(";") and "(" not in plain:
            m = re.match(r"^(?:mutable\s+|static\s+|volatile\s+)*"
                         r"(.+?)\s+\w+(\s*=[^;]*)?;$", plain.strip())
            if m and not any(m.group(1).startswith(k) for k in NON_MEMBER_STARTS):
                members.append(m.group(1))
    for t in members:
        if t.rstrip().endswith("*") and not t.startswith(("char", "void", "const char", "const void")):
            problems.append(f"{base}:{hline}: class {name}: raw pointer field "
                            f"of type `{t.strip()}` — use raw_ptr<T> "
                            f"(chromium-rawptr, check-raw-ptr-fields)")
    ctor_score = sum(member_points(t, True) for t in members)
    dtor_score = sum(member_points(t, False) for t in members)
    label = f"{base}:{hline}: class {name} (ctor-score {ctor_score}, dtor-score {dtor_score})"
    complex_class = ctor_score >= 10 or dtor_score >= 10
    if ctor_score >= 10:
        if not ctor_decl:
            problems.append(f"{label} — needs out-of-line CONSTRUCTOR")
        elif ctor_inline:
            problems.append(f"{label} — constructor `= default`/body inline in header")
    if dtor_score >= 10:
        if not dtor_decl:
            problems.append(f"{label} — needs out-of-line DESTRUCTOR")
        elif dtor_inline:
            problems.append(f"{label} — destructor `= default`/body inline in header")
    if move_inline and (ctor_score >= 10 or dtor_score >= 10):
        problems.append(f"{label} — move constructor `= default` inline in header")
    if complex_class and dtor_decl and not move_decl and not copy_deleted:
        # Rule-of-five: declaring a dtor suppresses implicit moves; a
        # value-like complex class then silently falls back to COPIES
        # (compile error when a member is move-only, perf loss when not).
        problems.append(f"{label} — dtor declared but no move ctor/assign "
                        f"declared (implicit moves suppressed)")
    for lineno, text in virt_bad:
        problems.append(f"{base}:{lineno}: virtual method with non-empty inline "
                        f"body: {text}…")


def audit_style(path: str, problems: list) -> None:
    with open(path, encoding="utf-8") as fh:
        lines = fh.read().splitlines()
    base = os.path.basename(path)
    for name, hline, body in find_classes(lines):
        audit_class(name, hline, body, base, problems)


def audit_includes(path: str, allow: set, problems: list) -> None:
    base = os.path.basename(path)
    with open(path, encoding="utf-8") as fh:
        for i, line in enumerate(fh, 1):
            m = re.match(r'\s*#include\s+"([^"]+)"', line)
            if not m:
                continue
            inc = m.group(1)
            if inc.startswith("chrome/android/inweb/") or \
               inc.startswith("third_party/zlib/"):
                continue
            if inc not in allow:
                problems.append(f"{base}:{i}: #include \"{inc}\" not on "
                                f"verified upstream allowlist")


def audit_unsafe_buffers(path: str, problems: list) -> None:
    """Heuristics for the unsafe-buffers plugin (hop-17 lesson:
    kHex[...] C-array indexing; archive reader pointer arithmetic):
      - indexing a file-local C-array (constexpr/static char x[] = …)
      - `.data() +` / `.data()+` pointer arithmetic
      - `reinterpret_cast<…>(… + n)` arithmetic on casts (subset)
    Scoped to production sources; shims are exempt (no plugin there).
    """
    if "unittest" in os.path.basename(path):
        return  # testonly targets are not built by chrome_public_apk
    base = os.path.basename(path)
    with open(path, encoding="utf-8") as fh:
        lines = fh.read().splitlines()
    decl_re = re.compile(r"\s*(?:static\s+|constexpr\s+|const\s+)*"
                         r"[A-Za-z_][\w:<>, ]*?\s(\w+)\[\s*\]?\s*(?:=|\{)")
    c_arrays = set()
    decl_lines = set()
    for idx, line in enumerate(lines, 1):
        m = decl_re.match(line)
        if m and not line.strip().startswith("//"):
            c_arrays.add(m.group(1))
            decl_lines.add(idx)
    for i, line in enumerate(lines, 1):
        if i in decl_lines:
            continue
        code = strip_line_comment(line)
        if not code.strip():
            continue
        if re.search(r"\.data\(\)\s*\+", code):
            problems.append(f"{base}:{i}: pointer arithmetic on .data() "
                            f"(unsafe-buffers) — use checked offsets/spans")
        for name in c_arrays:
            if re.search(r"\b%s\s*\[" % re.escape(name), code):
                problems.append(f"{base}:{i}: C-array indexing on `{name}` "
                                f"(unsafe-buffers) — use a Chromium API or "
                                f"class operator[]")


def main() -> int:
    allow = set()
    with open(ALLOWLIST_PATH, encoding="utf-8") as fh:
        for line in fh:
            entry = line.split("#")[0].strip()
            if entry:
                allow.add(entry)
    problems: list[str] = []
    for sub in sorted(os.listdir(SRC_ROOT)):
        subdir = os.path.join(SRC_ROOT, sub)
        if not os.path.isdir(subdir):
            continue
        for fn in sorted(os.listdir(subdir)):
            if not fn.endswith((".h", ".cc")):
                continue
            path = os.path.join(subdir, fn)
            audit_includes(path, allow, problems)
            audit_unsafe_buffers(path, problems)
            if fn.endswith(".h") and "unittest" not in fn:
                audit_style(path, problems)
    if problems:
        print("AUDIT FAIL — %d problem(s):" % len(problems))
        for p in problems:
            print("  " + p)
        return 1
    print("audit OK: style + include allowlist clean")
    return 0


if __name__ == "__main__":
    sys.exit(main())
