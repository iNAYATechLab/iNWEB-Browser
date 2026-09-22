// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#include "base/notreached.h"
#include "chrome/android/inweb/adblock/inweb_request_destination_map.h"

namespace inweb::adblock {

ResourceType ResourceTypeFromRequestDestination(
    network::mojom::RequestDestination destination) {
  switch (destination) {
    case network::mojom::RequestDestination::kScript:
    case network::mojom::RequestDestination::kWorker:
    case network::mojom::RequestDestination::kSharedWorker:
    case network::mojom::RequestDestination::kServiceWorker:
    case network::mojom::RequestDestination::kAudioWorklet:
    case network::mojom::RequestDestination::kPaintWorklet:
      return ResourceType::kScript;
    case network::mojom::RequestDestination::kImage:
      return ResourceType::kImage;
    case network::mojom::RequestDestination::kStyle:
    case network::mojom::RequestDestination::kXslt:
      return ResourceType::kStylesheet;
    case network::mojom::RequestDestination::kDocument:
    case network::mojom::RequestDestination::kFrame:
    case network::mojom::RequestDestination::kIframe:
    case network::mojom::RequestDestination::kFencedframe:
      return ResourceType::kSubdocument;
    case network::mojom::RequestDestination::kObject:
    case network::mojom::RequestDestination::kEmbed:
      return ResourceType::kObject;
    case network::mojom::RequestDestination::kAudio:
    case network::mojom::RequestDestination::kVideo:
    case network::mojom::RequestDestination::kTrack:
      return ResourceType::kMedia;
    case network::mojom::RequestDestination::kFont:
      return ResourceType::kFont;
    case network::mojom::RequestDestination::kEmpty:
    case network::mojom::RequestDestination::kManifest:
    case network::mojom::RequestDestination::kReport:
    case network::mojom::RequestDestination::kWebBundle:
    case network::mojom::RequestDestination::kWebIdentity:
    case network::mojom::RequestDestination::kCompressionDictionary:
    case network::mojom::RequestDestination::kSpeculationRules:
    case network::mojom::RequestDestination::kJson:
    case network::mojom::RequestDestination::kEmailVerification:
    case network::mojom::RequestDestination::kText:
      return ResourceType::kOther;
  }
  NOTREACHED_NORETURN();
}

}  // namespace inweb::adblock
