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

// Bounds-checked little-endian reads. Every offset is validated with
// RangeFits() before use, and all access goes through vector/string
// operator[] — no raw pointer arithmetic or C-array indexing anywhere
// in this file (unsafe-buffers-clean; hop-17 lesson).

uint16_t ReadU16At(const std::vector<uint8_t>& d, size_t off) {
  return static_cast<uint16_t>(d[off] | (d[off + 1] << 8));
}

uint32_t ReadU32At(const std::vector<uint8_t>& d, size_t off) {
  return static_cast<uint32_t>(d[off]) |
         (static_cast<uint32_t>(d[off + 1]) << 8) |
         (static_cast<uint32_t>(d[off + 2]) << 16) |
         (static_cast<uint32_t>(d[off + 3]) << 24);
}

// True when [off, off+len) lies fully inside d (overflow-safe).
bool RangeFits(const std::vector<uint8_t>& d, size_t off, size_t len) {
  return off <= d.size() && len <= d.size() - off;
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
  const uint32_t version = ReadU32At(package, 4);
  if (version != kCrxVersion3) {
    info.error = "unsupported CRX version (only 3 is handled)";
    return info;
  }
  const uint32_t header_size = ReadU32At(package, 8);
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

ZipMemoryReader::~ZipMemoryReader() = default;

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
      if (ReadU32At(data_, i) == kEocdSig) {
        eocd = i;
        break;
      }
    }
    if (eocd == std::string::npos || !RangeFits(data_, eocd, 22)) {
      error_ = "end-of-central-directory not found";
    } else {
      const uint64_t entry_count = ReadU16At(data_, eocd + 10);
      const uint64_t cd_offset = ReadU32At(data_, eocd + 16);
      size_t p = static_cast<size_t>(cd_offset);
      for (uint64_t i = 0; i < entry_count; ++i) {
        if (!RangeFits(data_, p, 46) || ReadU32At(data_, p) != kCdhSig) {
          error_ = "corrupt central directory";
          break;
        }
        ZipEntry entry;
        entry.method = static_cast<uint16_t>(ReadU16At(data_, p + 10));
        entry.crc32 = ReadU32At(data_, p + 16);
        entry.compressed_size = ReadU32At(data_, p + 20);
        entry.uncompressed_size = ReadU32At(data_, p + 24);
        entry.local_header_offset = ReadU32At(data_, p + 42);
        const uint16_t name_len =
            static_cast<uint16_t>(ReadU16At(data_, p + 28));
        const uint16_t extra_len =
            static_cast<uint16_t>(ReadU16At(data_, p + 30));
        const uint16_t comment_len =
            static_cast<uint16_t>(ReadU16At(data_, p + 32));
        if (!RangeFits(data_, p + 46, name_len)) {
          error_ = "corrupt entry name";
          break;
        }
        entry.name.resize(name_len);
        for (size_t k = 0; k < name_len; ++k) {
          entry.name[k] = static_cast<char>(data_[p + 46 + k]);
        }
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
  if (!RangeFits(data_, lfh, 30) || ReadU32At(data_, lfh) != kLfhSig) {
    error_ = "bad local header for " + name;
    return std::nullopt;
  }
  const uint16_t name_len = static_cast<uint16_t>(ReadU16At(data_, lfh + 26));
  const uint16_t extra_len = static_cast<uint16_t>(ReadU16At(data_, lfh + 28));
  const size_t data_offset = lfh + 30 + name_len + extra_len;
  if (!RangeFits(data_, data_offset, found->compressed_size)) {
    error_ = "entry data out of bounds: " + name;
    return std::nullopt;
  }
  std::vector<uint8_t> out(static_cast<size_t>(found->uncompressed_size));
  if (found->method == 0) {  // stored
    if (found->compressed_size != found->uncompressed_size) {
      error_ = "stored entry size mismatch: " + name;
      return std::nullopt;
    }
    out.assign(data_.begin() + data_offset,
               data_.begin() + data_offset + out.size());
  } else if (found->method == 8) {  // deflate (raw)
    if (out.empty()) {
      // An empty entry with method 8 is legal and means zero bytes.
    } else {
      // zlib's C API needs a raw pointer into the middle of data_.
      // Copy the bounded compressed slice into a local buffer so this
      // file contains no pointer arithmetic (unsafe-buffers-clean);
      // extension-package slices are small, the copy is negligible.
      std::vector<uint8_t> compressed(
          data_.begin() + data_offset,
          data_.begin() + data_offset + found->compressed_size);
      z_stream stream = {};
      stream.next_in = compressed.data();
      stream.avail_in = static_cast<uInt>(compressed.size());
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
