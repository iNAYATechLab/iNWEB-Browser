#pragma once
#include <openssl/sha.h>
#include <string>
namespace crypto {
// Host-test shim over crypto::SHA256HashString (real openssl).
inline std::string SHA256HashString(const std::string& input) {
  unsigned char digest[SHA256_DIGEST_LENGTH];
  SHA256(reinterpret_cast<const unsigned char*>(input.data()), input.size(),
         digest);
  return std::string(reinterpret_cast<char*>(digest), SHA256_DIGEST_LENGTH);
}
}  // namespace crypto
