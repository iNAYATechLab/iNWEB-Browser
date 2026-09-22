// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// iNWEB Browser — URLLoaderThrottle unit tests. Exercises the defer →
// worker-pool decide → cancel/resume flow with a scriptable decider (the
// engine itself is covered by the engine unittests; the throttle only
// transports decisions).

#include "chrome/android/inweb/adblock/inweb_adblock_url_loader_throttle.h"

#include <optional>
#include <string>
#include <vector>

#include "base/functional/bind.h"
#include "base/test/task_environment.h"
#include "chrome/android/inweb/adblock/inweb_request_destination_map.h"
#include "net/base/net_errors.h"
#include "net/base/redirect_info.h"
#include "services/network/public/cpp/resource_request.h"
#include "testing/gtest/include/gtest/gtest.h"
#include "third_party/blink/public/common/loader/url_loader_throttle.h"
#include "url/gurl.h"
#include "url/origin.h"

namespace inweb::adblock {

namespace {

class RecordingDelegate : public blink::URLLoaderThrottle::Delegate {
 public:
  void CancelWithError(int error_code, std::string_view) override {
    canceled_error_ = error_code;
  }
  void Resume() override { resumed_ = true; }

  std::optional<int> canceled_error_;
  bool resumed_ = false;
};

FilterDecision Decision(FilterAction action) {
  FilterDecision decision;
  decision.action = action;
  return decision;
}

}  // namespace

class InwebAdblockURLLoaderThrottleTest : public ::testing::Test {
 protected:
  base::test::TaskEnvironment task_environment_{
      base::test::TaskEnvironment::ThreadPoolPointerSupport::kNoPointerSupport};

  network::ResourceRequest ImageRequestFromNewsDoc() {
    network::ResourceRequest request;
    request.url = GURL("https://ads.example.com/pixel.gif");
    request.destination = network::mojom::RequestDestination::kImage;
    request.request_initiator =
        url::Origin::Create(GURL("https://news.example.com"));
    return request;
  }
};

TEST_F(InwebAdblockURLLoaderThrottleTest, DefersThenBlocksWhenEngineBlocks) {
  std::vector<RequestContext> seen;
  InwebAdblockURLLoaderThrottle throttle(base::BindRepeating(
      [](std::vector<RequestContext>* seen, const RequestContext& ctx) {
        seen->push_back(ctx);
        return Decision(FilterAction::kBlock);
      },
      &seen));
  RecordingDelegate delegate;
  throttle.set_delegate(&delegate);

  network::ResourceRequest request = ImageRequestFromNewsDoc();
  bool defer = false;
  throttle.WillStartRequest(&request, &defer);
  EXPECT_TRUE(defer);
  EXPECT_FALSE(delegate.resumed_);
  EXPECT_FALSE(delegate.canceled_error_.has_value());

  task_environment_.RunUntilIdle();

  ASSERT_EQ(1u, seen.size());
  EXPECT_EQ("https://ads.example.com/pixel.gif",
            seen[0].request_url.possibly_invalid_spec());
  EXPECT_EQ("news.example.com", seen[0].document_url.host());
  EXPECT_EQ(ResourceType::kImage, seen[0].resource_type);
  ASSERT_TRUE(delegate.canceled_error_.has_value());
  EXPECT_EQ(net::ERR_BLOCKED_BY_CLIENT, *delegate.canceled_error_);
  EXPECT_FALSE(delegate.resumed_);
}

TEST_F(InwebAdblockURLLoaderThrottleTest, ResumesOnPassAndOnAllow) {
  for (FilterAction action : {FilterAction::kPass, FilterAction::kAllow}) {
    InwebAdblockURLLoaderThrottle throttle(base::BindRepeating(
        [](FilterAction action, const RequestContext&) {
          return Decision(action);
        },
        action));
    RecordingDelegate delegate;
    throttle.set_delegate(&delegate);

    network::ResourceRequest request = ImageRequestFromNewsDoc();
    bool defer = false;
    throttle.WillStartRequest(&request, &defer);
    EXPECT_TRUE(defer);
    task_environment_.RunUntilIdle();
    EXPECT_TRUE(delegate.resumed_);
    EXPECT_FALSE(delegate.canceled_error_.has_value());
  }
}

TEST_F(InwebAdblockURLLoaderThrottleTest, MapsDestinationAndInitiator) {
  std::vector<RequestContext> seen;
  InwebAdblockURLLoaderThrottle throttle(base::BindRepeating(
      [](std::vector<RequestContext>* seen, const RequestContext& ctx) {
        seen->push_back(ctx);
        return Decision(FilterAction::kPass);
      },
      &seen));
  RecordingDelegate delegate;
  throttle.set_delegate(&delegate);

  network::ResourceRequest request;
  request.url = GURL("https://cdn.example.com/app.js");
  request.destination = network::mojom::RequestDestination::kScript;
  request.request_initiator =
      url::Origin::Create(GURL("https://news.example.com"));
  bool defer = false;
  throttle.WillStartRequest(&request, &defer);
  EXPECT_TRUE(defer);
  task_environment_.RunUntilIdle();

  ASSERT_EQ(1u, seen.size());
  EXPECT_EQ(ResourceType::kScript, seen[0].resource_type);

  // A request without an initiator gets an empty document URL.
  network::ResourceRequest no_initiator;
  no_initiator.url = GURL("https://tracker.example.net/x");
  no_initiator.destination = network::mojom::RequestDestination::kIframe;
  throttle.WillStartRequest(&no_initiator, &defer);
  EXPECT_TRUE(defer);
  task_environment_.RunUntilIdle();
  ASSERT_EQ(2u, seen.size());
  EXPECT_EQ(ResourceType::kSubdocument, seen[1].resource_type);
  EXPECT_TRUE(seen[1].document_url.is_empty());
}

TEST_F(InwebAdblockURLLoaderThrottleTest, RedirectReevaluatesWithOriginalDoc) {
  std::vector<RequestContext> seen;
  // First decision passes; the redirect target is blocked.
  InwebAdblockURLLoaderThrottle throttle(base::BindRepeating(
      [](std::vector<RequestContext>* seen, const RequestContext& ctx) {
        seen->push_back(ctx);
        return Decision(seen->size() == 1 ? FilterAction::kPass
                                          : FilterAction::kBlock);
      },
      &seen));
  RecordingDelegate delegate;
  throttle.set_delegate(&delegate);

  network::ResourceRequest request = ImageRequestFromNewsDoc();
  bool defer = false;
  throttle.WillStartRequest(&request, &defer);
  EXPECT_TRUE(defer);
  task_environment_.RunUntilIdle();
  EXPECT_TRUE(delegate.resumed_);

  net::RedirectInfo redirect_info;
  redirect_info.new_url = GURL("https://evil-tracker.net/pixel.gif");
  defer = false;
  throttle.WillRedirectRequest(&redirect_info,
                               network::mojom::URLResponseHead(), &defer,
                               nullptr);
  EXPECT_TRUE(defer);
  task_environment_.RunUntilIdle();

  ASSERT_EQ(2u, seen.size());
  EXPECT_EQ("https://evil-tracker.net/pixel.gif",
            seen[1].request_url.possibly_invalid_spec());
  // Original document governs party classification on redirects.
  EXPECT_EQ("news.example.com", seen[1].document_url.host());
  EXPECT_EQ(ResourceType::kImage, seen[1].resource_type);
  ASSERT_TRUE(delegate.canceled_error_.has_value());
  EXPECT_EQ(net::ERR_BLOCKED_BY_CLIENT, *delegate.canceled_error_);
}

}  // namespace inweb::adblock
