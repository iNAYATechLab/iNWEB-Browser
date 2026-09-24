// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#ifndef CHROME_ANDROID_INWEB_EXTENSIONS_INWEB_EXTENSION_ARCHIVE_H_
#define CHROME_ANDROID_INWEB_EXTENSIONS_INWEB_EXTENSION_ARCHIVE_H_

#include <cstdint>
#include <optional>
#include <string>
#include <vector>

namespace inweb::extensions {

// CRX3 container information (structural parse only — see below).
struct Crx3Info {
  bool ok = false;
  std::string error;
  // Byte offset of the embedded ZIP archive inside the package.
  size_t zip_offset = 0;
};

// Structural CRX3 parse: magic "Cr24", format version 3, 32-bit LE
// header length, protobuf header bytes, then the ZIP payload.
//
// HONEST SCOPE (PHASE6 §4 security model): v1 verifies structure and
// the ZIP's per-entry CRC32 integrity — it does NOT validate the CRX3
// signature against a key store (iNWEB ships no store; sideloaded
// packages are user-chosen local files, and the permission-warning
// review before enable is the trust surface). Identity-signature
// verification (Ed25519 over SignedData) is a documented, deferred
// extension point — never claimed as present.
Crx3Info ParseCrx3Container(const std::vector<uint8_t>& package);

// One entry from a ZIP central directory.
struct ZipEntry {
  std::string name;
  uint64_t compressed_size = 0;
  uint64_t uncompressed_size = 0;
  uint32_t crc32 = 0;
  uint16_t method = 0;  // 0 = stored, 8 = deflate
  // Offset of the local file header (needed to find the data).
  uint64_t local_header_offset = 0;
};

// Minimal in-memory ZIP reader (local headers + central directory,
// stored + deflate). Self-contained on zlib so it builds both in the
// Chromium tree and on the host harness without shimming Chromium's
// zip utilities. Every inflated entry is CRC32-verified — corrupt
// payloads fail with an error instead of returning garbage.
class ZipMemoryReader {
 public:
  explicit ZipMemoryReader(std::vector<uint8_t> data);

  // Parses the central directory. False + |error_| on malformed input.
  bool Open(std::string* error);

  // Returns and decompresses |name| (CRC-verified), or nullopt with
  // |error_| set (missing entry, bad method, corrupt data).
  std::optional<std::vector<uint8_t>> ReadEntry(const std::string& name);

  bool HasEntry(const std::string& name) const;
  const std::string& error() const { return error_; }
  const std::vector<ZipEntry>& entries() const { return entries_; }

 private:
  std::vector<uint8_t> data_;
  std::vector<ZipEntry> entries_;
  std::string error_;
};

}  // namespace inweb::extensions

#endif  // CHROME_ANDROID_INWEB_EXTENSIONS_INWEB_EXTENSION_ARCHIVE_H_
