// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#include "chrome/android/inweb/adblock/inweb_cosmetic_injector.h"

#include <utility>

#include "base/functional/bind.h"
#include "base/json/json_writer.h"
#include "base/strings/utf_string_conversions.h"
#include "base/task/task_traits.h"
#include "base/task/thread_pool.h"
#include "base/values.h"
#include "chrome/android/inweb/adblock/inweb_adblock_engine_holder.h"
#include "content/public/browser/navigation_handle.h"
#include "content/public/browser/render_frame_host.h"
#include "content/public/common/isolated_world_ids.h"

namespace inweb::adblock {

namespace {

// The tiny script executed in our isolated world. It only touches the
// shared DOM (appending a <style> element); it never interacts with
// the page's JavaScript context. |json_css| is a pre-escaped,
// JSON-quoted string literal.
std::u16string BuildInjectionScript(const std::string& json_css) {
  std::string script =
      "(function(){var s=document.createElement('style');"
      "s.setAttribute('data-inweb','cosmetic');"
      "s.textContent=" + json_css + ";"
      "(document.head||document.documentElement).appendChild(s);})();";
  return base::UTF8ToUTF16(script);
}

}  // namespace

InwebCosmeticInjector::~InwebCosmeticInjector() = default;

InwebCosmeticInjector::InwebCosmeticInjector(content::WebContents* web_contents)
    : content::WebContentsObserver(web_contents) {}

void InwebCosmeticInjector::DidFinishNavigation(
    content::NavigationHandle* handle) {
  // Pure stylesheet hiding is set-and-forget (PHASE4-COSMETIC §2):
  // inject once per committed document, main frame and same-process
  // iframes alike. Same-document navigations keep the old document;
  // error pages get no cosmetics.
  if (!handle->HasCommitted() || handle->IsSameDocument() ||
      handle->IsErrorPage()) {
    return;
  }
  content::RenderFrameHost* rfh = handle->GetRenderFrameHost();
  if (!rfh) {
    return;
  }
  const std::string host = rfh->GetLastCommittedOrigin().host();
  if (host.empty()) {
    return;
  }
  const content::GlobalRenderFrameHostId frame_id = rfh->GetGlobalId();

  // Compute the CSS on a worker thread (never the UI thread; the
  // holder lock is held there for the v1 full-scan decision). The
  // reply is bound to this injector — if the WebContents dies while we
  // are computing, the callback is a no-op. Policy (global toggle +
  // per-site allowlist) is honored inside CosmeticCssFor, before the
  // engine is consulted — one decision path with the network engine.
  base::ThreadPool::PostTaskAndReplyWithResult(
      FROM_HERE,
      {base::TaskPriority::USER_VISIBLE, base::TaskShutdownBehavior::SKIP_ON_SHUTDOWN},
      base::BindOnce([](const std::string& host) {
        return InwebAdblockEngineHolder::GetInstance()->CosmeticCssFor(host);
      },
                     host),
      base::BindOnce(&InwebCosmeticInjector::InjectCss,
                     weak_factory_.GetWeakPtr(), frame_id));
}

void InwebCosmeticInjector::InjectCss(
    content::GlobalRenderFrameHostId frame_id,
    const std::string& css) {
  if (css.empty()) {
    return;  // nothing to hide on this host (or filtering off)
  }
  // Re-resolve the frame: it may have been swapped or destroyed while
  // the CSS was computed. Losing the race loses the injection for
  // that document — acceptable for the secondary defense, documented.
  content::RenderFrameHost* rfh =
      content::RenderFrameHost::FromID(frame_id);
  if (!rfh) {
    return;
  }
  // JSON-Quote the CSS so it travels as one string literal (escaping
  // quotes/backslashes/newlines inside selectors).
  std::string json_css;
  base::JSONWriter::Write(base::Value(css), &json_css);

  // Embedder isolated world (the first one past content's reserved
  // range); no ExecuteJavaScript on web content at 154 — see the
  // header's pinned-tree honesty note.
  rfh->ExecuteJavaScriptInIsolatedWorld(
      BuildInjectionScript(json_css),
      content::RenderFrameHost::JavaScriptResultCallback(),
      content::ISOLATED_WORLD_ID_CONTENT_END);
}

WEB_CONTENTS_USER_DATA_KEY_IMPL(InwebCosmeticInjector);

}  // namespace inweb::adblock
