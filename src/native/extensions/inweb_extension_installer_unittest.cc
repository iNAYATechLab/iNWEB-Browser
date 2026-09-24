// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// iNWEB Browser — extension installer unit tests (PHASE6 §6.3):
// CRX3 structural parse, ZIP reading (stored + deflate, CRC-verified
// corruption), manifest subset parse (required fields, unicode,
// escapes, duplicates), version comparison, package inspection
// (valid/invalid/corrupt CRX per the design), and the sideload update
// decision.

#include "chrome/android/inweb/extensions/inweb_extension_installer.h"

#include <cstring>

#include "chrome/android/inweb/extensions/inweb_extension_archive.h"
#include "testing/gtest/include/gtest/gtest.h"
#include "third_party/zlib/zlib.h"

namespace inweb::extensions {

namespace {

// ---- tiny in-test ZIP builder (store + deflate via zlib) -----------

std::vector<uint8_t> U32(uint32_t v) {
  return {static_cast<uint8_t>(v), static_cast<uint8_t>(v >> 8),
          static_cast<uint8_t>(v >> 16), static_cast<uint8_t>(v >> 24)};
}
std::vector<uint8_t> U16(uint16_t v) {
  return {static_cast<uint8_t>(v), static_cast<uint8_t>(v >> 8)};
}
void Append(std::vector<uint8_t>* dst, const std::vector<uint8_t>& src) {
  dst->insert(dst->end(), src.begin(), src.end());
}
void Append(std::vector<uint8_t>* dst, const std::string& s) {
  dst->insert(dst->end(), s.begin(), s.end());
}

struct ZipFile {
  std::string name;
  std::string content;
  bool deflate = false;
};

std::vector<uint8_t> BuildZip(const std::vector<ZipFile>& files) {
  std::vector<uint8_t> out;
  struct Central {
    std::string name;
    uint32_t crc, comp_size, uncomp_size, offset;
    uint16_t method;
  };
  std::vector<Central> centrals;
  for (const ZipFile& f : files) {
    const uint32_t offset = static_cast<uint32_t>(out.size());
    const uint32_t crc = static_cast<uint32_t>(
        crc32(0, reinterpret_cast<const Bytef*>(f.content.data()),
              static_cast<uInt>(f.content.size())));
    std::vector<uint8_t> data(f.content.begin(), f.content.end());
    uint16_t method = 0;
    if (f.deflate) {
      // ZIP entries carry RAW deflate streams (no zlib wrapper), so
      // the builder emits raw deflate to match what real archivers
      // produce — and what the reader's inflateInit2(-15) expects.
      uLongf bound = compressBound(f.content.size());
      std::vector<uint8_t> comp(bound);
      uLongf comp_size = bound;
      z_stream zs = {};
      zs.next_in = const_cast<Bytef*>(
          reinterpret_cast<const Bytef*>(f.content.data()));
      zs.avail_in = static_cast<uInt>(f.content.size());
      zs.next_out = comp.data();
      zs.avail_out = static_cast<uInt>(bound);
      if (deflateInit2(&zs, 9, Z_DEFLATED, -15, 8, Z_DEFAULT_STRATEGY) ==
          Z_OK) {
        deflate(&zs, Z_FINISH);
        comp_size = static_cast<uLongf>(zs.total_out);
        deflateEnd(&zs);
      }
      comp.resize(comp_size);
      data = std::move(comp);
      method = 8;
    }
    Append(&out, U32(0x04034b50));
    Append(&out, U16(20));    // version needed
    Append(&out, U16(0));     // flags
    Append(&out, U16(method));
    Append(&out, U16(0));     // time
    Append(&out, U16(0));     // date
    Append(&out, U32(crc));
    Append(&out, U32(static_cast<uint32_t>(data.size())));
    Append(&out, U32(static_cast<uint32_t>(f.content.size())));
    Append(&out, U16(static_cast<uint16_t>(f.name.size())));
    Append(&out, U16(0));     // extra len
    Append(&out, f.name);
    Append(&out, data);
    centrals.push_back({f.name, crc, static_cast<uint32_t>(data.size()),
                        static_cast<uint32_t>(f.content.size()), offset, method});
  }
  const uint32_t cd_offset = static_cast<uint32_t>(out.size());
  for (const Central& c : centrals) {
    Append(&out, U32(0x02014b50));
    Append(&out, U16(20));    // version made by
    Append(&out, U16(20));    // version needed
    Append(&out, U16(0));     // flags
    Append(&out, U16(c.method));
    Append(&out, U16(0));     // time
    Append(&out, U16(0));     // date
    Append(&out, U32(c.crc));
    Append(&out, U32(c.comp_size));
    Append(&out, U32(c.uncomp_size));
    Append(&out, U16(static_cast<uint16_t>(c.name.size())));
    Append(&out, U16(0));     // extra
    Append(&out, U16(0));     // comment
    Append(&out, U16(0));     // disk
    Append(&out, U16(0));     // internal attrs
    Append(&out, U32(0));     // external attrs
    Append(&out, U32(c.offset));
    Append(&out, c.name);
  }
  const uint32_t cd_size = static_cast<uint32_t>(out.size()) - cd_offset;
  Append(&out, U32(0x06054b50));
  Append(&out, U16(0));       // disk
  Append(&out, U16(0));       // cd disk
  Append(&out, U16(static_cast<uint16_t>(centrals.size())));
  Append(&out, U16(static_cast<uint16_t>(centrals.size())));
  Append(&out, U32(cd_size));
  Append(&out, U32(cd_offset));
  Append(&out, U16(0));       // comment len
  return out;
}

std::vector<uint8_t> WrapCrx3(const std::vector<uint8_t>& zip,
                              const std::vector<uint8_t>& header) {
  std::vector<uint8_t> out = {'C', 'r', '2', '4'};
  Append(&out, U32(3));
  Append(&out, U32(static_cast<uint32_t>(header.size())));
  Append(&out, header);
  Append(&out, zip);
  return out;
}

const char* kManifestV3 =
    "{\n"
    "  \"name\": \"Example Blocker\",\n"
    "  \"version\": \"1.2.0\",\n"
    "  \"manifest_version\": 3,\n"
    "  \"permissions\": [\"storage\", \"declarativeNetRequest\", \"tabs\"]\n"
    "}\n";

}  // namespace

TEST(Crx3ContainerTest, ParsesWellFormedContainer) {
  const std::vector<uint8_t> zip = BuildZip({{"manifest.json", kManifestV3}});
  const Crx3Info crx = ParseCrx3Container(WrapCrx3(zip, {1, 2, 3, 4}));
  ASSERT_TRUE(crx.ok);  // error: "%s" (see below)
  EXPECT_EQ(16u, crx.zip_offset);  // 12-byte header prefix + 4-byte proto stub
}

TEST(Crx3ContainerTest, RejectsBadMagicVersionAndBounds) {
  EXPECT_FALSE(ParseCrx3Container({'P', 'K', '3', '4', 0, 0, 0, 0}).ok);
  std::vector<uint8_t> zip = BuildZip({{"manifest.json", kManifestV3}});
  std::vector<uint8_t> v2 = {'C', 'r', '2', '4'};
  Append(&v2, U32(2));
  Append(&v2, U32(4));
  Append(&v2, std::vector<uint8_t>{1, 2, 3, 4});
  Append(&v2, zip);
  EXPECT_FALSE(ParseCrx3Container(v2).ok);
  std::vector<uint8_t> huge = {'C', 'r', '2', '4'};
  Append(&huge, U32(3));
  Append(&huge, U32(0xFFFFFF));  // header larger than the file
  Append(&huge, std::vector<uint8_t>(zip));
  EXPECT_FALSE(ParseCrx3Container(huge).ok);
}

TEST(ZipMemoryReaderTest, ReadsStoredAndDeflateEntries) {
  const std::string long_text(5000, 'x');
  const std::vector<uint8_t> zip =
      BuildZip({{"manifest.json", std::string(kManifestV3), /*deflate=*/true},
                {"raw.txt", "hello", /*deflate=*/false}});
  ZipMemoryReader reader(zip);
  ASSERT_TRUE(reader.Open(nullptr));
  auto manifest = reader.ReadEntry("manifest.json");
  ASSERT_TRUE(manifest);
  EXPECT_EQ(std::string(kManifestV3),
            std::string(manifest->begin(), manifest->end()));
  auto raw = reader.ReadEntry("raw.txt");
  ASSERT_TRUE(raw);
  EXPECT_EQ("hello", std::string(raw->begin(), raw->end()));
  EXPECT_FALSE(reader.ReadEntry("missing.txt").has_value());
  (void)long_text;
}

TEST(ZipMemoryReaderTest, CorruptionFailsLoudly) {
  std::vector<uint8_t> zip = BuildZip({{"a.txt", "hello world"}});
  // Corrupt one payload byte (inside the stored entry data).
  zip[40] ^= 0xFF;
  ZipMemoryReader reader(std::move(zip));
  ASSERT_TRUE(reader.Open(nullptr));
  EXPECT_FALSE(reader.ReadEntry("a.txt").has_value());  // CRC mismatch
  EXPECT_NE("", reader.error());
}

TEST(ManifestParserTest, ParsesRequiredFieldsAndPermissions) {
  const ExtensionManifest m = ParseManifest(kManifestV3);
  ASSERT_FALSE(m.parse_error.has_value());
  EXPECT_EQ("Example Blocker", m.name);
  EXPECT_EQ("1.2.0", m.version);
  EXPECT_EQ(3, m.manifest_version);
  EXPECT_EQ(3u, m.permissions.size());
  EXPECT_EQ("storage", m.permissions[0]);
  EXPECT_EQ("declarativeNetRequest", m.permissions[1]);
}

TEST(ManifestParserTest, HandlesEscapesUnicodeAndDuplicates) {
  const ExtensionManifest m = ParseManifest(
      "{\"name\": \"A\\\"B\\\\C\\u0986\\u09AE\", \"version\": \"1\", "
      "\"version\": \"2\", \"manifest_version\": 2}");
  ASSERT_FALSE(m.parse_error.has_value());
  EXPECT_EQ("A\"B\\C\xe0\xa6\x86\xe0\xa6\xae", m.name);  // UTF-8 Bengali
  EXPECT_EQ("2", m.version);  // duplicate key: last wins
  EXPECT_EQ(2, m.manifest_version);
}

TEST(ManifestParserTest, RejectsMalformedJson) {
  EXPECT_TRUE(ParseManifest("{\"name\": ").parse_error.has_value());
  EXPECT_TRUE(ParseManifest("[1,2,3").parse_error.has_value());
  EXPECT_TRUE(ParseManifest("{\"name\": \"a\"} trailing")
                  .parse_error.has_value());
}

TEST(VersionTest, ChromeVersionOrdering) {
  EXPECT_EQ(0, *CompareVersions("1.2.0", "1.2"));
  EXPECT_EQ(-1, *CompareVersions("1.2.0", "1.2.1"));
  EXPECT_EQ(1, *CompareVersions("1.10", "1.9"));
  EXPECT_EQ(1, *CompareVersions("2.0.0.1", "2.0.0.0"));
  EXPECT_EQ(-1, *CompareVersions("1", "1.0.0.1"));
  EXPECT_FALSE(CompareVersions("1.x", "1.0").has_value());
  EXPECT_FALSE(CompareVersions("", "1.0").has_value());
  EXPECT_FALSE(CompareVersions("1.2.3.4.5", "1.0").has_value());
}

TEST(InspectPackageTest, AcceptsValidZipAndCrx3) {
  const std::vector<uint8_t> zip = BuildZip({{"manifest.json", kManifestV3}});
  InspectionResult plain = InspectPackage(zip);
  EXPECT_EQ(PackageVerdict::kValid, plain.verdict);
  EXPECT_FALSE(plain.is_crx);
  EXPECT_EQ("Example Blocker", plain.manifest.name);
  EXPECT_EQ(3u, plain.manifest.permissions.size());

  InspectionResult crx = InspectPackage(WrapCrx3(zip, {9, 9}));
  ASSERT_EQ(PackageVerdict::kValid, crx.verdict);
  EXPECT_TRUE(crx.is_crx);
}

TEST(InspectPackageTest, ClassifiesCorruptAndNotAnExtension) {
  // Garbage container.
  EXPECT_EQ(PackageVerdict::kCorrupt,
            InspectPackage({'n', 'o', 't', 'a', 'p', 'a', 'c', 'k'}).verdict);
  // ZIP without manifest.json.
  EXPECT_EQ(PackageVerdict::kNotAnExtension,
            InspectPackage(BuildZip({{"readme.txt", "hi"}})).verdict);
  // Manifest missing required fields.
  EXPECT_EQ(
      PackageVerdict::kNotAnExtension,
      InspectPackage(BuildZip({{"manifest.json", "{\"name\": \"x\"}"}}))
          .verdict);
  // Unsupported manifest_version.
  EXPECT_EQ(PackageVerdict::kNotAnExtension,
            InspectPackage(BuildZip({{"manifest.json",
                                      "{\"name\": \"x\", \"version\": \"1\", "
                                      "\"manifest_version\": 1}"}}))
                .verdict);
  // Corrupt ZIP payload (CRC failure on manifest read).
  std::vector<uint8_t> bad = BuildZip({{"manifest.json", kManifestV3}});
  bad[45] ^= 0xFF;  // inside stored manifest data
  const InspectionResult corrupt = InspectPackage(bad);
  EXPECT_TRUE(corrupt.verdict == PackageVerdict::kCorrupt ||
              corrupt.verdict == PackageVerdict::kNotAnExtension);
  EXPECT_NE("", corrupt.error);
}

TEST(UpdateDecisionTest, SideloadVersionPolicy) {
  EXPECT_EQ(UpdateDecision::kAcceptFirstInstall, DecideUpdate({}, "1.0"));
  EXPECT_EQ(UpdateDecision::kAcceptNewerVersion,
            DecideUpdate("1.2.0", "1.3"));
  EXPECT_EQ(UpdateDecision::kRejectOlderVersion,
            DecideUpdate("1.2.0", "1.2.0"));
  EXPECT_EQ(UpdateDecision::kRejectOlderVersion, DecideUpdate("2.0", "1.9"));
}

}  // namespace inweb::extensions
