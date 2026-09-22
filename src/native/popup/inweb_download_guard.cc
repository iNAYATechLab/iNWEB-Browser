// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#include "chrome/android/inweb/popup/inweb_download_guard.h"

#include <utility>

#include "base/functional/bind.h"
#include "base/no_destructor.h"
#include "chrome/android/inweb/adblock/inweb_adblock_engine_holder.h"

namespace inweb::popup {

InwebDownloadGuard::InwebDownloadGuard(Decider decider)
    : decider_(std::move(decider)) {}

InwebDownloadGuard::~InwebDownloadGuard() = default;

DownloadDecision InwebDownloadGuard::ShouldDeclineDownload(
    const GURL& download_url,
    const GURL& document_url) const {
  if (!decider_) {
    return DownloadDecision::kAllow;
  }
  // §2D: engine BLOCK decisions on OBJECT/OTHER downloads are
  // declined. The engine's own decision order (disabled → PASS,
  // allowlisted → ALLOW, first @@ → ALLOW, first block → BLOCK) makes
  // this safe: exceptions and the site allowlist win, tracking
  // protection off means kPass, and only a real matching rule blocks.
  const adblock::RequestContext context{download_url, document_url,
                                        adblock::ResourceType::kObject};
  if (decider_.Run(context).action == adblock::FilterAction::kBlock) {
    declined_downloads_.fetch_add(1, std::memory_order_relaxed);
    return DownloadDecision::kDeclinedByEngine;
  }
  return DownloadDecision::kAllow;
}

InwebDownloadGuard* GetDownloadGuard() {
  static base::NoDestructor<InwebDownloadGuard> guard(
      base::BindRepeating([](const adblock::RequestContext& context) {
        return adblock::InwebAdblockEngineHolder::GetInstance()->Decide(
            context);
      }));
  return guard.get();
}

}  // namespace inweb::popup
