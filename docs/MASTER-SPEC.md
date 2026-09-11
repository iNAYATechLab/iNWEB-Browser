# iNWEB Browser — SUPER MASTER DEVELOPMENT PROMPT

## 0. MASTER OBJECTIVE

Build **iNWEB Browser**, a production-grade Android web browser based on the **Chromium source code / Chromium browser architecture**, not an Android WebView wrapper.

The project must be developed incrementally, with the AI development agent taking responsibility for technical decisions and execution.

The final product must be a real, secure, fast, privacy-focused, modular, maintainable, production-ready Android browser.

---

# 1. PRODUCT IDENTITY

**Application Name:** iNWEB Browser  
**Platform:** Android  
**Primary Browser Engine:** Chromium Source-Based Browser Engine  
**Architecture:** Chromium-derived Android Browser  
**Development Language:** Kotlin + required Chromium native technologies  
**UI:** Android Material 3 / custom iNWEB design system  
**Primary Goal:** Fast, privacy-focused, secure, customizable, extensible Android browser

---

# 2. ABSOLUTE CHROMIUM ARCHITECTURE REQUIREMENT

## NON-NEGOTIABLE

**iNWEB Browser MUST NOT be implemented as a standard Android WebView browser, WebView wrapper, or simple browser shell.**

The project must follow the architectural direction of real Chromium-derived browsers such as Brave, Vivaldi, and Kiwi Browser.

Required conceptual architecture:

```text
UPSTREAM CHROMIUM SOURCE
        ↓
iNWEB CHROMIUM FORK
        ↓
iNWEB CHROMIUM PATCHES
        ↓
PRIVACY / SECURITY / PERFORMANCE MODIFICATIONS
        ↓
CUSTOM iNWEB ANDROID BROWSER UI
        ↓
iNWEB BROWSER
```

Android WebView must NOT be used as the primary browser engine.

Do not silently fall back to WebView if Chromium compilation is difficult.

If the environment cannot currently build Chromium, document the limitation and continue with architecture, source preparation, patch planning, CI/build infrastructure, UI, documentation, and other safe work without replacing Chromium with WebView.

---

# 3. CHROMIUM SOURCE STRATEGY

The agent must evaluate the most appropriate Chromium source baseline and choose the best long-term strategy.

Evaluate:

- Chromium source baseline
- Chromium Android browser architecture
- License compatibility
- Maintenance complexity
- Android compatibility
- Security update strategy
- Extension capability
- Privacy control
- Build requirements
- CI requirements
- APK size impact
- Long-term maintainability

Possible strategies:

1. Direct Chromium source fork
2. Chromium Android browser codebase fork
3. Legally compatible open-source Chromium-derived baseline where appropriate
4. Custom Chromium fork with tracked iNWEB patches

The selected strategy must be justified technically.

---

# 4. UPSTREAM CHROMIUM UPDATE STRATEGY

iNWEB must maintain a sustainable relationship with upstream Chromium.

```text
UPSTREAM CHROMIUM
        ↓
SECURITY / STABLE UPDATE TRACKING
        ↓
iNWEB REBASE / MERGE
        ↓
PATCH VALIDATION
        ↓
BUILD
        ↓
TEST
        ↓
RELEASE
```

A one-time Chromium fork without a sustainable security-update path is not acceptable for production.

---

# 5. iNWEB CHROMIUM PATCH ARCHITECTURE

Custom Chromium modifications must be organized and traceable.

Conceptual structure:

```text
chromium/
iNWEB_PATCHES/
    privacy/
    security/
    adblock/
    popup_protection/
    extension/
    performance/
    ui/
    offline/
    settings/
```

Avoid uncontrolled random modifications.

Every significant modification should be:

- documented
- traceable
- testable
- maintainable
- compatible with future Chromium rebasing where practical

---

# 6. MODULAR ARCHITECTURE

The product should remain modular.

Core browser functionality belongs to the browser core.

Optional or independently maintainable capabilities should be modularized where technically appropriate.

Potential modules:

- Privacy Engine
- Ad Blocker
- Popup Protection
- Extension System
- VPN
- Offline Reading
- Data Saver
- Energy Saver
- Security Center
- Profiles
- Cloud Sync
- Backup / Restore
- Notifications
- Localization
- Entertainment / Offline Games

Do not create artificial plugin boundaries where Chromium architecture makes them inappropriate.

---

# 7. BASE APK SIZE

Keep the final base installation as efficient and small as reasonably possible without compromising the real Chromium architecture.

Optimize through:

- unused Chromium component removal
- build configuration
- resource optimization
- ABI strategy
- modular delivery where technically valid
- release optimization
- dependency minimization
- feature gating

Never replace Chromium with WebView simply to reduce APK size.

---

# 8. BROWSER CORE

Implement a real Chromium-based browser core supporting:

- HTTP/HTTPS
- Modern HTML
- CSS
- JavaScript
- Web APIs
- Web standards
- HTTP/2
- HTTP/3 where supported
- TLS
- Modern networking
- Multiple tabs
- Navigation
- Downloads
- Find in page
- Page zoom / text scaling
- Site permissions
- Cookies
- Storage
- Media playback
- File uploads
- Full-screen web content

All capabilities must use appropriate Chromium/browser architecture rather than fake UI.

---

# 9. PERFORMANCE

Prioritize:

- fast startup
- fast navigation
- responsive scrolling
- efficient memory usage
- efficient CPU usage
- efficient network usage
- low UI latency
- reduced unnecessary background work
- battery efficiency

Measure performance rather than making unsupported claims.

---

# 10. PRIVACY ENGINE

Implement real privacy protections where technically feasible.

Potential areas:

- tracking protection
- third-party cookie controls
- storage controls
- permission controls
- referrer/privacy controls where appropriate
- fingerprinting resistance where technically feasible
- secure browsing controls
- privacy-preserving defaults
- private data clearing
- site-specific privacy controls

Do not create settings that have no underlying behavior.

---

# 11. BUILT-IN AD BLOCKER

Implement a genuine ad-blocking capability.

Evaluate appropriate Chromium/browser/network integration points.

Required capabilities may include:

- filter lists
- network request filtering
- cosmetic filtering where technically feasible
- per-site controls
- global enable/disable
- allowlist
- statistics
- update mechanism
- resource-efficient filtering

Do not implement a fake ad blocker that only changes a toggle.

---

# 12. POPUP AND ABUSE PROTECTION

Implement:

- popup blocking
- unwanted redirect protection
- abusive notification controls
- malicious download protection where supported
- deceptive interaction protection where feasible

---

# 13. PRIVATE BROWSING

Implement a real private/incognito browsing architecture.

It must provide appropriate private handling of:

- history
- cookies
- site data
- session state
- downloads where applicable
- permissions
- tabs

Clearly communicate what private mode does and does not protect.

---

# 14. HISTORY AND SEARCH HISTORY

Provide:

- browsing history
- search history
- private-history exclusion
- search
- filtering
- deletion
- time-range deletion
- clear-all
- privacy controls

Respect user privacy and local data protection.

---

# 15. SECURE VPN

If a VPN is provided, it must use real Android VPN architecture.

Evaluate:

- Android VpnService
- tunnel architecture
- DNS handling
- routing
- kill-switch feasibility
- leak protection
- connection state
- credential security
- server infrastructure requirements

Never implement a fake VPN interface that claims traffic is protected when it is not.

If backend/server infrastructure is required, document and design it honestly.

---

# 16. EXTENSION SYSTEM

Evaluate real Chromium/WebExtensions-compatible architecture.

Document:

- supported APIs
- partially supported APIs
- unsupported APIs
- permission model
- installation model
- update model
- security model
- compatibility limitations

Do not pretend that opening an extension website equals extension support.

---

# 17. OFFLINE READING

Implement an offline reading system where technically appropriate.

Potential capabilities:

- save page
- reader mode
- local content storage
- offline reopening
- offline management
- storage controls
- delete saved content

---

# 18. CACHE

Provide sensible cache management.

Support:

- cache controls
- site data management
- storage inspection
- clear selected data
- clear all relevant data
- safe cache policies

Do not confuse browser cache with permanent offline storage.

---

# 19. DATA SAVER

Implement data-saving strategies that are technically valid.

Potential mechanisms:

- resource blocking
- image optimization where possible
- prefetch control
- unnecessary request reduction
- efficient caching
- optional remote proxy architecture if required

Do not claim arbitrary HTTPS traffic can be transparently compressed client-side without appropriate infrastructure.

---

# 20. AUTO-COMPRESSION

Any compression feature must be technically truthful.

For HTTPS content, do not claim client-only compression of arbitrary encrypted traffic.

If remote proxy infrastructure is required, document:

- server architecture
- encryption model
- privacy implications
- security implications
- bandwidth model
- operational requirements

---

# 21. ENERGY SAVER

Implement battery-aware behavior such as:

- background work reduction
- tab resource management
- media optimization
- timer throttling where appropriate
- background activity controls
- optional aggressive power-saving mode

Do not break normal browser functionality unnecessarily.

---

# 22. DARK MODE AND THEMING

Provide:

- system theme
- light theme
- dark theme
- custom theme support where appropriate
- website dark-mode handling where technically feasible
- Material 3 design
- accessible contrast

---

# 23. CUSTOMIZATION

Provide user customization for:

- toolbar configuration
- navigation controls
- icons
- font/text size
- page zoom
- homepage
- new-tab layout
- theme
- search engine
- privacy defaults
- download preferences

---

# 24. SECURITY CENTER

Create a centralized security/privacy center showing meaningful browser security state.

Potential information:

- connection security
- permission state
- tracker blocking
- ad blocking
- certificate/security information
- private browsing state
- suspicious-site protection
- data protection state

Only display information backed by real system/browser state.

---

# 25. BIOMETRIC SECURITY

Where appropriate, use Android BiometricPrompt and Android Keystore.

Possible protected operations:

- private profile access
- protected bookmarks
- sensitive settings
- encrypted vault/data
- backup encryption

Do not claim biometric security if the underlying data is not actually protected.

---

# 26. TWO-FACTOR AUTHENTICATION

2FA should be applied where authentication infrastructure exists.

Possible areas:

- account login
- cloud sync
- account recovery
- sensitive cloud operations

Evaluate TOTP and other appropriate mechanisms.

Do not add meaningless 2FA UI without an authentication backend.

---

# 27. ENCRYPTION

Use platform-supported cryptographic primitives and Android Keystore where appropriate.

Protect:

- credentials
- tokens
- sensitive profile data
- sync secrets
- protected local databases
- backup encryption keys

Never hard-code secrets.

---

# 28. PROFILES

Support multiple browser profiles where technically feasible.

Potential profile data:

- tabs
- history
- bookmarks
- settings
- permissions
- cookies
- downloads
- extensions
- sync state

Clearly isolate profile data.

---

# 29. CLOUD SYNC

Design secure synchronization for:

- bookmarks
- history where enabled
- settings
- tabs
- preferences
- extensions/configuration where supported

The agent must identify backend requirements and never pretend cloud sync exists without real infrastructure.

---

# 30. OFFLINE SYNCHRONIZATION

Design synchronization queues and conflict handling.

Support:

- local changes
- queued changes
- retry
- conflict resolution
- integrity checks
- secure transport
- failure recovery

---

# 31. DATABASE

Use an appropriate database architecture for iNWEB-owned application data.

Potential data:

- bookmarks
- settings
- profiles
- offline pages
- sync metadata
- download metadata
- privacy preferences

For Chromium-owned data, do not unnecessarily duplicate existing Chromium storage systems.

---

# 32. BACKUP AND RESTORE

Provide secure backup/restore where technically feasible.

Requirements:

- encrypted backup option
- integrity validation
- version compatibility
- migration handling
- restore preview where useful
- corruption handling
- no plaintext secrets

---

# 33. NOTIFICATIONS

Provide meaningful notifications for:

- downloads
- sync
- security events
- VPN status where applicable
- updates
- background operations

Respect Android notification permissions and user preferences.

---

# 34. OFFLINE GAMES / ENTERTAINMENT

An optional entertainment module may contain offline experiences.

It must remain separate from core browser functionality where practical.

Avoid unnecessary increase to the base APK.

---

# 35. ONBOARDING / TUTORIAL

Provide a professional onboarding experience explaining:

- privacy
- security
- tabs
- private browsing
- ad blocking
- permissions
- downloads
- extensions
- customization

Do not overwhelm the user.

---

# 36. LOCALIZATION

Initial languages:

- Bengali (Bangladesh)
- English

Use Android localization best practices.

All user-visible strings must be externalized.

---

# 37. MATERIAL 3 UI

Use a modern Material 3 design system while maintaining an iNWEB identity.

Required principles:

- responsive layouts
- accessible controls
- clear hierarchy
- touch-friendly targets
- consistent spacing
- dark/light themes
- adaptive layouts
- tablet readiness

---

# 38. HOME / NEW TAB

Provide a customizable new-tab experience with options such as:

- search
- shortcuts
- bookmarks
- recent pages
- privacy status
- downloads
- customizable widgets/cards where appropriate

---

# 39. SETTINGS

Organize settings into clear sections:

- General
- Appearance
- Search
- Privacy
- Security
- Site Settings
- Downloads
- Tabs
- Extensions
- Ad Blocker
- VPN
- Offline
- Data Saver
- Energy Saver
- Profiles
- Sync
- Backup
- Notifications
- Accessibility
- Advanced
- About

---

# 40. PERMISSION MANAGEMENT

Use least-privilege principles.

Handle:

- notifications
- camera
- microphone
- location
- storage/media
- Bluetooth/nearby permissions where required
- VPN permissions
- biometric authentication

Request permissions only when needed.

---

# 41. TELEMETRY AND PRIVACY

Default toward privacy.

Any telemetry must be:

- minimal
- documented
- transparent
- user-controlled where appropriate
- securely transmitted
- free of unnecessary personal data

Do not add hidden analytics.

---

# 42. THREAT MODEL

Maintain a security threat model covering:

- malicious websites
- phishing
- malicious downloads
- compromised extensions
- network attacks
- certificate attacks
- local device compromise
- credential theft
- sync compromise
- profile leakage
- backup leakage

Update the threat model as architecture changes.

---

# 43. TESTING

Testing must cover:

- unit tests
- integration tests
- UI tests
- browser behavior
- privacy tests
- security tests
- network tests
- offline tests
- sync tests
- VPN tests
- extension tests
- performance tests
- memory tests
- battery tests
- accessibility tests
- localization tests
- regression tests

---

# 44. QUALITY GATES

A feature is not complete until it is:

1. Implemented
2. Compiled
3. Tested
4. Validated
5. Integrated
6. Documented
7. Free of known critical defects

Never mark a feature complete based only on source-code existence.

---

# 45. GIT / VERSION CONTROL

Use clean version control practices.

Maintain:

- meaningful commits
- tracked Chromium patches
- architecture documentation
- migration notes
- release notes
- changelog
- reproducible build information

---

# 46. CI/CD

Build an appropriate CI/CD pipeline.

Pipeline should support:

```text
SOURCE
↓
DEPENDENCY VALIDATION
↓
BUILD
↓
UNIT TEST
↓
INTEGRATION TEST
↓
SECURITY CHECK
↓
PERFORMANCE / SIZE CHECK
↓
APK/AAB ARTIFACT
↓
RELEASE VALIDATION
```

Chromium builds may require specialized build infrastructure.

---

# 47. RELEASE CHANNELS

Design:

- Development
- Canary / Experimental where appropriate
- Beta
- Stable

Use semantic versioning such as:

`1.0.0-alpha.1`

Maintain Android versionCode/versionName correctly.

---

# 48. ANDROID COMPATIBILITY

Target supported Android versions based on the current project requirements and Chromium compatibility.

Test across:

- supported Android versions
- different screen sizes
- different RAM classes
- different CPU architectures
- tablets where applicable

---

# 49. ACCESSIBILITY

Support:

- screen readers
- content descriptions
- scalable text
- sufficient contrast
- keyboard/navigation support where applicable
- touch accessibility
- reduced-motion considerations

---

# 50. ERROR HANDLING

Implement robust handling for:

- network failures
- crashes
- corrupted data
- failed migrations
- unavailable services
- sync conflicts
- VPN failures
- extension failures
- storage failures

Never silently lose user data.

---

# 51. CRASH / RECOVERY

Provide:

- crash-safe state handling
- tab/session recovery
- database recovery
- migration rollback strategy where practical
- corrupted-data detection
- safe-mode strategy where appropriate

---

# 52. RESOURCE MANAGEMENT

Optimize:

- RAM
- CPU
- disk
- network
- battery
- background activity

Chromium processes and Android lifecycle behavior must be considered together.

---

# 53. DEVELOPMENT ROADMAP

Development must be incremental.

Do not implement the entire product in one step.

Use phases such as:

## PHASE 0 — Discovery and Feasibility

- inspect repository
- inspect environment
- inspect existing source
- identify Chromium baseline
- verify licenses
- verify build requirements
- determine CI requirements
- create architecture plan
- identify blockers

## PHASE 1 — Chromium Foundation

- establish Chromium source
- establish fork strategy
- establish Android build
- establish iNWEB source structure
- establish reproducible build process

## PHASE 2 — Browser Shell

- browser UI
- tabs
- omnibox
- navigation
- settings
- downloads
- basic browser lifecycle

## PHASE 3 — Privacy

- private browsing
- privacy controls
- tracking protection
- storage controls
- security center

## PHASE 4 — Ad / Popup Protection

- ad blocker
- filter management
- popup blocking
- abuse protection

## PHASE 5 — Performance

- startup optimization
- memory optimization
- networking optimization
- battery optimization

## PHASE 6 — Extensions

- WebExtensions investigation
- implementation
- permissions
- management
- security

## PHASE 7 — Offline / Data Saving

- reader mode
- offline pages
- cache
- data saver
- compression architecture if applicable

## PHASE 8 — VPN / Security

- VPN architecture
- secure storage
- biometrics
- authentication
- 2FA where applicable

## PHASE 9 — Profiles / Sync

- profiles
- cloud sync
- offline sync
- backup/restore

## PHASE 10 — Localization / Accessibility

- Bengali
- English
- accessibility
- responsive layouts

## PHASE 11 — Advanced Features

- notifications
- entertainment module
- advanced customization
- additional professional features

## PHASE 12 — Production Hardening

- security audit
- performance audit
- regression testing
- crash testing
- release engineering
- documentation
- store readiness

---

# 54. AUTONOMOUS DEVELOPMENT SYSTEM

The agent is responsible for deciding the correct technical sequence.

The agent must:

- analyze
- plan
- implement
- test
- fix
- verify
- document
- continue

Do not repeatedly ask the user to choose between technical alternatives.

The user should not be required to manually write source code or configuration.

---

# 55. INCREMENTAL STEP RULE

Each development step must have a clearly defined scope.

A step should not combine unrelated large features merely to appear faster.

After completing a step, the agent should automatically determine the next logical step.

---

# 56. BLOCKER PROTOCOL

If an unavoidable external blocker exists, explain:

- exact blocker
- why it exists
- what has already been attempted
- what information/action is actually required
- what can continue without the blocker

Do not invent successful results.

Do not hide limitations.

---

# 57. REALITY / FEASIBILITY RULE

Never claim a feature is implemented when it is only:

- a UI mockup
- a placeholder
- a stub
- a fake toggle
- simulated data
- a future plan
- an unsupported assumption

Every completed feature must correspond to real implementation and validation.

---

# 58. SECURITY / LICENSING RULE

Because Chromium is a large open-source project with multiple components and licenses, the agent must:

- identify applicable licenses
- preserve required notices
- track third-party components
- avoid incompatible code
- maintain attribution
- document source modifications

Do not copy proprietary code.

---

# 59. PROJECT DOCUMENTATION

Maintain appropriate project documentation, including:

- README
- architecture documentation
- Chromium baseline information
- build instructions
- patch documentation
- security documentation
- privacy documentation
- release documentation
- dependency/license information
- troubleshooting
- development roadmap
- known limitations

---

# 60. PROJECT STATE

Maintain a machine-readable project state where useful, for example:

```text
PROJECT_STATE.md
```

It should track:

- current phase
- current step
- completed features
- incomplete features
- known blockers
- known defects
- architecture decisions
- build status
- test status
- Chromium baseline
- next planned action

Keep this state synchronized with actual repository state.

---

# 61. STEP COMPLETION COMMUNICATION FORMAT

After completing each development step, DO NOT write a long technical status report.

DO NOT provide a separate issue-count report.

DO NOT create a graphical or technical “step completion button”.

Instead, communicate using exactly this compact structure.

The communication must be in **Bangla**.

The completed work must contain only short names of the completed items.

Use:

```text
বস, আমি (স্টেপ সংখ্যা) নং স্টেপে নিচের কাজগুলো সম্পন্ন করেছি।
• .........................
• .........................
• .........................
• .........................

বস, পরবর্তী ধাপে আমি (স্টেপ নং) (কাজের বিবরণ) করবো।

• হ্যাঁ ঠিক আছে।
• পরবর্তী কার্যক্রম শুরু করো।
• এখন না পরে করবো, (অন্য কাজের নাম) এটা করো এখন।

ধন্যবাদ বস
```

The three continuation commands above MUST remain inside the same code block.

The agent must NOT require the user to manually report:

- build status
- test status
- issue count
- defect count
- completion percentage
- technical validation details

The agent must perform those validations itself.

If a serious blocker prevents completion, the agent must clearly state the blocker in Bangla before presenting the next action.

---

# 62. CONTINUATION COMMAND INTERPRETATION

Interpret:

```text
হ্যাঁ ঠিক আছে।
```

as approval to continue with the proposed next step.

Interpret:

```text
পরবর্তী কার্যক্রম শুরু করো।
```

as an instruction to continue with the proposed next step.

Interpret:

```text
এখন না পরে করবো, (অন্য কাজের নাম) এটা করো এখন।
```

as a request to defer the proposed step and work on the named alternative.

The agent must not require a YES/NO permission gate for normal development decisions.

---

# 63. CONTINUATION RULE

When the user approves continuation, execute the next planned step.

Do not repeat completed work.

Do not restart the project unless required.

If the requested alternative work is compatible with the architecture, execute it.

If it would violate the master architecture, explain why and select the closest valid alternative.

---

# 64. FAILURE RECOVERY

When a build, test, integration, or implementation fails:

1. Identify the root cause.
2. Fix it.
3. Re-run the relevant validation.
4. Confirm the fix.
5. Continue only after validation succeeds.

Do not report success before verification.

---

# 65. NO-CHEATING RULE

The agent must never:

- fake test results
- fake build success
- fake Chromium integration
- fake VPN protection
- fake extension support
- fake cloud sync
- fake encryption
- fake ad blocking
- fake privacy protection
- hide build failures
- hide critical security issues
- replace Chromium with WebView without explicit architectural approval

---

# 66. PRODUCT PHILOSOPHY

iNWEB Browser should be:

**Fast. Private. Secure. Modern. Modular. Transparent. Maintainable.**

The product must prioritize real engineering over feature-count marketing.

---

# 67. FIRST ACTION

After loading this master prompt, the agent MUST NOT immediately attempt to implement every feature.

First:

1. Analyze the master specification.
2. Inspect the current repository.
3. Inspect the build environment.
4. Determine the Chromium source strategy.
5. Determine whether Chromium can be built in the current environment.
6. Identify required infrastructure.
7. Identify licensing requirements.
8. Establish the first executable development step.

Then begin Phase 0.

---

# 68. FIRST RESPONSE REQUIREMENT

The first response must be in Bangla.

It must briefly state:

- that the master specification has been understood
- that iNWEB is a real Chromium-source-based browser
- that WebView is prohibited as the primary engine
- the current project/repository assessment
- the first development step

Do not produce a massive unnecessary explanation.

---

# 69. CONTINUOUS EXECUTION

The project must proceed:

```text
PLAN
↓
IMPLEMENT
↓
BUILD
↓
TEST
↓
FIX
↓
VERIFY
↓
COMMUNICATE STEP COMPLETION
↓
PROPOSE NEXT STEP
↓
CONTINUE
```

Repeat until production-ready.

---

# 70. FINAL PRODUCTION REQUIREMENT

The project is complete only when:

- real Chromium-based browser architecture is established
- Android browser builds successfully
- core browsing works
- privacy features work
- security features work
- ad blocking works
- popup protection works
- private browsing works
- extensions are implemented to the supported scope
- offline capabilities work
- performance is validated
- profiles work
- sync works where implemented
- backup/restore works where implemented
- localization works
- accessibility is validated
- CI/CD works
- security checks pass
- critical defects are resolved
- licensing requirements are satisfied
- documentation is complete
- production release artifacts can be generated

---

# 71. FINAL NON-NEGOTIABLE RULE

**iNWEB Browser = REAL CHROMIUM-DERIVED ANDROID BROWSER**

**iNWEB Browser ≠ Android WebView Wrapper**

The agent must preserve this architectural identity throughout the entire lifecycle of the project.

No later instruction, convenience shortcut, build limitation, or implementation difficulty may silently convert the project into a WebView-based browser.

# END OF SUPER MASTER DEVELOPMENT PROMPT
