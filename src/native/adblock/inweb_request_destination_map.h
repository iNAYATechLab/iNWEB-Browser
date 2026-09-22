// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#ifndef CHROME_ANDROID_INWEB_ADBLOCK_INWEB_REQUEST_DESTINATION_MAP_H_
#define CHROME_ANDROID_INWEB_ADBLOCK_INWEB_REQUEST_DESTINATION_MAP_H_

#include "chrome/android/inweb/adblock/inweb_resource_type.h"
#include "services/network/public/mojom/fetch_api.mojom-shared.h"

namespace inweb::adblock {

// Maps a network request's RequestDestination to the engine's ResourceType.
//
// Documented v1 mapping decisions (PHASE4 §7/§10, ADR-040):
// - Workers and worklets (kWorker, kSharedWorker, kServiceWorker,
//   kAudioWorklet, kPaintWorklet) map to kScript: they are script fetches and
//   ABP-family `$script` rules apply to them.
// - kXslt maps to kStylesheet.
// - kDocument/kFrame/kIframe/kFencedframe map to kSubdocument. The outermost
//   main frame never reaches the engine: throttle registration exempts it.
// - kEmpty maps to kOther. RequestDestination cannot distinguish XHR/fetch
//   (both are destination "empty" per the Fetch spec), so the engine's kXhr
//   type is unreachable from the network stack in v1 — a documented
//   deviation from the Kotlin reference. Rules that constrain only $xhr
//   therefore do not match in v1.
ResourceType ResourceTypeFromRequestDestination(
    network::mojom::RequestDestination destination);

}  // namespace inweb::adblock

#endif  // CHROME_ANDROID_INWEB_ADBLOCK_INWEB_REQUEST_DESTINATION_MAP_H_
