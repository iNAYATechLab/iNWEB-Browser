// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#ifndef CHROME_ANDROID_INWEB_ADBLOCK_INWEB_DEFAULT_FILTER_LIST_H_
#define CHROME_ANDROID_INWEB_ADBLOCK_INWEB_DEFAULT_FILTER_LIST_H_

namespace inweb::adblock {

// The embedded iNWEB starter tracking-protection list (patch 0007,
// ADR-041). Loaded by InwebAdblockEngineHolder at first use, so v1 blocks
// well-known advertising/tracking hosts out of the box — no network, no
// subscription, deterministic on every device.
//
// Curation policy (conservative by design):
// - Host-anchored rules (`||host^`) only: no generic URL patterns that
//   could false-positive on first-party content.
// - Only long-established, stable ad/tracker hosts from the EasyList /
//   EasyPrivacy families. If a host also serves functional traffic for
//   common logins (e.g. connect.facebook.net), it is excluded.
// - The final section is the device-test fixture (MASTER-SPEC device
//   matrix B-5..B-8): `.invalid` TLD hosts (RFC 2606 — guaranteed
//   non-routable), so on-device blocking verification never depends on
//   third-party servers. It includes one block, one type-constrained
//   block, and one exception, exercising all three decision paths.
extern const char kInwebDefaultFilterList[];

}  // namespace inweb::adblock

#endif  // CHROME_ANDROID_INWEB_ADBLOCK_INWEB_DEFAULT_FILTER_LIST_H_
