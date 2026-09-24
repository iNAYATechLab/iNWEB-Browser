// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#include "chrome/android/inweb/extensions/inweb_extension_archive.h"

#include <cstring>

#include "third_party/zlib/zlib.h"

namespace inweb::extensions {

namespace {

constexpr uint8_t kCrxMagic[] = {'C', 'r', '2', '4'};
constexpr uint32_t kCrxVersion3 = 3;

uint32_t ReadU16(const uint8_t* p) {
  return static_cast<uint32_t>(p[0]) | (static_cast<uint32_t>(p[1]) << 8);
}

uint32_t ReadU32(const uint8_t* p) {
  return static_cast<uint32_t>(p[0]) | (static_cast<uint32_t>(p[1]) << 8) |
         (static_cast<uint32_t>(p[2]) << 16) |
         (static_cast<uint32_t>(p[3]) << 24);
}

}  // namespace

Crx3Info ParseCrx3Container(const std::vector<uint8_t>& package) {
  Crx3Info info;
  if (package.size() < 12) {
    info.error = "package too small for a CRX header";
    return info;
  }
  if (std::memcmp(package.data(), kCrxMagic, sizeof(kCrxMagic)) != 0) {
    info.error = "bad CRX magic";
    return info;
  }
  const uint32_t version = ReadU32(package.data() + 4);
  if (version != kCrxVersion3) {
    info.error = "unsupported CRX version (only 3 is handled)";
    return info;
  }
  const uint32_t header_size = ReadU32(package.data() + 8);
  if (header_size == 0 ||
      static_cast<uint64_t>(header_size) + 12 >= package.size()) {
    info.error = "CRX header length out of bounds";
    return info;
  }
  info.zip_offset = 12 + static_cast<size_t>(header_size);
  info.ok = true;
  return info;
}

ZipMemoryReader::ZipMemoryReader(std::vector<uint8_t> data)
    : data_(std::move(data)) {}

namespace {

// End-of-central-directory signature.
constexpr uint32_t kEocdSig = 0x06054b50;
// Central-directory file-header signature.
constexpr uint32_t kCdhSig = 0x02014b50;
// Local file-header signature.
constexpr uint32_t kLfhSig = 0x04034b50;
// Maximum tolerated comment/search window for the EOCD.
constexpr size_t kEocdSearchLimit = 65536;

}  // namespace

bool ZipMemoryReader::Open(std::string* error) {
  entries_.clear();
  error_.clear();
  // Find the EOCD record scanning backwards (ZIP64 not supported —
  // documented limit; extension packages never approach 4 GB).
  if (data_.size() < 22) {
    error_ = "not a ZIP (too small)";
  } else {
    const size_t search_start =
        data_.size() > kEocdSearchLimit ? data_.size() - kEocdSearchLimit : 0;
    size_t eocd = std::string::npos;
    for (size_t i = data_.size() - 22 + 1; i-- > search_start;) {
      if (ReadU32(data_.data() + i) == kEocdSig) {
        eocd = i;
        break;
      }
    }
    if (eocd == std::string::npos) {
      error_ = "end-of-central-directory not found";
    } else {
      const uint64_t entry_count = ReadU16(data_.data() + eocd + 10);
      const uint64_t cd_offset = ReadU32(data_.data() + eocd + 16);
      size_t p = static_cast<size_t>(cd_offset);
      for (uint64_t i = 0; i < entry_count; ++i) {
        if (p + 46 > data_.size() || ReadU32(data_.data() + p) != kCdhSig) {
          error_ = "corrupt central directory";
          break;
        }
        ZipEntry entry;
        entry.method = static_cast<uint16_t>(ReadU16(data_.data() + p + 10));
        entry.crc32 = ReadU32(data_.data() + p + 16);
        entry.compressed_size = ReadU32(data_.data() + p + 20);
        entry.uncompressed_size = ReadU32(data_.data() + p + 24);
        entry.local_header_offset = ReadU32(data_.data() + p + 42);
        const uint16_t name_len = static_cast<uint16_t>(ReadU16(data_.data() + p + 28));
        const uint16_t extra_len = static_cast<uint16_t>(ReadU16(data_.data() + p + 30));
        const uint16_t comment_len = static_cast<uint16_t>(ReadU16(data_.data() + p + 32));
        if (p + 46 + name_len > data_.size()) {
          error_ = "corrupt entry name";
          break;
        }
        entry.name.assign(reinterpret_cast<const char*>(data_.data() + p + 46),
                          name_len);
        entries_.push_back(std::move(entry));
        p += 46 + name_len + extra_len + comment_len;
      }
    }
  }
  if (!error_.empty()) {
    entries_.clear();
    if (error) {
      *error = error_;
    }
    return false;
  }
  return true;
}

bool ZipMemoryReader::HasEntry(const std::string& name) const {
  for (const ZipEntry& entry : entries_) {
    if (entry.name == name) {
      return true;
    }
  }
  return false;
}

std::optional<std::vector<uint8_t>> ZipMemoryReader::ReadEntry(
    const std::string& name) {
  error_.clear();
  const ZipEntry* found = nullptr;
  for (const ZipEntry& entry : entries_) {
    if (entry.name == name) {
      found = &entry;
      break;
    }
  }
  if (!found) {
    error_ = "entry not found: " + name;
    return std::nullopt;
  }
  // Validate the local header at the recorded offset.
  const size_t lfh = static_cast<size_t>(found->local_header_offset);
  if (lfh + 30 > data_.size() || ReadU32(data_.data() + lfh) != kLfhSig) {
    error_ = "bad local header for " + name;
    return std::nullopt;
  }
  const uint16_t name_len = static_cast<uint16_t>(ReadU16(data_.data() + lfh + 26));
  const uint16_t extra_len = static_cast<uint16_t>(ReadU16(data_.data() + lfh + 28));
  const size_t data_offset = lfh + 30 + name_len + extra_len;
  if (data_offset + found->compressed_size > data_.size()) {
    error_ = "entry data out of bounds: " + name;
    return std::nullopt;
  }
  const uint8_t* src = data_.data() + data_offset;
  std::vector<uint8_t> out(static_cast<size_t>(found->uncompressed_size));
  if (found->method == 0) {  // stored
    if (found->compressed_size != found->uncompressed_size) {
      error_ = "stored entry size mismatch: " + name;
      return std::nullopt;
    }
    std::memcpy(out.data(), src, out.size());
  } else if (found->method == 8) {  // deflate (raw)
    if (out.empty()) {
      // An empty entry with method 8 is legal and means zero bytes.
    } else {
      z_stream stream = {};
      stream.next_in = const_cast<Bytef*>(src);
      stream.avail_in = static_cast<uInt>(found->compressed_size);
      stream.next_out = out.data();
      stream.avail_out = static_cast<uInt>(out.size());
      if (inflateInit2(&stream, -15) != Z_OK ||  // raw deflate
          inflate(&stream, Z_FINISH) != Z_STREAM_END) {
        inflateEnd(&stream);
        error_ = "deflate failed for " + name;
        return std::nullopt;
      }
      inflateEnd(&stream);
    }
  } else {
    error_ = "unsupported compression method for " + name;
    return std::nullopt;
  }
  // CRC32 integrity — corruption fails loudly, never returns garbage.
  const uint32_t actual =
      static_cast<uint32_t>(crc32(0, out.data(), static_cast<uInt>(out.size())));
  if (actual != found->crc32) {
    error_ = "CRC mismatch for " + name;
    return std::nullopt;
  }
  return out;
}

}  // namespace inweb::extensions
