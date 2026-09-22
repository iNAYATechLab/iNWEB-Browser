// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// iNWEB Browser — redirect guard unit tests (PHASE4-POPUP §2B policy),
// scriptable decider + allowlist checker; plus the production-decider
// path through the real engine holder with an @@ exception rule.

#include "chrome/android/inweb/popup/inweb_redirect_guard.h"

#include <string>
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
  bool allowlisted = false;
};

InwebRedirectGuard GuardFrom(ScriptedState* state) {
  return InwebRedirectGuard(
      base::BindRepeating(
          [](ScriptedState* state, const RequestContext& context) {
            state->seen.push_back(context);
            FilterDecision decision;
            decision.action = state->action;
            return decision;
          },
          state),
      base::BindRepeating(
          [](ScriptedState* state, const GURL&) {
            return state->allowlisted;
          },
          state));
}

// The canonical tab-takeover attempt: hidden ad subframe on another
// origin steers the top-level page, no activation anywhere.
NavigationFacts TakeoverFacts() {
  NavigationFacts facts;
  facts.in_main_frame = true;
  facts.renderer_initiated = true;
  facts.has_user_gesture = false;
  facts.started_with_transient_activation = false;
  facts.started_by_ad = false;
  facts.has_initiator = true;
  facts.cross_origin_initiator = true;
  facts.target_url = GURL("https://tracker-redirect.example.net/landing");
  facts.current_page_url = GURL("https://news.example.com/story");
  return facts;
}

}  // namespace

TEST(InwebRedirectGuardTest, SubframeNavigationIsNotOurs) {
  ScriptedState state;
  auto guard = GuardFrom(&state);
  NavigationFacts facts = TakeoverFacts();
  facts.in_main_frame = false;  // subframe target: engine rules apply.
  EXPECT_EQ(guard.ShouldAllowNavigation(facts), RedirectDecision::kProceed);
  EXPECT_EQ(guard.blocked_redirect_count(), 0);
  EXPECT_TRUE(state.seen.empty());  // engine never consulted
}

TEST(InwebRedirectGuardTest, BrowserInitiatedNavigationProceeds) {
  ScriptedState state;
  auto guard = GuardFrom(&state);
  NavigationFacts facts = TakeoverFacts();
  facts.renderer_initiated = false;  // omnibox / bookmark / UI button.
  EXPECT_EQ(guard.ShouldAllowNavigation(facts), RedirectDecision::kProceed);
  EXPECT_TRUE(state.seen.empty());
}

TEST(InwebRedirectGuardTest, SameDocumentAndPageActivationsProceed) {
  ScriptedState state;
  auto guard = GuardFrom(&state);
  NavigationFacts same_document = TakeoverFacts();
  same_document.is_same_document = true;
  NavigationFacts activation = TakeoverFacts();
  activation.is_page_activation = true;
  EXPECT_EQ(guard.ShouldAllowNavigation(same_document),
            RedirectDecision::kProceed);
  EXPECT_EQ(guard.ShouldAllowNavigation(activation),
            RedirectDecision::kProceed);
  EXPECT_TRUE(state.seen.empty());
}

TEST(InwebRedirectGuardTest, UserActivationIsNeverTouched) {
  ScriptedState state;
  auto guard = GuardFrom(&state);
  NavigationFacts gesture = TakeoverFacts();
  gesture.has_user_gesture = true;
  NavigationFacts transient = TakeoverFacts();
  transient.started_with_transient_activation = true;
  EXPECT_EQ(guard.ShouldAllowNavigation(gesture), RedirectDecision::kProceed);
  EXPECT_EQ(guard.ShouldAllowNavigation(transient),
            RedirectDecision::kProceed);
  EXPECT_TRUE(state.seen.empty());
}

TEST(InwebRedirectGuardTest, GesturelessCrossOriginInitiatorIsBlocked) {
  ScriptedState state;  // engine kPass: frame-context rule decides alone.
  auto guard = GuardFrom(&state);
  EXPECT_EQ(guard.ShouldAllowNavigation(TakeoverFacts()),
            RedirectDecision::kBlockedTabTakeover);
  EXPECT_EQ(guard.blocked_redirect_count(), 1);
  ASSERT_EQ(state.seen.size(), 1u);
  EXPECT_EQ(state.seen[0].resource_type, ResourceType::kDocument);
  EXPECT_EQ(state.seen[0].request_url.possibly_invalid_spec(),
            "https://tracker-redirect.example.net/landing");
}

TEST(InwebRedirectGuardTest, SameOriginGesturelessRedirectProceeds) {
  ScriptedState state;
  auto guard = GuardFrom(&state);
  NavigationFacts facts = TakeoverFacts();
  facts.cross_origin_initiator = false;  // same-site script redirect.
  EXPECT_EQ(guard.ShouldAllowNavigation(facts), RedirectDecision::kProceed);
  EXPECT_TRUE(state.seen.empty());  // not a takeover: engine not asked.
}

TEST(InwebRedirectGuardTest, UnattributedNavigationProceeds) {
  ScriptedState state;
  auto guard = GuardFrom(&state);
  NavigationFacts facts = TakeoverFacts();
  facts.has_initiator = false;  // no initiator origin: cannot attribute.
  EXPECT_EQ(guard.ShouldAllowNavigation(facts), RedirectDecision::kProceed);
  EXPECT_TRUE(state.seen.empty());
}

TEST(InwebRedirectGuardTest, StartedByAdBlocksEvenSameOrigin) {
  ScriptedState state;
  auto guard = GuardFrom(&state);
  NavigationFacts facts = TakeoverFacts();
  facts.cross_origin_initiator = false;
  facts.started_by_ad = true;  // Chromium ad-frame tagging.
  EXPECT_EQ(guard.ShouldAllowNavigation(facts),
            RedirectDecision::kBlockedTabTakeover);
  EXPECT_EQ(guard.blocked_redirect_count(), 1);
}

TEST(InwebRedirectGuardTest, AllowlistedSiteIsExempt) {
  ScriptedState state;
  state.allowlisted = true;
  auto guard = GuardFrom(&state);
  EXPECT_EQ(guard.ShouldAllowNavigation(TakeoverFacts()),
            RedirectDecision::kProceed);
  EXPECT_EQ(guard.blocked_redirect_count(), 0);
}

TEST(InwebRedirectGuardTest, EngineExceptionUnblocks) {
  ScriptedState state;
  state.action = FilterAction::kAllow;  // @@ rule matched the target.
  auto guard = GuardFrom(&state);
  EXPECT_EQ(guard.ShouldAllowNavigation(TakeoverFacts()),
            RedirectDecision::kProceed);
  ASSERT_EQ(state.seen.size(), 1u);
  EXPECT_EQ(guard.blocked_redirect_count(), 0);
}

TEST(InwebRedirectGuardTest, EngineBlockIsLeftToUrlLoaderThrottle) {
  ScriptedState state;
  state.action = FilterAction::kBlock;  // 0006 owns host-based blocking.
  auto guard = GuardFrom(&state);
  EXPECT_EQ(guard.ShouldAllowNavigation(TakeoverFacts()),
            RedirectDecision::kProceed);
  EXPECT_EQ(guard.blocked_redirect_count(), 0);
}

TEST(InwebRedirectGuardTest, CounterCountsOnlyRealBlocks) {
  ScriptedState state;
  auto guard = GuardFrom(&state);
  NavigationFacts benign = TakeoverFacts();
  benign.cross_origin_initiator = false;
  EXPECT_EQ(guard.ShouldAllowNavigation(benign), RedirectDecision::kProceed);
  EXPECT_EQ(guard.ShouldAllowNavigation(TakeoverFacts()),
            RedirectDecision::kBlockedTabTakeover);
  EXPECT_EQ(guard.ShouldAllowNavigation(TakeoverFacts()),
            RedirectDecision::kBlockedTabTakeover);
  EXPECT_EQ(guard.blocked_redirect_count(), 2);
}

TEST(InwebRedirectGuardTest, ProductionDeciderExceptionThroughHolder) {
  // A real holder with an @@ exception for the redirect target: the
  // production decider path must honor it (identity-provider flows).
  adblock::InwebAdblockEngineHolder holder;
  adblock::TrackingProtectionSettings settings(
      /*enabled=*/true, adblock::CookiePolicy::kAllowAll,
      /*allowlisted_sites=*/{});
  std::vector<adblock::ParsedFilterList> lists;
  lists.push_back(adblock::FilterListParser::Parse(
      "@@||idp-redirect.example.net^$document", "redirect-test"));
  holder.SetFilterLists(std::move(lists));
  holder.UpdateSettings(settings);

  InwebRedirectGuard exception_guard(
      base::BindRepeating(
          [](adblock::InwebAdblockEngineHolder* holder,
             const RequestContext& context) { return holder->Decide(context); },
          &holder),
      base::BindRepeating([](const GURL&) { return false; }));
  NavigationFacts facts = TakeoverFacts();
  facts.target_url = GURL("https://idp-redirect.example.net/sso-return");
  EXPECT_EQ(exception_guard.ShouldAllowNavigation(facts),
            RedirectDecision::kProceed);

  // Sanity on the same holder: a target with no matching rule gives
  // kPass, so the frame-context rule blocks it — proves the @@ result
  // above is the rule's doing, not a stale default.
  NavigationFacts unmatched = TakeoverFacts();
  unmatched.target_url = GURL("https://plain-redirect.example.net/x");
  EXPECT_EQ(exception_guard.ShouldAllowNavigation(unmatched),
            RedirectDecision::kBlockedTabTakeover);
}

}  // namespace inweb::popup
