// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// iNWEB Browser — ad-block resource types.
//
// Ported 1:1 from the tested Kotlin reference implementation
// (src/core/tracking-protection/…/ResourceType.kt).
// EasyList option tokens map onto these values (see FilterListParser).

#ifndef CHROME_ANDROID_INWEB_ADBLOCK_INWEB_RESOURCE_TYPE_H_
#define CHROME_ANDROID_INWEB_ADBLOCK_INWEB_RESOURCE_TYPE_H_

namespace inweb::adblock {

// Resource types a network request can have (EasyList option tokens).
enum class ResourceType {
  kDocument,
  kScript,
  kImage,
  kStylesheet,
  kXhr,
  kSubdocument,
  kObject,
  kWebsocket,
  kPopup,
  kMedia,
  kFont,
  kOther,
};

}  // namespace inweb::adblock

#endif  // CHROME_ANDROID_INWEB_ADBLOCK_INWEB_RESOURCE_TYPE_H_
