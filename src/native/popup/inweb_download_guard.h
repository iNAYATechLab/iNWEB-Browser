// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#ifndef CHROME_ANDROID_INWEB_POPUP_INWEB_DOWNLOAD_GUARD_H_
#define CHROME_ANDROID_INWEB_POPUP_INWEB_DOWNLOAD_GUARD_H_

#include <atomic>

#include "base/functional/callback.h"
#include "chrome/android/inweb/adblock/inweb_request_context.h"
#include "chrome/android/inweb/adblock/inweb_resource_type.h"
#include "url/gurl.h"

namespace inweb::popup {

// Download-protection decision, PHASE4-POPUP-PROTECTION §2D.
//
// Two layers, honestly scoped (§6: no remote reputation or scan
// service is claimed or bundled):
//
// 1. Automatic-download limiting — Chromium's existing
//    DownloadRequestLimiter + the AUTOMATIC_DOWNLOADS content setting
//    (default ASK) already require explicit user confirmation when a
//    site triggers multiple automatic (non-gesture) downloads. iNWEB
//    keeps that default, unchanged — no new code, no claim beyond it.
//
// 2. Engine-backed host checks — this guard: a download whose URL the
//    filter engine BLOCKs (host/object rules, consulted with
//    ResourceType::kObject through the process-wide holder) is
//    declined at the download-interception point
//    (ChromeDownloadManagerDelegate::InterceptDownloadIfApplicable).
//    Engine exceptions (@@) and the site allowlist are honored by the
//    engine's own decision order; when tracking protection is off the
//    engine reports kPass and every download proceeds.
enum class DownloadDecision {
  kAllow,
  kDeclinedByEngine,
};

// Download guard. The decision core is pure and unit-tested with a
// scriptable decider; the production instance consults the native
// ad-block engine holder (patches 0005–0007) with
// ResourceType::kObject, per the §2D "BLOCK decisions on OBJECT/OTHER
// downloads" rule.
class InwebDownloadGuard {
 public:
  using Decider =
      base::RepeatingCallback<adblock::FilterDecision(const adblock::RequestContext&)>;

  explicit InwebDownloadGuard(Decider decider);
  ~InwebDownloadGuard();

  InwebDownloadGuard(const InwebDownloadGuard&) = delete;
  InwebDownloadGuard& operator=(const InwebDownloadGuard&) = delete;

  // |download_url|: the URL of the file being downloaded.
  // |document_url|: the page that initiated the download (may be empty
  // for downloads without an initiating document).
  DownloadDecision ShouldDeclineDownload(const GURL& download_url,
                                         const GURL& document_url) const;

  // Real counter for the Security Center (§24). Never fabricated:
  // zero until a real download is declined.
  int declined_download_count() const { return declined_downloads_.load(); }

 private:
  mutable std::atomic<int> declined_downloads_{0};
  Decider decider_;
};

// Process-wide production instance: decider bound to the ad-block
// engine holder. Lazily created on first use and never destroyed.
InwebDownloadGuard* GetDownloadGuard();

}  // namespace inweb::popup

#endif  // CHROME_ANDROID_INWEB_POPUP_INWEB_DOWNLOAD_GUARD_H_
