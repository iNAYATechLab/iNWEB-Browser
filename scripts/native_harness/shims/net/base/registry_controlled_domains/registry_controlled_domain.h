#pragma once
#include <string>
// NOTE: upstream takes base::StringPiece; the harness keeps std::string
// (call-site compatible) — path + enum-class scoping are the fidelity bits.
namespace net::registry_controlled_domains {
enum class PrivateRegistryFilter { EXCLUDE_PRIVATE_REGISTRIES, INCLUDE_PRIVATE_REGISTRIES };
// Host-test shim: Kotlin DomainClassifier parity (multi-part suffix table +
// last-two-labels fallback + IPv4 passthrough). The real build uses
// Chromium's full PSL.
std::string GetDomainAndRegistry(const std::string& host, PrivateRegistryFilter filter);
}  // namespace net::registry_controlled_domains
