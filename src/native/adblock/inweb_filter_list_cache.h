// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#ifndef CHROME_ANDROID_INWEB_ADBLOCK_INWEB_FILTER_LIST_CACHE_H_
#define CHROME_ANDROID_INWEB_ADBLOCK_INWEB_FILTER_LIST_CACHE_H_

#include <map>
#include <optional>
#include <string>

#include "base/files/file_path.h"

namespace inweb::adblock {

struct FilterListSource;

// Cache metadata persisted beside each cached list body. Ported from the
// Kotlin reference (lists/FilterListCache.kt).
struct FilterListMetadata {
  FilterListMetadata();
  FilterListMetadata(std::string source_id, int64_t downloaded_at_ms);
  ~FilterListMetadata();
  FilterListMetadata(const FilterListMetadata&);
  FilterListMetadata& operator=(const FilterListMetadata&);
  FilterListMetadata(FilterListMetadata&&);
  FilterListMetadata& operator=(FilterListMetadata&&);

  std::string source_id;
  // When this content was last successfully downloaded or confirmed (304).
  int64_t downloaded_at_ms = 0;
  std::optional<std::string> etag;
  std::optional<std::string> last_modified;
  // `! Version:` header parsed from the list text, if present.
  std::optional<std::string> version;
  // Network rules parsed from the stored body at store time.
  int rule_count = 0;
  // SHA-256 of the stored body, pinned at download time (G-07). A cached
  // copy whose body no longer matches is never served. Null = stored
  // before pinning existed (legacy copy) — verified as-is.
  std::optional<std::string> content_sha256;

  // Wire format (tab-separated, "iNWEB-FILTERLIST-META v=1" header) —
  // byte-compatible with the Kotlin reference implementation.
  std::string Serialize() const;
  // Returns nullopt for anything not matching the exact format (corrupt).
  static std::optional<FilterListMetadata> Deserialize(
      const std::string& text);
};

// Port: raw filter-list cache keyed by source id.
class FilterListCache {
 public:
  virtual ~FilterListCache() = default;

  virtual void Store(const FilterListSource& source,
                     const std::string& body,
                     const FilterListMetadata& metadata) = 0;
  virtual std::optional<std::string> LoadBody(
      const FilterListSource& source) = 0;
  virtual std::optional<FilterListMetadata> LoadMetadata(
      const FilterListSource& source) = 0;
  virtual void Remove(const FilterListSource& source) = 0;
  virtual void Clear() = 0;
};

// In-memory cache for tests and engine-less previews (no file system).
class MemoryFilterListCache : public FilterListCache {
 public:
  MemoryFilterListCache();
  ~MemoryFilterListCache() override;
  MemoryFilterListCache(const MemoryFilterListCache&) = delete;
  MemoryFilterListCache& operator=(const MemoryFilterListCache&) = delete;
  MemoryFilterListCache(MemoryFilterListCache&&) = delete;
  MemoryFilterListCache& operator=(MemoryFilterListCache&&) = delete;

  void Store(const FilterListSource& source,
             const std::string& body,
             const FilterListMetadata& metadata) override;
  std::optional<std::string> LoadBody(
      const FilterListSource& source) override;
  std::optional<FilterListMetadata> LoadMetadata(
      const FilterListSource& source) override;
  void Remove(const FilterListSource& source) override;
  void Clear() override;

 private:
  std::map<std::string, std::string> bodies_;
  std::map<std::string, FilterListMetadata> metadata_;
};

// Real file-backed cache: one directory, two files per source
// (`<id>.txt` body + `<id>.meta` metadata). Writes are atomic (temp file +
// rename), mirroring the session-persistence strategy (ADR-011). A
// corrupt metadata file is reported as missing — the body stays usable;
// the manager re-downloads on the next refresh. Ported from the Kotlin
// reference (lists/FileFilterListCache.kt).
class FileFilterListCache : public FilterListCache {
 public:
  explicit FileFilterListCache(base::FilePath directory);

  void Store(const FilterListSource& source,
             const std::string& body,
             const FilterListMetadata& metadata) override;
  std::optional<std::string> LoadBody(
      const FilterListSource& source) override;
  std::optional<FilterListMetadata> LoadMetadata(
      const FilterListSource& source) override;
  void Remove(const FilterListSource& source) override;
  void Clear() override;

 private:
  base::FilePath BodyPath(const std::string& id) const;
  base::FilePath MetaPath(const std::string& id) const;
  static void WriteAtomically(const base::FilePath& target,
                              const std::string& content);

  base::FilePath directory_;
};

}  // namespace inweb::adblock

#endif  // CHROME_ANDROID_INWEB_ADBLOCK_INWEB_FILTER_LIST_CACHE_H_
