// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#ifndef CHROME_ANDROID_INWEB_ADBLOCK_INWEB_COSMETIC_INJECTOR_H_
#define CHROME_ANDROID_INWEB_ADBLOCK_INWEB_COSMETIC_INJECTOR_H_

#include "base/memory/weak_ptr.h"
#include "content/public/browser/global_routing_id.h"
#include "content/public/browser/web_contents.h"
#include "content/public/browser/web_contents_observer.h"
#include "content/public/browser/web_contents_user_data.h"

namespace inweb::adblock {

// Per-frame cosmetic-filter stylesheet injection (PHASE4-COSMETIC §2).
//
// One instance per WebContents (attached from
// ChromeContentBrowserClient::OnWebContentsCreated). On every committed
// non-same-document, non-error navigation in ANY frame (main frame or
// same-process iframe), the injector asks the engine holder for the
// grouped hiding CSS for that frame's host — the computation runs on a
// worker thread (never the UI thread; PHASE4-COSMETIC §2) — and inserts
// it back on the UI thread by executing a tiny script in our own
// isolated world that appends a <style> element to the document.
//
// Pinned-tree honesty (PHASE4-COSMETIC said "content-layer CSS-insertion
// API"): at 154 there is no public CSS-insertion API for web content —
// RenderFrameHost::ExecuteJavaScript is WebUI-only. The closest real
// path is ExecuteJavaScriptInIsolatedWorld with the first embedder
// world (content::ISOLATED_WORLD_ID_CONTENT_END), whose DOM
// modification (appending a <style> element) styles the shared
// document. Known limitations, documented not hidden: a brief flash of
// unhidden elements before injection; strict-CSP pages may defeat
// inline <style> insertion (cosmetic filtering is the secondary
// defense — network blocking is primary); races with frame death lose
// the injection for that frame (RenderFrameHost::FromID returns null —
// skipped silently).
class InwebCosmeticInjector
    : public content::WebContentsObserver,
      public content::WebContentsUserData<InwebCosmeticInjector> {
 public:
  ~InwebCosmeticInjector() override;

  InwebCosmeticInjector(const InwebCosmeticInjector&) = delete;
  InwebCosmeticInjector& operator=(const InwebCosmeticInjector&) = delete;

  // content::WebContentsUserData<T> reads T::kUserDataKey; at @154 the
  // macro expands to `static const int kUserDataKey = 0`, and the
  // out-of-line definition comes from WEB_CONTENTS_USER_DATA_KEY_IMPL in
  // the .cc.
  WEB_CONTENTS_USER_DATA_KEY_DECL();

  // content::WebContentsObserver:
  void DidFinishNavigation(content::NavigationHandle* handle) override;

 private:
  friend class content::WebContentsUserData<InwebCosmeticInjector>;
  explicit InwebCosmeticInjector(content::WebContents* web_contents);

  // Runs on the UI thread after the worker-thread CSS computation.
  // Bound through weak_factory_ — a no-op if the WebContents (and so
  // this injector) was destroyed meanwhile.
  void InjectCss(content::GlobalRenderFrameHostId frame_id,
                 const std::string& css);

  base::WeakPtrFactory<InwebCosmeticInjector> weak_factory_{this};
};

}  // namespace inweb::adblock

#endif  // CHROME_ANDROID_INWEB_ADBLOCK_INWEB_COSMETIC_INJECTOR_H_
