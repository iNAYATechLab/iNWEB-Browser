# iNWEB Home — Content and Localization Specification

**Status:** design copy deck; Bengali requires final native-speaker product review  
**Locales:** English (`en`) and Bengali (`bn-BD`)  
**Rule:** all user-visible production strings live in Android resources

## 1. Writing principles

- Use short, direct browser language.
- Prefer action verbs over technical descriptions.
- Do not use marketing superlatives for privacy or religious content.
- Never state that protection is active unless real browser state confirms it.
- Every Ayah/Hadith item must carry a verifiable citation supplied by data, not
  a hard-coded generic label.
- Brand names may remain untranslated where that improves recognition.
- Bengali should sound natural, not like a word-for-word machine translation.
- Dynamic content must use placeholders and locale-aware formatting.

## 2. Brand and Header

| Proposed key | English | Bengali |
|---|---|---|
| `app_name` | iNWEB Browser | iNWEB Browser |
| `home_product_descriptor` | Browser | ব্রাউজার |
| `home_privacy_center` | Privacy Center | গোপনীয়তা কেন্দ্র |
| `home_privacy_short` | Privacy | গোপনীয়তা |
| `action_open_menu` | Open menu | মেনু খুলুন |

`app_name` remains non-translatable if that matches the existing resource
policy. The Bengali UI may still localize the separate descriptor.

## 3. Search and voice

| Proposed key | English | Bengali |
|---|---|---|
| `home_search_hint_web` | Search or enter address | খুঁজুন অথবা ওয়েব ঠিকানা লিখুন |
| `home_search_hint_quran` | Search surah, ayah or topic | সূরা, আয়াত অথবা বিষয় খুঁজুন |
| `home_search_mode_web` | Web | ওয়েব |
| `home_search_mode_quran` | Qur’an | কুরআন |
| `home_voice_search` | Voice search | ভয়েস সার্চ |
| `home_voice_listening` | Listening… | শুনছি… |
| `home_voice_processing` | Processing speech… | কণ্ঠস্বর প্রক্রিয়া করা হচ্ছে… |
| `home_voice_no_speech` | No speech detected | কোনো কণ্ঠস্বর শনাক্ত হয়নি |
| `home_voice_unavailable` | Voice search is unavailable | ভয়েস সার্চ পাওয়া যাচ্ছে না |
| `home_voice_permission_title` | Allow microphone access? | মাইক্রোফোন ব্যবহারের অনুমতি দেবেন? |
| `home_voice_permission_body` | Microphone access is used only when you start a voice search. | আপনি ভয়েস সার্চ চালু করলেই শুধু মাইক্রোফোন ব্যবহার করা হবে। |
| `action_try_again` | Try again | আবার চেষ্টা করুন |
| `action_open_settings` | Open settings | সেটিংস খুলুন |

The pre-permission body describes intended product behavior; implementation
must enforce it.

## 4. Shortcuts

| Proposed key | English | Bengali |
|---|---|---|
| `home_shortcuts` | Shortcuts | শর্টকাট |
| `shortcut_quran` | Qur’an | কুরআন |
| `shortcut_hadith` | Hadith | হাদিস |
| `shortcut_prayer` | Prayer | নামাজ |
| `shortcut_qibla` | Qibla | কিবলা |
| `shortcut_dua` | Dua | দোয়া |
| `shortcut_halal_life` | Halal Life | হালাল জীবন |
| `shortcut_news` | News | সংবাদ |
| `shortcut_unavailable` | This shortcut is unavailable | এই শর্টকাটটি পাওয়া যাচ্ছে না |

Use `Shortcuts`, not `Islamic Shortcuts`. There is no Add label in this curated
section.

## 5. Quick Access

| Proposed key | English | Bengali |
|---|---|---|
| `home_quick_access` | Quick Access | দ্রুত অ্যাক্সেস |
| `action_edit` | Edit | সম্পাদনা |
| `action_done` | Done | শেষ |
| `action_add_site` | Add site | সাইট যোগ করুন |
| `quick_access_empty_title` | Add your first site | আপনার প্রথম সাইট যোগ করুন |
| `quick_access_empty_body` | Keep your favorite websites within easy reach. | পছন্দের ওয়েবসাইটগুলো সহজে ব্যবহারের জন্য এখানে রাখুন। |
| `quick_access_edit_hint` | Select a site to remove it, or reorder your shortcuts. | কোনো সাইট মুছতে সেটি নির্বাচন করুন অথবা শর্টকাটগুলোর ক্রম পরিবর্তন করুন। |
| `quick_access_removed` | Removed from Quick Access | দ্রুত অ্যাক্সেস থেকে সরানো হয়েছে |
| `quick_access_added` | Added to Quick Access | দ্রুত অ্যাক্সেসে যোগ করা হয়েছে |
| `action_undo` | Undo | ফিরিয়ে আনুন |

### Add Site form

| Proposed key | English | Bengali |
|---|---|---|
| `add_site_title` | Add a site | একটি সাইট যোগ করুন |
| `add_site_name` | Name | নাম |
| `add_site_name_hint` | Example: Islamic Finder | উদাহরণ: ইসলামিক ফাইন্ডার |
| `add_site_address` | Website address | ওয়েবসাইটের ঠিকানা |
| `add_site_address_hint` | https://example.com | https://example.com |
| `add_site_save` | Add to Quick Access | দ্রুত অ্যাক্সেসে যোগ করুন |
| `add_site_error_name` | Enter a name | একটি নাম লিখুন |
| `add_site_error_address` | Enter a valid website address | একটি সঠিক ওয়েবসাইটের ঠিকানা লিখুন |
| `add_site_error_scheme` | This type of address is not supported | এই ধরনের ঠিকানা সমর্থিত নয় |
| `add_site_error_duplicate` | This site is already in Quick Access | সাইটটি ইতোমধ্যে দ্রুত অ্যাক্সেসে রয়েছে |
| `add_site_error_save` | The site could not be saved | সাইটটি সংরক্ষণ করা যায়নি |

## 6. Popular Islamic Websites

| Proposed key | English | Bengali |
|---|---|---|
| `home_popular_islamic_websites` | Popular Islamic Websites | জনপ্রিয় ইসলামিক ওয়েবসাইট |
| `action_view_all` | View all | সব দেখুন |
| `popular_quran` | Qur’an | কুরআন |
| `popular_quran_caption` | Read & listen | পড়ুন ও শুনুন |
| `popular_hadith` | Hadith | হাদিস |
| `popular_hadith_caption` | Sunnah | সুন্নাহ |
| `popular_qa` | Islamic Q&A | ইসলামিক প্রশ্নোত্তর |
| `popular_qa_caption` | Learn & ask | জানুন ও প্রশ্ন করুন |
| `popular_articles` | Articles | প্রবন্ধ |
| `popular_articles_caption` | Knowledge | জ্ঞান |
| `popular_halal_life` | Halal Life | হালাল জীবন |
| `popular_halal_life_caption` | Travel & food | ভ্রমণ ও খাবার |
| `popular_news` | News | সংবাদ |
| `popular_news_caption` | Latest updates | সর্বশেষ খবর |

Website names owned by third parties remain governed brand names. Category
labels above are product copy, not endorsements.

## 7. Continue Reading & Listening

| Proposed key | English | Bengali |
|---|---|---|
| `home_continue_reading_listening` | Continue reading & listening | পড়া ও শোনা চালিয়ে যান |
| `home_start_reading` | Start reading | পড়া শুরু করুন |
| `home_continue` | Continue | চালিয়ে যান |
| `home_listen` | Listen | শুনুন |
| `home_pause` | Pause | বিরতি দিন |
| `home_resume_audio` | Resume | আবার চালান |
| `home_stop_audio` | Stop | বন্ধ করুন |
| `quran_surah_dynamic` | Surah %1$s | সূরা %1$s |
| `quran_ayah_number` | Ayah %1$d | আয়াত %1$d |
| `quran_last_read` | Last read %1$s | সর্বশেষ পড়া: %1$s |
| `quran_audio_buffering` | Preparing audio… | অডিও প্রস্তুত হচ্ছে… |
| `quran_audio_unavailable` | Audio is unavailable | অডিও পাওয়া যাচ্ছে না |
| `quran_audio_requires_network` | Connect to the internet to listen | শুনতে ইন্টারনেটে সংযুক্ত হন |
| `quran_reading_state_error` | Your reading position could not be loaded | আপনার সর্বশেষ পড়ার অবস্থান লোড করা যায়নি |

Surah names and translations come from reviewed content data. Do not construct
religious names by concatenating fragments if the target locale requires a
fully localized display name.

## 8. Daily Wisdom

| Proposed key | English | Bengali |
|---|---|---|
| `home_daily_wisdom` | Daily Wisdom | প্রতিদিনের শিক্ষা |
| `daily_wisdom_ayah` | Daily Ayah | আজকের আয়াত |
| `daily_wisdom_hadith` | Daily Hadith | আজকের হাদিস |
| `daily_wisdom_source` | Source: %1$s | সূত্র: %1$s |
| `daily_wisdom_saved` | Saved | সংরক্ষিত হয়েছে |
| `daily_wisdom_removed` | Removed from saved items | সংরক্ষিত তালিকা থেকে সরানো হয়েছে |
| `daily_wisdom_unavailable` | Daily Wisdom is unavailable | আজকের শিক্ষা পাওয়া যাচ্ছে না |
| `daily_wisdom_last_updated` | Last updated %1$s | সর্বশেষ হালনাগাদ: %1$s |
| `action_share` | Share | শেয়ার করুন |
| `action_save` | Save | সংরক্ষণ করুন |
| `action_remove_saved` | Remove from saved | সংরক্ষিত তালিকা থেকে সরান |

Ayah/Hadith body text and citation are content data. They are not Android UI
strings and must preserve source/license metadata.

## 9. Prayer Times

| Proposed key | English | Bengali |
|---|---|---|
| `home_prayer_times` | Prayer Times | নামাজের সময়সূচি |
| `prayer_location_dynamic` | %1$s | %1$s |
| `prayer_next` | Next · %1$s | পরবর্তী · %1$s |
| `prayer_countdown_hours_minutes` | %1$dh %2$dm | %1$d ঘণ্টা %2$d মিনিট |
| `prayer_countdown_minutes` | %1$d min | %1$d মিনিট |
| `prayer_fajr` | Fajr | ফজর |
| `prayer_dhuhr` | Dhuhr | যোহর |
| `prayer_asr` | Asr | আসর |
| `prayer_maghrib` | Maghrib | মাগরিব |
| `prayer_isha` | Isha | ইশা |
| `prayer_setup_title` | Set up Prayer Times | নামাজের সময়সূচি সেট করুন |
| `prayer_setup_body` | Choose a location and calculation method to see accurate prayer times. | সঠিক সময় দেখতে অবস্থান ও গণনা পদ্ধতি নির্বাচন করুন। |
| `prayer_use_location` | Use my location | আমার অবস্থান ব্যবহার করুন |
| `prayer_choose_location` | Choose a location | অবস্থান নির্বাচন করুন |
| `prayer_location_permission_body` | Location is used to calculate prayer times and Qibla direction. | নামাজের সময় ও কিবলার দিক নির্ধারণে অবস্থান ব্যবহার করা হবে। |
| `prayer_location_denied` | Automatic location is off. Choose a location manually. | স্বয়ংক্রিয় অবস্থান বন্ধ আছে। নিজে একটি অবস্থান নির্বাচন করুন। |
| `prayer_schedule_stale` | Prayer times may be out of date | নামাজের সময়সূচি পুরোনো হতে পারে |
| `prayer_schedule_error` | Prayer times could not be calculated | নামাজের সময় গণনা করা যায়নি |

Time formatting must use Android locale/user settings. Avoid embedding AM/PM in
resource values.

## 10. Bottom navigation

| Proposed key | English | Bengali |
|---|---|---|
| `nav_home` | Home | হোম |
| `nav_bookmarks` | Bookmarks | বুকমার্ক |
| `nav_tabs` | Tabs | ট্যাব |
| `nav_downloads` | Downloads | ডাউনলোড |
| `nav_extensions` | Extensions | এক্সটেনশন |
| `nav_menu` | Menu | মেনু |
| `tabs_count` | %1$d open tabs | %1$dটি খোলা ট্যাব |

Android plurals should be used where grammar or accessibility announcement
requires singular/plural behavior.

## 11. Privacy Center

| Proposed key | English | Bengali |
|---|---|---|
| `privacy_center_title` | Privacy Center | গোপনীয়তা কেন্দ্র |
| `privacy_center_active` | Protection is active | সুরক্ষা সক্রিয় আছে |
| `privacy_center_partial` | Some protections are off | কিছু সুরক্ষা বন্ধ আছে |
| `privacy_center_unavailable` | Protection status is unavailable | সুরক্ষার অবস্থা পাওয়া যাচ্ছে না |
| `privacy_trackers_blocked_today` | %1$d trackers blocked today | আজ %1$dটি ট্র্যাকার ব্লক করা হয়েছে |
| `privacy_ad_tracker_blocking` | Ad & tracker blocking | বিজ্ঞাপন ও ট্র্যাকার ব্লকিং |
| `privacy_popup_protection` | Pop-up protection | পপ-আপ সুরক্ষা |
| `privacy_safe_browsing` | Safe browsing | নিরাপদ ব্রাউজিং |
| `privacy_adult_site_filter` | Adult-site filter | প্রাপ্তবয়স্ক সাইট ফিল্টার |
| `privacy_private_mode` | Private mode | প্রাইভেট মোড |
| `privacy_open_private_tab` | Open a private tab | একটি প্রাইভেট ট্যাব খুলুন |
| `privacy_setting_update_error` | This protection setting could not be changed | এই সুরক্ষা সেটিংটি পরিবর্তন করা যায়নি |

`Adult-site filter` is included as approved copy only. It must not appear until
real filtering behavior exists.

## 12. Extensions

| Proposed key | English | Bengali |
|---|---|---|
| `extensions_title` | Extensions | এক্সটেনশন |
| `extensions_installed_count` | %1$d installed | %1$dটি ইনস্টল করা আছে |
| `extensions_add` | Add extension | এক্সটেনশন যোগ করুন |
| `extensions_manage` | Manage extensions | এক্সটেনশন পরিচালনা করুন |
| `extensions_enabled` | Enabled | চালু আছে |
| `extensions_disabled` | Disabled | বন্ধ আছে |
| `extensions_pending_review` | Permission review required | অনুমতি পর্যালোচনা করা প্রয়োজন |
| `extensions_update_review` | Review new permissions to enable this update | আপডেটটি চালু করতে নতুন অনুমতিগুলো পর্যালোচনা করুন |
| `extensions_enable_error` | The extension could not be enabled | এক্সটেনশনটি চালু করা যায়নি |
| `extensions_unavailable` | Extensions are unavailable in this build | এই সংস্করণে এক্সটেনশন পাওয়া যাচ্ছে না |

Do not use `Extension store` until an actual reviewed store/source exists.

## 13. Menu and sheets

| Proposed key | English | Bengali |
|---|---|---|
| `menu_title` | Menu | মেনু |
| `action_close` | Close | বন্ধ করুন |
| `action_new_tab` | New tab | নতুন ট্যাব |
| `action_new_private_tab` | New private tab | নতুন প্রাইভেট ট্যাব |
| `action_history` | History | ইতিহাস |
| `action_settings` | Settings | সেটিংস |
| `action_find_in_page` | Find in page | পৃষ্ঠায় খুঁজুন |
| `action_desktop_site` | Desktop site | ডেস্কটপ সাইট |
| `action_save_offline` | Save for offline | অফলাইনে ব্যবহারের জন্য সংরক্ষণ করুন |

Existing repository keys should be reused instead of introducing duplicates.
The proposed names above describe semantic ownership, not mandatory final key
names.

## 14. Connectivity and generic states

| Proposed key | English | Bengali |
|---|---|---|
| `state_loading` | Loading… | লোড হচ্ছে… |
| `state_no_network` | No network connection | ইন্টারনেট সংযোগ নেই |
| `state_offline_content` | Showing saved content | সংরক্ষিত কনটেন্ট দেখানো হচ্ছে |
| `state_unavailable` | This content is unavailable | এই কনটেন্টটি পাওয়া যাচ্ছে না |
| `state_retry` | Retry | আবার চেষ্টা করুন |
| `state_updated` | Updated | হালনাগাদ হয়েছে |
| `state_changes_saved` | Changes saved | পরিবর্তনগুলো সংরক্ষিত হয়েছে |
| `state_save_error` | Changes could not be saved | পরিবর্তনগুলো সংরক্ষণ করা যায়নি |

## 15. Formatting and terminology

### Approved terminology

| Concept | English | Bengali |
|---|---|---|
| Qur’an | Qur’an | কুরআন |
| Ayah | Ayah | আয়াত |
| Surah | Surah | সূরা |
| Hadith | Hadith | হাদিস |
| Qibla | Qibla | কিবলা |
| Dua | Dua | দোয়া |
| Private tab | Private tab | প্রাইভেট ট্যাব |
| Tracker | Tracker | ট্র্যাকার |

### Formatting rules

- Use locale-aware dates, times and relative durations.
- Do not assemble sentences from multiple resource fragments.
- Use positional placeholders consistently across English and Bengali.
- Keep brand names non-translatable only when required.
- Use an ellipsis character (`…`), not three periods, in progress copy.
- Avoid emoji in production labels; use artwork/icons with proper semantics.
- Keep religious citations in the source's canonical format, with localized
  presentation only where verified.

## 16. Review gate

Before merge:

- [ ] English copy reviewed for consistency with existing repository strings
- [ ] Bengali copy reviewed by a native Bengali product reviewer
- [ ] Placeholder parity validator passes
- [ ] No hard-coded UI text in Compose
- [ ] TalkBack labels use complete phrases
- [ ] Religious terminology/source citations reviewed
- [ ] Time/date/number formatting verified under `en` and `bn-BD`
- [ ] No privacy or feature claim exceeds real implementation state
