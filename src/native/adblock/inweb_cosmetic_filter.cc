// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#include "chrome/android/inweb/adblock/inweb_cosmetic_filter.h"

#include <algorithm>
#include <cstring>

#include "base/strings/string_split.h"
#include "base/strings/string_util.h"

namespace inweb::adblock {

namespace {

// Marker candidates, earliest occurrence wins (Kotlin parity).
struct Marker {
  const char* text;
  CosmeticLineResult::Kind kind;
  bool is_exception;
};

const Marker kMarkers[] = {
    {"##", CosmeticLineResult::Kind::kHide, false},
    {"#@#", CosmeticLineResult::Kind::kException, true},
    {"#?#", CosmeticLineResult::Kind::kProcedural, false},
    {"#$#", CosmeticLineResult::Kind::kUnsupported, false},
    {"#%#", CosmeticLineResult::Kind::kUnsupported, false},
};

// Returns null on malformed domain lists (e.g. empty entries).
bool ParseDomains(const std::string& domain_part,
                  std::map<std::string, bool>* domains) {
  if (domain_part.empty() ||
      base::TrimWhitespaceASCII(domain_part, base::TRIM_ALL).empty()) {
    return true;  // blank = applies everywhere
  }
  for (const std::string& entry : base::SplitString(
           domain_part, ",", base::TRIM_WHITESPACE,
           base::SPLIT_WANT_ALL)) {
    std::string e = base::ToLowerASCII(entry);
    if (e.empty()) {
      return false;
    }
    if (e[0] == '~') {
      std::string domain = e.substr(1);
      if (domain.empty()) {
        return false;
      }
      (*domains)[domain] = false;
    } else {
      (*domains)[e] = true;
    }
  }
  return true;
}

}  // namespace

CosmeticLineResult CosmeticFilterParser::ParseLine(const std::string& line) {
  CosmeticLineResult result;
  const std::string trimmed(
      base::TrimWhitespaceASCII(line, base::TRIM_ALL));
  if (trimmed.empty() || trimmed[0] == '!' || trimmed[0] == '[') {
    return result;  // kNotCosmetic (comments handled by the caller)
  }

  // Earliest marker wins.
  const Marker* best = nullptr;
  size_t best_index = std::string::npos;
  for (const Marker& marker : kMarkers) {
    const size_t index = trimmed.find(marker.text);
    if (index != std::string::npos && index < best_index) {
      best = &marker;
      best_index = index;
    }
  }
  if (!best) {
    return result;  // kNotCosmetic: a network rule or plain text
  }

  const std::string domain_part = trimmed.substr(0, best_index);
  const std::string selector(base::TrimWhitespaceASCII(
      trimmed.substr(best_index + strlen(best->text)), base::TRIM_ALL));
  if (selector.empty()) {
    result.kind = CosmeticLineResult::Kind::kInvalid;
    return result;
  }
  switch (best->kind) {
    case CosmeticLineResult::Kind::kProcedural:
    case CosmeticLineResult::Kind::kUnsupported:
      result.kind = best->kind;
      return result;
    default:
      break;
  }
  result.rule.raw = trimmed;
  result.rule.selector = selector;
  if (!ParseDomains(domain_part, &result.rule.domains)) {
    result.kind = CosmeticLineResult::Kind::kInvalid;
    result.rule = CosmeticRule();
    return result;
  }
  result.rule.is_exception = best->is_exception;
  result.kind = best->kind;
  return result;
}

ParsedCosmeticList CosmeticFilterParser::Parse(const std::string& text,
                                               const std::string& list_id) {
  ParsedCosmeticList parsed;
  parsed.list_id = list_id;
  for (const std::string& raw_line : base::SplitString(
           text, "\n", base::TRIM_WHITESPACE, base::SPLIT_WANT_ALL)) {
    const std::string line(
        base::TrimWhitespaceASCII(raw_line, base::TRIM_ALL));
    if (line.empty()) {
      continue;
    }
    if (line[0] == '!' || line[0] == '[') {
      ++parsed.comment_count;
      continue;
    }
    const CosmeticLineResult result = ParseLine(line);
    switch (result.kind) {
      case CosmeticLineResult::Kind::kHide:
      case CosmeticLineResult::Kind::kException:
        parsed.rules.push_back(result.rule);
        break;
      case CosmeticLineResult::Kind::kProcedural:
        ++parsed.procedural_rule_count;
        break;
      case CosmeticLineResult::Kind::kUnsupported:
        ++parsed.unsupported_rule_count;
        break;
      case CosmeticLineResult::Kind::kInvalid:
        ++parsed.invalid_rule_count;
        break;
      case CosmeticLineResult::Kind::kNotCosmetic:
        break;  // network rule — not this parser's concern
    }
  }
  return parsed;
}

CosmeticFilterEngine::CosmeticFilterEngine(std::vector<CosmeticRule> rules)
    : rules_(std::move(rules)) {}

CosmeticFilterEngine::CosmeticFilterEngine(CosmeticFilterEngine&&) = default;
CosmeticFilterEngine& CosmeticFilterEngine::operator=(CosmeticFilterEngine&&) =
    default;
CosmeticFilterEngine::~CosmeticFilterEngine() = default;

std::string CosmeticFilterEngine::HideCssFor(const std::string& host) const {
  std::string h = base::ToLowerASCII(std::string(
      base::TrimWhitespaceASCII(host, base::TRIM_ALL)));
  if (!h.empty() && h.back() == '.') {
    h.pop_back();
  }
  if (h.empty()) {
    return "";
  }

  // Linked-set semantics: first occurrence order preserved, duplicates
  // collapse (Kotlin linkedSetOf parity).
  std::vector<std::string> hidden;
  std::vector<std::string> cancelled;
  auto add_unique = [](std::vector<std::string>* set,
                       const std::string& value) {
    if (std::find(set->begin(), set->end(), value) == set->end()) {
      set->push_back(value);
    }
  };
  for (const CosmeticRule& rule : rules_) {
    if (!DomainsMatch(rule.domains, h)) {
      continue;
    }
    if (rule.is_exception) {
      add_unique(&cancelled, rule.selector);
    } else {
      add_unique(&hidden, rule.selector);
    }
  }
  std::vector<std::string> active;
  for (const std::string& selector : hidden) {
    if (std::find(cancelled.begin(), cancelled.end(), selector) ==
        cancelled.end()) {
      active.push_back(selector);
    }
  }
  if (active.empty()) {
    return "";
  }
  std::string css;
  for (size_t i = 0; i < active.size(); ++i) {
    if (i > 0) {
      css += ", ";
    }
    css += active[i];
  }
  css += " { display: none !important; }";
  return css;
}

bool CosmeticFilterEngine::DomainsMatch(
    const std::map<std::string, bool>& domains,
    const std::string& host) {
  for (const auto& [domain, included] : domains) {
    if (!included && HostMatchesDomain(host, domain)) {
      return false;  // excluded
    }
  }
  bool has_includes = false;
  for (const auto& [domain, included] : domains) {
    if (included) {
      has_includes = true;
      if (HostMatchesDomain(host, domain)) {
        return true;
      }
    }
  }
  return !has_includes;
}

bool CosmeticFilterEngine::HostMatchesDomain(const std::string& host,
                                             const std::string& domain) {
  return host == domain ||
         (host.size() > domain.size() &&
          host.compare(host.size() - domain.size(), domain.size(), domain) ==
              0 &&
          host[host.size() - domain.size() - 1] == '.');
}


// Out-of-line ctor/dtor definitions (chromium-style fallout fix).
CosmeticRule::CosmeticRule() = default;
CosmeticRule::~CosmeticRule() = default;

ParsedCosmeticList::ParsedCosmeticList() = default;
ParsedCosmeticList::~ParsedCosmeticList() = default;


CosmeticRule::CosmeticRule(CosmeticRule&&) = default;
CosmeticRule& CosmeticRule::operator=(CosmeticRule&&) = default;
ParsedCosmeticList::ParsedCosmeticList(ParsedCosmeticList&&) = default;
ParsedCosmeticList& ParsedCosmeticList::operator=(ParsedCosmeticList&&) = default;


CosmeticRule::CosmeticRule(const CosmeticRule&) = default;
CosmeticRule& CosmeticRule::operator=(const CosmeticRule&) = default;
ParsedCosmeticList::ParsedCosmeticList(const ParsedCosmeticList&) = default;
ParsedCosmeticList& ParsedCosmeticList::operator=(const ParsedCosmeticList&) = default;

}  // namespace inweb::adblock
