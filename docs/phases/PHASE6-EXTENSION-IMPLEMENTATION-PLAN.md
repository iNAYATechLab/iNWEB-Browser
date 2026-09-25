# Phase 6 — Extension patches 0014–0017 implementation plan

**Branch:** `feature/extensions-0014-0017`

**Base:** `origin/main` at `02f1f72`

**Status:** pre-code plan

## 1. Current implementation inspected

- Pinned Chromium `154.0.8037.21` already contains the experimental
  `enable_desktop_android_extensions` path, Android extension Java/C++
  bridges, management WebUI, app-menu entries, toolbar action UI, popup
  contents, options routing, JNI headers, resources, and tests.
- The regular-phone build leaves that path disabled. iNWEB currently builds no
  extension runtime and exposes no extension destination.
- The repository already has a tested pure-JVM extension registry and authored
  native CRX/ZIP inspection sources, but none are patched into Chromium.
- The old Phase 6 document numbers extension work as `0013`–`0016`; `0013` is
  now the app-layer probe, so extension work is renumbered to `0014`–`0017`.

## 2. Honesty and safety boundary

Preparation does not equal support. No extension UI is exposed and no API is
reported supported until the pinned Chromium build, API audit, and device
matrix prove it. Unsupported operations must fail explicitly; no placeholder
success, fake count, Web Store installation claim, or unverified capability is
allowed.

The current hop-32 continues independently at commit
`ebd638afb82dba624a4255ee3e7c0ead5a8bbb1b`. These patches are not added to
that running build.

## 3. Patch split

### 0014 — `extension-enable-android`

- opt the iNWEB GN configs into Chromium's existing experimental
  `enable_desktop_android_extensions` path (not the broader desktop-only
  `enable_extensions` path);
- include the build-time API audit tool;
- update the Phase 6 facts and patch registry;
- do not expose a navigation destination merely because the code compiles.

### 0015 — `extension-management-ui`

- add the authored CRX3/ZIP structural inspector and its native unit tests;
- bind only to the already-built upstream Android management surfaces after a
  real runtime availability signal;
- preserve sideload review-before-enable and manual update ordering;
- keep Chrome Web Store as discovery only, never promise direct installation.

### 0016 — `extension-action-surfaces`

- use Chromium's existing Android toolbar action, popup, and options routing;
- add no fake action model;
- expose surfaces only when the real extension backend is enabled for the
  profile;
- audit the resulting compiled API/surface registrations.

### 0017 — `extension-policy-wiring`

- enforce permission review, disable/remove paths, update consent, MV2 notice,
  and the built-in protection precedence/kill-switch boundary;
- unsupported APIs return failure and stay out of the advertised support
  matrix;
- record the measured API matrix from the artifact, never the design target.

## 4. Provider follow-up merged after PR #4

Before extension changes, audit the new approved Popular Sites provider:

- `PopularSite` always receives non-blank destination/accessibility names;
- catalog admission cannot be bypassed through a public arbitrary-list seam;
- approved locale variants resolve only to explicitly approved URLs;
- the shipped empty catalog continues to hide the section.

This is a separate commit and remains independent of the extension patches.

## 5. Validation

Each patch will be validated independently and cumulatively:

1. registry/schema and patch path lint;
2. patch apply, reverse, and re-apply on a minimal pinned-tree fixture;
3. GN label/path validation;
4. host native installer tests;
5. pure-JVM tests;
6. build-time API audit against a real artifact when available;
7. pinned Chromium build hop, then device tests before any availability claim.

A patch that has not reached steps 6–7 remains prepared/unproven and cannot
turn on user-facing extension capability.
