// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#include "chrome/android/inweb/extensions/inweb_extension_installer.h"

#include <cctype>
#include <map>

#include "chrome/android/inweb/extensions/inweb_extension_archive.h"

namespace inweb::extensions {

namespace {

// ---- Minimal JSON subset parser -------------------------------------
// Objects, arrays, strings (with \" \\ \/ \b \f \n \r \t \uXXXX),
// integer literals, true/false/null. Everything else is an error.
// Written for the manifest fields iNWEB reads; it is NOT a general
// JSON engine and says so.

class JsonParser {
 public:
  explicit JsonParser(const std::string& text) : text_(text) {}

  bool Parse(std::string* error) {
    SkipWhitespace();
    if (!ParseValue()) {
      *error = "JSON parse error at offset " + std::to_string(pos_);
      return false;
    }
    SkipWhitespace();  // tolerate trailing whitespace/newline
    if (pos_ != text_.size()) {
      *error = "JSON parse error at offset " + std::to_string(pos_);
      return false;
    }
    return true;
  }

  const std::map<std::string, std::string>& strings() const {
    return strings_;
  }
  const std::map<std::string, long>& ints() const { return ints_; }

  // Path accessors: "permissions" is an array of strings under a
  // top-level key; "permissions.3" is its fourth element.
  std::vector<std::string> StringArray(const std::string& key) const {
    std::vector<std::string> out;
    for (size_t i = 0;; ++i) {
      auto it = strings_.find(key + "." + std::to_string(i));
      if (it == strings_.end()) {
        break;
      }
      out.push_back(it->second);
    }
    return out;
  }

 private:
  void SkipWhitespace() {
    while (pos_ < text_.size() &&
           std::isspace(static_cast<unsigned char>(text_[pos_]))) {
      ++pos_;
    }
  }

  bool Consume(char c) {
    if (pos_ < text_.size() && text_[pos_] == c) {
      ++pos_;
      return true;
    }
    return false;
  }

  bool ParseValue(const std::string& path = "") {
    SkipWhitespace();
    if (pos_ >= text_.size()) {
      return false;
    }
    const char c = text_[pos_];
    if (c == '{') {
      ++pos_;
      if (Consume('}')) {
        return true;
      }
      while (true) {
        SkipWhitespace();
        std::string key;
        if (!ParseString(&key)) {
          return false;
        }
        SkipWhitespace();
        if (!Consume(':')) {
          return false;
        }
        const std::string child =
            path.empty() ? key : path + "." + key;
        if (!ParseValue(child)) {
          return false;
        }
        SkipWhitespace();
        if (Consume(',')) {
          continue;
        }
        return Consume('}');
      }
    }
    if (c == '[') {
      ++pos_;
      if (Consume(']')) {
        return true;
      }
      size_t index = 0;
      while (true) {
        const std::string child =
            path.empty() ? std::to_string(index)
                         : path + "." + std::to_string(index);
        if (!ParseValue(child)) {
          return false;
        }
        ++index;
        SkipWhitespace();
        if (Consume(',')) {
          continue;
        }
        return Consume(']');
      }
    }
    if (c == '"') {
      std::string value;
      if (!ParseString(&value)) {
        return false;
      }
      strings_[path] = value;
      return true;
    }
    if (std::isdigit(static_cast<unsigned char>(c)) || c == '-') {
      size_t start = pos_;
      ++pos_;
      while (pos_ < text_.size() &&
             std::isdigit(static_cast<unsigned char>(text_[pos_]))) {
        ++pos_;
      }
      ints_[path] = std::stol(text_.substr(start, pos_ - start));
      return true;
    }
    if (text_.compare(pos_, 4, "true") == 0) {
      pos_ += 4;
      strings_[path] = "true";
      return true;
    }
    if (text_.compare(pos_, 5, "false") == 0) {
      pos_ += 5;
      strings_[path] = "false";
      return true;
    }
    if (text_.compare(pos_, 4, "null") == 0) {
      pos_ += 4;
      return true;
    }
    return false;
  }

  bool ParseString(std::string* out) {
    if (!Consume('"')) {
      return false;
    }
    out->clear();
    while (pos_ < text_.size()) {
      const char c = text_[pos_++];
      if (c == '"') {
        return true;
      }
      if (c != '\\') {
        out->push_back(c);
        continue;
      }
      if (pos_ >= text_.size()) {
        return false;
      }
      const char esc = text_[pos_++];
      switch (esc) {
        case '"': out->push_back('"'); break;
        case '\\': out->push_back('\\'); break;
        case '/': out->push_back('/'); break;
        case 'b': out->push_back('\b'); break;
        case 'f': out->push_back('\f'); break;
        case 'n': out->push_back('\n'); break;
        case 'r': out->push_back('\r'); break;
        case 't': out->push_back('\t'); break;
        case 'u': {
          if (pos_ + 4 > text_.size()) {
            return false;
          }
          const std::string hex = text_.substr(pos_, 4);
          pos_ += 4;
          for (char h : hex) {
            if (!std::isxdigit(static_cast<unsigned char>(h))) {
              return false;
            }
          }
          // BMP only (no surrogate pairs — documented limit).
          const uint32_t code = static_cast<uint32_t>(std::stoul(hex, nullptr, 16));
          if (code < 0x80) {
            out->push_back(static_cast<char>(code));
          } else if (code < 0x800) {
            out->push_back(static_cast<char>(0xC0 | (code >> 6)));
            out->push_back(static_cast<char>(0x80 | (code & 0x3F)));
          } else {
            out->push_back(static_cast<char>(0xE0 | (code >> 12)));
            out->push_back(static_cast<char>(0x80 | ((code >> 6) & 0x3F)));
            out->push_back(static_cast<char>(0x80 | (code & 0x3F)));
          }
          break;
        }
        default:
          return false;
      }
    }
    return false;  // unterminated string
  }

  std::string text_;
  size_t pos_ = 0;
  std::map<std::string, std::string> strings_;
  std::map<std::string, long> ints_;
};

std::optional<std::vector<long>> VersionComponents(const std::string& v) {
  std::vector<long> parts;
  size_t start = 0;
  while (true) {
    const size_t dot = v.find('.', start);
    const std::string part =
        dot == std::string::npos ? v.substr(start) : v.substr(start, dot - start);
    if (part.empty() || part.size() > 9) {
      return std::nullopt;
    }
    for (char c : part) {
      if (!std::isdigit(static_cast<unsigned char>(c))) {
        return std::nullopt;
      }
    }
    parts.push_back(std::stol(part));
    if (dot == std::string::npos) {
      break;
    }
    start = dot + 1;
  }
  if (parts.empty() || parts.size() > 4) {
    return std::nullopt;
  }
  return parts;
}

}  // namespace

ExtensionManifest ParseManifest(const std::string& json) {
  ExtensionManifest manifest;
  JsonParser parser(json);
  std::string error;
  if (!parser.Parse(&error)) {
    manifest.parse_error = error;
    return manifest;
  }
  manifest.name = parser.strings().count("name") ? parser.strings().at("name") : "";
  manifest.version =
      parser.strings().count("version") ? parser.strings().at("version") : "";
  manifest.manifest_version =
      parser.ints().count("manifest_version") ? static_cast<int>(parser.ints().at("manifest_version")) : 0;
  manifest.permissions = parser.StringArray("permissions");
  return manifest;
}

std::optional<int> CompareVersions(const std::string& a, const std::string& b) {
  const auto ca = VersionComponents(a);
  const auto cb = VersionComponents(b);
  if (!ca || !cb) {
    return std::nullopt;
  }
  for (size_t i = 0; i < 4; ++i) {
    const long x = i < ca->size() ? (*ca)[i] : 0;
    const long y = i < cb->size() ? (*cb)[i] : 0;
    if (x < y) {
      return -1;
    }
    if (x > y) {
      return 1;
    }
  }
  return 0;
}

InspectionResult InspectPackage(const std::vector<uint8_t>& package) {
  InspectionResult result;

  std::vector<uint8_t> zip_bytes;
  const Crx3Info crx = ParseCrx3Container(package);
  if (crx.ok) {
    result.is_crx = true;
    zip_bytes.assign(package.begin() + static_cast<long>(crx.zip_offset),
                     package.end());
  } else if (package.size() >= 4 && package[0] == 'P' && package[1] == 'K') {
    zip_bytes = package;
  } else {
    result.error = "neither a CRX3 package nor a ZIP archive: " + crx.error;
    return result;
  }

  ZipMemoryReader reader(std::move(zip_bytes));
  std::string zip_error;
  if (!reader.Open(&zip_error)) {
    result.error = "corrupt archive: " + zip_error;
    return result;
  }
  if (!reader.HasEntry("manifest.json")) {
    result.verdict = PackageVerdict::kNotAnExtension;
    result.error = "no manifest.json — not an extension package";
    return result;
  }
  auto manifest_bytes = reader.ReadEntry("manifest.json");
  if (!manifest_bytes) {
    result.verdict = PackageVerdict::kCorrupt;
    result.error = "manifest.json unreadable: " + reader.error();
    return result;
  }
  result.raw_manifest.assign(manifest_bytes->begin(), manifest_bytes->end());

  result.manifest = ParseManifest(result.raw_manifest);
  if (result.manifest.parse_error) {
    result.verdict = PackageVerdict::kNotAnExtension;
    result.error = "manifest.json: " + *result.manifest.parse_error;
    return result;
  }
  if (result.manifest.name.empty()) {
    result.verdict = PackageVerdict::kNotAnExtension;
    result.error = "manifest.json: missing required field \"name\"";
    return result;
  }
  if (!VersionComponents(result.manifest.version)) {
    result.verdict = PackageVerdict::kNotAnExtension;
    result.error = "manifest.json: missing or invalid \"version\"";
    return result;
  }
  if (result.manifest.manifest_version != 2 &&
      result.manifest.manifest_version != 3) {
    result.verdict = PackageVerdict::kNotAnExtension;
    result.error = "manifest.json: unsupported manifest_version (need 2 or 3)";
    return result;
  }
  result.verdict = PackageVerdict::kValid;
  return result;
}

UpdateDecision DecideUpdate(const std::optional<std::string>& installed,
                            const std::string& incoming) {
  if (!installed) {
    return UpdateDecision::kAcceptFirstInstall;
  }
  const auto cmp = CompareVersions(*installed, incoming);
  if (cmp && *cmp < 0) {
    return UpdateDecision::kAcceptNewerVersion;
  }
  return UpdateDecision::kRejectOlderVersion;
}

}  // namespace inweb::extensions
