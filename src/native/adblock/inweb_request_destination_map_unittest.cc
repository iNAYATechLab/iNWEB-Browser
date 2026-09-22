// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// iNWEB Browser — RequestDestination → ResourceType mapping unit tests.
// Ported from the Kotlin reference semantics; the mapping table itself is
// new (Kotlin lived above the network stack).

#include "chrome/android/inweb/adblock/inweb_request_destination_map.h"

#include "testing/gtest/include/gtest/gtest.h"

namespace inweb::adblock {

namespace {
using Dest = network::mojom::RequestDestination;
}  // namespace

TEST(InwebRequestDestinationMapTest, ScriptsAndWorkersMapToScript) {
  EXPECT_EQ(ResourceType::kScript, ResourceTypeFromRequestDestination(Dest::kScript));
  EXPECT_EQ(ResourceType::kScript, ResourceTypeFromRequestDestination(Dest::kWorker));
  EXPECT_EQ(ResourceType::kScript,
            ResourceTypeFromRequestDestination(Dest::kSharedWorker));
  EXPECT_EQ(ResourceType::kScript,
            ResourceTypeFromRequestDestination(Dest::kServiceWorker));
  EXPECT_EQ(ResourceType::kScript,
            ResourceTypeFromRequestDestination(Dest::kAudioWorklet));
  EXPECT_EQ(ResourceType::kScript,
            ResourceTypeFromRequestDestination(Dest::kPaintWorklet));
}

TEST(InwebRequestDestinationMapTest, MediaTypesMapDirectly) {
  EXPECT_EQ(ResourceType::kImage, ResourceTypeFromRequestDestination(Dest::kImage));
  EXPECT_EQ(ResourceType::kStylesheet,
            ResourceTypeFromRequestDestination(Dest::kStyle));
  EXPECT_EQ(ResourceType::kStylesheet,
            ResourceTypeFromRequestDestination(Dest::kXslt));
  EXPECT_EQ(ResourceType::kFont, ResourceTypeFromRequestDestination(Dest::kFont));
  EXPECT_EQ(ResourceType::kMedia, ResourceTypeFromRequestDestination(Dest::kAudio));
  EXPECT_EQ(ResourceType::kMedia, ResourceTypeFromRequestDestination(Dest::kVideo));
  EXPECT_EQ(ResourceType::kMedia, ResourceTypeFromRequestDestination(Dest::kTrack));
}

TEST(InwebRequestDestinationMapTest, FramesMapToSubdocument) {
  // kDocument reaching the engine means a non-outermost frame (throttle
  // registration exempts the outermost main frame).
  EXPECT_EQ(ResourceType::kSubdocument,
            ResourceTypeFromRequestDestination(Dest::kDocument));
  EXPECT_EQ(ResourceType::kSubdocument, ResourceTypeFromRequestDestination(Dest::kFrame));
  EXPECT_EQ(ResourceType::kSubdocument,
            ResourceTypeFromRequestDestination(Dest::kIframe));
  EXPECT_EQ(ResourceType::kSubdocument,
            ResourceTypeFromRequestDestination(Dest::kFencedframe));
}

TEST(InwebRequestDestinationMapTest, ObjectsAndEmbeds) {
  EXPECT_EQ(ResourceType::kObject, ResourceTypeFromRequestDestination(Dest::kObject));
  EXPECT_EQ(ResourceType::kObject, ResourceTypeFromRequestDestination(Dest::kEmbed));
}

TEST(InwebRequestDestinationMapTest, EverythingElseIsOtherIncludingEmpty) {
  // kEmpty covers XHR/fetch — documented v1 deviation: kXhr unreachable.
  EXPECT_EQ(ResourceType::kOther, ResourceTypeFromRequestDestination(Dest::kEmpty));
  EXPECT_EQ(ResourceType::kOther, ResourceTypeFromRequestDestination(Dest::kJson));
  EXPECT_EQ(ResourceType::kOther,
            ResourceTypeFromRequestDestination(Dest::kManifest));
  EXPECT_EQ(ResourceType::kOther,
            ResourceTypeFromRequestDestination(Dest::kSpeculationRules));
}

}  // namespace inweb::adblock
