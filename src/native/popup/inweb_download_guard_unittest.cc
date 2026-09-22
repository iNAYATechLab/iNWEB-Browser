// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// iNWEB Browser — download guard unit tests (PHASE4-POPUP §2D policy):
// engine BLOCK on the download URL declines the download; exceptions,
// allowlist and tracking-protection-off all allow; the real §24
// counter counts only real declines. Includes the production-decider
// path through the real engine holder with an object rule.

#include "chrome/android/inweb/popup/inweb_download_guard.h"

#include <vector>

#include "base/functional/bind.h"
#include "chrome/android/inweb/adblock/inweb_adblock_engine_holder.h"
#include "chrome/android/inweb/adblock/inweb_filter_list_parser.h"
#include "testing/gtest/include/gtest/gtest.h"

namespace inweb::popup {

namespace {

using adblock::FilterAction;
using adblock::FilterDecision;
using adblock::RequestContext;
using adblock::ResourceType;

struct ScriptedState {
  FilterAction action = FilterAction::kPass;
  std::vector<RequestContext> seen;
};

InwebDownloadGuard GuardFrom(ScriptedState* state) {
  return InwebDownloadGuard(base::BindRepeating(
      [](ScriptedState* state, const RequestContext& context) {
        state->seen.push_back(context);
        FilterDecision decision;
        decision.action = state->action;
        return decision;
      },
      state));
}

const GURL kDownloadUrl = GURL("https://cdn.tracker.example.net/payload.apk");
const GURL kDocumentUrl = GURL("https://sketchy.example.com/free-stuff");

}  // namespace

TEST(InwebDownloadGuardTest, EngineBlockDeclinesTheDownload) {
  ScriptedState state;
  state.action = FilterAction::kBlock;
  auto guard = GuardFrom(&state);
  EXPECT_EQ(guard.ShouldDeclineDownload(kDownloadUrl, kDocumentUrl),
            DownloadDecision::kDeclinedByEngine);
  EXPECT_EQ(guard.declined_download_count(), 1);
  ASSERT_EQ(state.seen.size(), 1u);
  EXPECT_EQ(state.seen[0].resource_type, ResourceType::kObject);
  EXPECT_EQ(state.seen[0].request_url, kDownloadUrl);
  EXPECT_EQ(state.seen[0].document_url, kDocumentUrl);
}

TEST(InwebDownloadGuardTest, EnginePassAllowsTheDownload) {
  ScriptedState state;  // kPass: tracking protection off or no rule.
  auto guard = GuardFrom(&state);
  EXPECT_EQ(guard.ShouldDeclineDownload(kDownloadUrl, kDocumentUrl),
            DownloadDecision::kAllow);
  EXPECT_EQ(guard.declined_download_count(), 0);
}

TEST(InwebDownloadGuardTest, EngineExceptionAllowsTheDownload) {
  ScriptedState state;
  state.action = FilterAction::kAllow;  // @@ exception matched.
  auto guard = GuardFrom(&state);
  EXPECT_EQ(guard.ShouldDeclineDownload(kDownloadUrl, kDocumentUrl),
            DownloadDecision::kAllow);
  EXPECT_EQ(guard.declined_download_count(), 0);
}

TEST(InwebDownloadGuardTest, EmptyDocumentUrlStillChecksTheHost) {
  // Downloads without an initiating document still get the host check;
  // the engine treats an empty document as third-party context.
  ScriptedState state;
  state.action = FilterAction::kBlock;
  auto guard = GuardFrom(&state);
  EXPECT_EQ(guard.ShouldDeclineDownload(kDownloadUrl, GURL()),
            DownloadDecision::kDeclinedByEngine);
  ASSERT_EQ(state.seen.size(), 1u);
  EXPECT_TRUE(state.seen[0].document_url.is_empty());
}

TEST(InwebDownloadGuardTest, CounterCountsOnlyRealDeclines) {
  ScriptedState state;
  auto guard = GuardFrom(&state);
  EXPECT_EQ(guard.ShouldDeclineDownload(kDownloadUrl, kDocumentUrl),
            DownloadDecision::kAllow);
  state.action = FilterAction::kBlock;
  EXPECT_EQ(guard.ShouldDeclineDownload(kDownloadUrl, kDocumentUrl),
            DownloadDecision::kDeclinedByEngine);
  EXPECT_EQ(guard.ShouldDeclineDownload(kDownloadUrl, kDocumentUrl),
            DownloadDecision::kDeclinedByEngine);
  EXPECT_EQ(guard.declined_download_count(), 2);
}

TEST(InwebDownloadGuardTest, ProductionDeciderObjectRuleThroughHolder) {
  // A real holder with an object rule for the download host: the
  // production decider path must decline (§2D BLOCK on OBJECT).
  adblock::InwebAdblockEngineHolder holder;
  adblock::TrackingProtectionSettings settings(
      /*enabled=*/true, adblock::CookiePolicy::kAllowAll,
      /*allowlisted_sites=*/{});
  std::vector<adblock::ParsedFilterList> lists;
  lists.push_back(adblock::FilterListParser::Parse(
      "||tracker.example.net^$object", "download-test"));
  holder.SetFilterLists(std::move(lists));
  holder.UpdateSettings(settings);

  InwebDownloadGuard guard(base::BindRepeating(
      [](adblock::InwebAdblockEngineHolder* holder,
         const RequestContext& context) { return holder->Decide(context); },
      &holder));
  EXPECT_EQ(guard.ShouldDeclineDownload(kDownloadUrl, kDocumentUrl),
            DownloadDecision::kDeclinedByEngine);

  // Same engine, an @@ exception for the exact download URL wins.
  std::vector<adblock::ParsedFilterList> exception_lists;
  exception_lists.push_back(adblock::FilterListParser::Parse(
      "@@||cdn.tracker.example.net/payload.apk^$object", "download-ex"));
  holder.SetFilterLists(std::move(exception_lists));
  EXPECT_EQ(guard.ShouldDeclineDownload(kDownloadUrl, kDocumentUrl),
            DownloadDecision::kAllow);
}

}  // namespace inweb::popup
