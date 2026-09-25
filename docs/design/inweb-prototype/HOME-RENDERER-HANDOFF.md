# iNWEB Home Renderer Draft — Lead Handoff

**Branch:** `feature/home-renderer-draft`
**Base:** `origin/main` at `fd92474`
**Renderer status:** source-level draft; not wired; no APK/device claim
**Blocker:** Lead-owned app-layer/Compose injection probe

## 1. Delivered Android paths

| Path | Change |
|---|---|
| `src/android-app/src/main/kotlin/com/inweb/browser/ui/home/InwebHomeRenderer.kt` | New thin Material 3 renderer over `HomePageModel` plus typed callback contract |
| `src/android-app/src/main/kotlin/com/inweb/browser/ui/home/HomeGlyph.kt` | New Compose UI-graphics glyphs; avoids unavailable Material icon dependencies and resource ownership drift |

The existing `ui/HomePage.kt`, `BrowserScreen.kt`, `OmniboxBar.kt`,
`BrowserBottomBar.kt`, `BrowserViewModel.kt`, theme, navigation, resources, and
core modules are intentionally unchanged. Strings are delivered below as the
blocking ID/en/bn catalog required by A9. The draft is not called until the
Lead proves injection, lands the catalog/resources, and performs final wiring.

## 2. Renderer contract

### State input

`InwebHomeRenderer(model: HomePageModel, actions: InwebHomeActions)` consumes:

| Model field | Rendering |
|---|---|
| `privacy` | Header Privacy Center chip; active/partial/loading/unavailable is announced truthfully; blocked count is not relabelled as “today” |
| `searchMode` | Web or Qur'an hint on a focus-forwarding search affordance; no second text input |
| `voiceSearchState` | Voice action only when not `UNSUPPORTED`; listening/processing/denied/error labels are accessible |
| `shortcuts` | Canonical curated utilities; `HIDDEN` omitted, `DISABLED` non-actionable, `AVAILABLE` emits utility ID |
| `quickAccess` | Explicit user-owned entries; URL open and Add callbacks only |
| `popularSites` | Reviewed HTTPS entries only; no renderer defaults |
| `continueReading` | First-use/loading/resume/error; unavailable omitted |
| `dailyWisdom` | Verified content/citation plus save/share; unavailable omitted |
| `prayerTimes` | Setup/loading/schedule/stale/error with locale/time-zone formatted real times; unavailable omitted |
| `browserSnapshot.recentPages` | Bounded rows, displayed count, See all; hidden when empty |
| `browserSnapshot.bookmarks` | Bounded rows, displayed count, See all; hidden when empty |
| `browserSnapshot.recentDownloads` | Bounded rows, real lifecycle labels, See all; hidden when empty |
| `visibleSections` | Core-authoritative visibility for designed Home sections |

`suggestedTopSites` and `offlinePages` remain available in the snapshot but are
not silently rebranded as curated Shortcuts or Quick Access.

### Action output

`InwebHomeActions` keeps platform behavior outside composables:

- focus authoritative omnibox
- start real voice flow
- open Privacy Center and Menu
- open one curated utility
- navigate a normal URL
- add Quick Access entry
- open one download
- open full Recent/Bookmarks/Downloads surfaces
- start/continue/retry reading
- save/share/retry wisdom
- configure/retry Prayer Times

No callback reports success. Persistence and provider completion remain the
integration layer's responsibility.

## 3. Final Home feed order

1. Header (brand, Privacy Center, Menu)
2. Search affordance (focuses authoritative omnibox)
3. Curated Shortcuts
4. Quick Access
5. Recent Pages snapshot, when non-empty (MASTER-SPEC §38 / A2)
6. Bookmarks snapshot, when non-empty (MASTER-SPEC §38 / A2)
7. Downloads snapshot, when non-empty (MASTER-SPEC §38 / A2)
8. Popular Islamic Websites
9. Continue Reading & Listening
10. Daily Wisdom
11. Prayer Times

Privacy Center is never duplicated as a bottom card. Full Recent, Bookmarks,
and Downloads management remains in navigation.

## 4. Omnibox rule

The Home search surface is a button-like affordance that emits
`onFocusOmnibox`. It contains no `TextField`, query state, keyboard action, URL
classification, or search-engine logic. `OmniboxBar` remains the sole widget
owner; `HomeSearchRouter`/`OmniboxParser` remain the sole classification path.

The integration must provide an explicit focus requester/controller seam. Do
not implement focus by creating a second input or copying text state.

## 5. Chromium paths for Lead integration

A1 confirms these pinned-tree touchpoints. The probe decides the exact minimal
subset; this is a path list, not permission to edit all files blindly.

### New iNWEB target/source area

- `chrome/android/inweb/home/BUILD.gn`
- `chrome/android/inweb/home/` generated/copied Kotlin source layout selected by the injection probe
- `chrome/android/BUILD.gn` dependency wiring

### NTP/Home hooks verified by Lead

- `chrome/android/java/src/org/chromium/chrome/browser/ntp/NewTabPage.java`
- `chrome/android/java/src/org/chromium/chrome/browser/ntp/NewTabPageCoordinator.java`
- `chrome/android/java/src/org/chromium/chrome/browser/ntp/NewTabPageLayout.java`
- `chrome/android/java/src/org/chromium/chrome/browser/ntp/NewTabPageManager.java`
- `chrome/android/java/src/org/chromium/chrome/browser/ntp/NewTabPageLayoutProperties.java`
- `chrome/android/java/src/org/chromium/chrome/browser/ntp/NewTabPageLayoutViewBinder.java`
- `chrome/android/java/src/org/chromium/chrome/browser/homepage/HomepageManager.java`
- `chrome/android/java/src/org/chromium/chrome/browser/homepage/HomepagePolicyManager.java`

### Strings/resources

- `chrome/browser/ui/android/strings/android_chrome_strings.grd`
- `chrome/android/java/res_chromium_base/`

The Lead owns patch generation, GN wiring, target visibility, resource-copy
rules, Compose compiler plugin proof, and `ui/0026-home-page`.

## 6. New string IDs

The same catalog is available in machine-readable form at
`docs/design/inweb-prototype/HOME-STRING-CATALOG.csv`. No Android resource file
is changed on this branch; the Lead must land these IDs before compiling the
renderer.

`home_product_name` is non-translatable brand text (`iNWEB`). The following
translatable IDs have English/Bengali parity:

| ID | English | Bengali |
|---|---|---|
| `home_product_descriptor` | Browser | ব্রাউজার |
| `home_open_menu` | Open menu | মেনু খুলুন |
| `home_privacy_center` | Privacy Center | গোপনীয়তা কেন্দ্র |
| `home_privacy_active` | Protection is active | সুরক্ষা সক্রিয় আছে |
| `home_privacy_partial` | Some protections are off | কিছু সুরক্ষা বন্ধ আছে |
| `home_privacy_loading` | Loading protection status | সুরক্ষার অবস্থা লোড হচ্ছে |
| `home_privacy_unavailable` | Protection status is unavailable | সুরক্ষার অবস্থা পাওয়া যাচ্ছে না |
| `home_search_hint_web` | Search or enter address | খুঁজুন অথবা ওয়েব ঠিকানা লিখুন |
| `home_search_hint_quran` | Search surah, ayah or topic | সূরা, আয়াত অথবা বিষয় খুঁজুন |
| `home_voice_search` | Voice search | ভয়েস সার্চ |
| `home_voice_listening` | Listening… | শুনছি… |
| `home_voice_processing` | Processing speech… | কণ্ঠস্বর প্রক্রিয়া করা হচ্ছে… |
| `home_voice_permission_denied` | Microphone permission is denied | মাইক্রোফোনের অনুমতি দেওয়া হয়নি |
| `home_voice_unavailable` | Voice search is unavailable | ভয়েস সার্চ পাওয়া যাচ্ছে না |
| `home_voice_error` | Voice search failed. Try again. | ভয়েস সার্চ ব্যর্থ হয়েছে। আবার চেষ্টা করুন। |
| `home_quick_access` | Quick Access | দ্রুত অ্যাক্সেস |
| `home_quick_access_empty` | Add your first site | আপনার প্রথম সাইট যোগ করুন |
| `home_add_site` | Add site | সাইট যোগ করুন |
| `home_items_shown` | `%1$d shown` | `%1$dটি দেখানো হচ্ছে` |
| `home_see_all` | See all | সব দেখুন |
| `home_popular_islamic_websites` | Popular Islamic Websites | জনপ্রিয় ইসলামিক ওয়েবসাইট |
| `home_shortcut_quran` | Qur’an | কুরআন |
| `home_shortcut_hadith` | Hadith | হাদিস |
| `home_shortcut_prayer` | Prayer | নামাজ |
| `home_shortcut_qibla` | Qibla | কিবলা |
| `home_shortcut_dua` | Dua | দোয়া |
| `home_shortcut_halal_life` | Halal Life | হালাল জীবন |
| `home_shortcut_news` | News | সংবাদ |
| `home_popular_quran` | Qur’an | কুরআন |
| `home_popular_hadith` | Hadith | হাদিস |
| `home_popular_qa` | Islamic Q&A | ইসলামিক প্রশ্নোত্তর |
| `home_popular_articles` | Articles | প্রবন্ধ |
| `home_popular_halal_life` | Halal Life | হালাল জীবন |
| `home_popular_news` | News | সংবাদ |
| `home_continue_reading_listening` | Continue reading & listening | পড়া ও শোনা চালিয়ে যান |
| `home_start_reading` | Start reading | পড়া শুরু করুন |
| `home_continue` | Continue | চালিয়ে যান |
| `home_reading_position` | `%1$s · Ayah %2$d` | `%1$s · আয়াত %2$d` |
| `home_daily_wisdom` | Daily Wisdom | প্রতিদিনের শিক্ষা |
| `home_wisdom_source` | `Source: %1$s` | `সূত্র: %1$s` |
| `home_save` | Save | সংরক্ষণ করুন |
| `home_remove_saved` | Remove from saved | সংরক্ষিত তালিকা থেকে সরান |
| `home_prayer_times` | Prayer Times | নামাজের সময়সূচি |
| `home_prayer_setup` | Set up Prayer Times | নামাজের সময়সূচি সেট করুন |
| `home_prayer_setup_action` | Choose location | অবস্থান বেছে নিন |
| `home_prayer_next` | `Next · %1$s · %2$s` | `পরবর্তী · %1$s · %2$s` |
| `home_prayer_stale` | Prayer times may be out of date | নামাজের সময়সূচি পুরোনো হতে পারে |
| `home_prayer_method` | `Method: %1$s · Asr: %2$s` | `পদ্ধতি: %1$s · আসর: %2$s` |
| `home_prayer_fajr` | Fajr | ফজর |
| `home_prayer_dhuhr` | Dhuhr | যোহর |
| `home_prayer_asr` | Asr | আসর |
| `home_prayer_maghrib` | Maghrib | মাগরিব |
| `home_prayer_isha` | Isha | ইশা |
| `home_retry` | Try again | আবার চেষ্টা করুন |
| `home_failure_offline` | No network connection | ইন্টারনেট সংযোগ নেই |
| `home_failure_load` | This content could not be loaded | এই কনটেন্ট লোড করা যায়নি |
| `home_failure_permission` | Permission is required | অনুমতি প্রয়োজন |
| `home_failure_unsupported` | This feature is not supported | এই ফিচারটি সমর্থিত নয় |

Existing IDs reused: `app_name`, `home_shortcuts`, `home_recent_pages`,
`action_bookmarks`, `action_downloads`, `action_share`, and all six
`download_state_*` IDs.

## 7. Asset/resource contract

The renderer deliberately does not invent final app resource IDs before the
probe proves the copy path. The Lead should map the governed prototype assets:

| Intended app ID | Prototype source |
|---|---|
| `inweb_home_header_ambient` | `assets/header-ambient.jpg` |
| `inweb_home_brand_mark` | `assets/logo-mark.jpg` |
| `inweb_home_continue_reading` | `assets/quran-ambient.jpg` |
| `inweb_home_daily_wisdom` | `assets/daily-wisdom.jpg` |
| `inweb_home_prayer_header` | `assets/prayer-dawn.jpg` |
| shortcut/site IDs | distinct files under the prototype's governed icon/artwork set |

Final image cards require dark directional scrims and decorative-image
semantics. The draft imports no Material icon package (absent from the pinned
Chromium tree). Its small Compose UI-graphics glyphs are only a source-build
fallback until the Lead maps governed drawables; they are not final
visual-asset approval.

## 8. Native/JNI assessment

**No native or JNI addition is required by the renderer itself.** It consumes
Kotlin state and emits callbacks. Chromium navigation/NTP hosting, focus bridge,
and lifecycle integration may require Java/Kotlin host glue, but not new C++ or
JNI unless the Lead's probe proves an unavoidable platform boundary.

**Recorded exception (A9):** real on-device blocking is enforced by C++ patches
`0005`–`0007`, which expose no counter. `HomePrivacyState.Active.blockedCount`
therefore has no truthful device bridge today. The renderer deliberately shows
only Active/Partial/Unavailable state and never renders the number. Any future
counter bridge is Lead-owned and outside `ui/0026`. Such a need must be raised
before implementation, not assumed.

## 9. Local verification boundary

Locally provable:

- Kotlin core behavior and tests
- XML well-formedness, string parity, placeholders, localization policy
- authored Kotlin structural syntax
- no hardcoded user-visible renderer strings
- ownership diff and byte-identical A7 mirrors

Not locally provable until Lead injection/build:

- Compose dependency/compiler-plugin wiring in GN
- Android resource generation for every new ID
- full type/dependency resolution of the authored app target
- NTP host attachment and omnibox focus bridge
- screenshots, TalkBack, 200% font scale, contrast on-device, IME behavior
- APK/device behavior and Chromium lifecycle correctness
