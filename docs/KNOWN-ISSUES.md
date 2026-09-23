# Known Issues — iNWEB Browser

Honest, dated record of open product issues. Every entry states what is
known, what is NOT known, and the evidence needed to close it. No issue
is silently dropped; fixed issues move to the "Resolved" section with
the fix commit and verification evidence.

---

## Open

### K-1 — Browser hangs / becomes unresponsive under heavy load

- **Reported:** 2026-09-23, by the product owner (user report, Bangla:
  "মাঝে মাঝে হ্যাং করতেছে সঠিকভাবে কাজ করতে চাই না লোড বেশি পড়লে").
- **Affected build:** v1.0.0-alpha.1 (the only released APK to date).
- **Symptom:** intermittent hangs / unresponsiveness when the load is
  high (many tabs / heavy pages — exact trigger not yet characterized).
- **What is known:**
  - alpha.1 contains the pinned Chromium 154 base + the ui/branding
    patches (0001–0004) + the browser shell — it does **NOT** contain
    any Stage-2 native code (adblock engine, popup guards: those are
    patch-series only, never compiled into an APK yet).
  - Therefore the hang predates our tracking-protection native code;
    candidate sources are (a) base Chromium behavior on the device,
    (b) the browser shell layer, (c) device resource limits (RAM
    class), or a combination. No cause is confirmed.
- **What is NOT known:** device model/RAM/Android version, whether it
  is an ANR (input-dispatch timeout), renderer kill, or memory
  thrash; whether it reproduces on other devices.
- **Evidence needed to close:** an ANR trace (`adb bugreport` or the
  `/data/anr/` trace file) or logcat around a hang; device model +
  RAM; a rough reproduction recipe (tab count, site). Voluntary — the
  device-matrix load test below proceeds regardless.
- **Plan (already scheduled work, no new claims):**
  1. B-5..B-8 device-matrix session gains an explicit heavy-load
     scenario (many tabs + heavy page + rapid tab switching) on the
     low-RAM device class (§48 RAM-class coverage) — attempt
     reproduction with perfetto/meminfo capture per PHASE5 §5.
  2. The alpha.2 build (incremental from state-13 with the Stage-2
     patches) will be A/B-compared for the same scenario — the
     combined matcher's device budgets (PHASE5 §2/§5) must hold, i.e.
     our patches must not add to the problem.
  3. PHASE12 production-hardening review re-examines main-thread work
     in the shell for anything that could block input under load.
- **Status:** open — triage, not diagnosed. No fix claimed.

---

## Resolved

(none yet)
