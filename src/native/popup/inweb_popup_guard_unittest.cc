// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// iNWEB Browser — popup guard unit tests (PHASE4-POPUP §2A policy),
// scriptable decider + allowlist checker; plus the production-decider
// path through the real engine holder with a $popup rule.

#include "chrome/android/inweb/popup/inweb_popup_guard.h"

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

adblock::RequestContext Request(const std::string& url) {
  return RequestContext{GURL(url), GURL("https://news.example.com/story"),
                        ResourceType::kPopup};
}

FilterDecision Decision(FilterAction action) {
  FilterDecision decision;
  decision.action = action;
  return decision;
}

struct ScriptedState {
  FilterAction action = FilterAction::kPass;
  std::vector<RequestContext> seen;
  bool allowlisted = false;
};

InwebPopupGuard GuardFrom(ScriptedState* state) {
  return InwebPopupGuard(
      base::BindRepeating(
          [](ScriptedState* state, const RequestContext& context) {
            state->seen.push_back(context);
            return Decision(state->action);
          },
          state),
      base::BindRepeating(
          [](ScriptedState* state, const GURL&) {
            return state->allowlisted;
          },
          state));
}

}  // namespace

TEST(InwebPopupGuardTest, GesturelessPopupIsBlockedByDefault) {
  ScriptedState state;  // kPass, not allowlisted
  InwebPopupGuard guard = GuardFrom(&state);
  EXPECT_EQ(PopupDecision::kBlockedNoGesture,
            guard.ShouldAllowPopup(GURL("https://ads.example.net/win"),
                                   GURL("https://sketchy.example.com/page"),
                                   /*user_gesture=*/false));
  EXPECT_EQ(1, guard.blocked_popup_count());
  EXPECT_EQ(0, guard.engine_blocked_count());
  // The engine was consulted with the popup resource type.
  ASSERT_EQ(1u, state.seen.size());
  EXPECT_EQ(ResourceType::kPopup, state.seen[0].resource_type);
}

TEST(InwebPopupGuardTest, GestureAllowsWhenEngineIsSilent) {
  ScriptedState state;
  InwebPopupGuard guard = GuardFrom(&state);
  EXPECT_EQ(PopupDecision::kAllow,
            guard.ShouldAllowPopup(GURL("https://docs.example.org/open"),
                                   GURL("https://docs.example.org/"),
                                   /*user_gesture=*/true));
  EXPECT_EQ(0, guard.blocked_popup_count());
}

TEST(InwebPopupGuardTest, AllowlistExemptsGesturelessPopup) {
  ScriptedState state;
  state.allowlisted = true;
  InwebPopupGuard guard = GuardFrom(&state);
  EXPECT_EQ(PopupDecision::kAllow,
            guard.ShouldAllowPopup(GURL("https://anything.example/"),
                                   GURL("https://trusted.example.com/"),
                                   /*user_gesture=*/false));
  EXPECT_EQ(0, guard.blocked_popup_count());
}

TEST(InwebPopupGuardTest, EngineExceptionAllowsGesturelessPopup) {
  ScriptedState state;
  state.action = FilterAction::kAllow;  // @@ exception path
  InwebPopupGuard guard = GuardFrom(&state);
  EXPECT_EQ(PopupDecision::kAllow,
            guard.ShouldAllowPopup(GURL("https://good.example.com/"),
                                   GURL("https://news.example.com/"),
                                   /*user_gesture=*/false));
  EXPECT_EQ(0, guard.blocked_popup_count());
}

TEST(InwebPopupGuardTest, EngineBlocksEvenWithGesture) {
  ScriptedState state;
  state.action = FilterAction::kBlock;  // $popup rule matched
  InwebPopupGuard guard = GuardFrom(&state);
  EXPECT_EQ(PopupDecision::kBlockedByEngine,
            guard.ShouldAllowPopup(GURL("https://popup-tracker.invalid/"),
                                   GURL("https://news.example.com/"),
                                   /*user_gesture=*/true));
  EXPECT_EQ(1, guard.blocked_popup_count());
  EXPECT_EQ(1, guard.engine_blocked_count());

  // Allowlist does not override an explicit engine BLOCK (the engine
  // checks the allowlist before rules; kBlock here means the site is not
  // allowlisted).
  EXPECT_EQ(PopupDecision::kBlockedByEngine,
            guard.ShouldAllowPopup(GURL("https://popup-tracker.invalid/"),
                                   GURL("https://news.example.com/"),
                                   /*user_gesture=*/false));
  EXPECT_EQ(2, guard.blocked_popup_count());
}

TEST(InwebPopupGuardTest, ProductionDeciderBlocksPopupRuleWithGesture) {
  // Real engine via the holder: a $popup rule on a fixture host blocks
  // the window even with a gesture — the engine value-add.
  adblock::InwebAdblockEngineHolder holder;
  std::vector<adblock::ParsedFilterList> lists;
  lists.push_back(adblock::FilterListParser::Parse(
      "||popup-fixture.invalid^$popup\n", "popup-test"));
  holder.SetFilterLists(std::move(lists));

  InwebPopupGuard guard(
      base::BindRepeating(
          [](adblock::InwebAdblockEngineHolder* holder,
             const RequestContext& context) { return holder->Decide(context); },
          &holder),
      base::BindRepeating([](const GURL&) { return false; }));

  EXPECT_EQ(PopupDecision::kBlockedByEngine,
            guard.ShouldAllowPopup(GURL("https://popup-fixture.invalid/ad"),
                                   GURL("https://news.example.com/"),
                                   /*user_gesture=*/true));
  EXPECT_EQ(PopupDecision::kBlockedByEngine,
            guard.ShouldAllowPopup(GURL("https://popup-fixture.invalid/ad"),
                                   GURL("https://news.example.com/"),
                                   /*user_gesture=*/false));
  // A non-matching popup URL still opens with a gesture.
  EXPECT_EQ(PopupDecision::kAllow,
            guard.ShouldAllowPopup(GURL("https://ordinary.example.org/"),
                                   GURL("https://news.example.com/"),
                                   /*user_gesture=*/true));
  EXPECT_EQ(2, guard.blocked_popup_count());
}

}  // namespace inweb::popup
