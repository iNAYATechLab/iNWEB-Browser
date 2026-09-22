// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// iNWEB Browser — filter-list parser (implementation).

#include "chrome/android/inweb/adblock/inweb_filter_list_parser.h"

#include <string>
#include <utility>
#include <vector>

#include "base/no_destructor.h"
#include "base/strings/string_split.h"
#include "base/strings/string_util.h"
#include "chrome/android/inweb/adblock/inweb_pattern_compiler.h"

namespace inweb::adblock {

namespace {

bool IsBlank(const std::string& s) {
  return base::TrimString(s, base::kWhitespaceASCII, base::TRIM_ALL).empty();
}

}  // namespace

// static
ParsedFilterList FilterListParser::Parse(const std::string& text,
                                         const std::string& list_id) {
  ParsedFilterList parsed;
  parsed.list_id = list_id;

  std::vector<std::string> lines =
      base::SplitString(text, "\n", base::TRIM_WHITESPACE, base::SPLIT_WANT_ALL);

  int line_number = 0;
  for (const std::string& raw_line : lines) {
    ++line_number;
    const std::string& line = raw_line;  // already trimmed by SplitString
    if (line.empty()) {
      continue;
    }
    if (line[0] == '!' || line[0] == '[') {
      ++parsed.comment_count;
      continue;
    }
    if (line.find("##") != std::string::npos ||
        line.find("#@#") != std::string::npos ||
        line.find("#?#") != std::string::npos) {
      ++parsed.cosmetic_rule_count;
      continue;
    }
    std::optional<NetworkFilterRule> rule = ParseRule(line, line_number);
    if (!rule) {
      ++parsed.invalid_rule_count;
    } else if (rule->unsupported) {
      ++parsed.unsupported_rule_count;
    } else {
      parsed.network_rules.push_back(std::move(*rule));
    }
  }
  return parsed;
}

// static
std::optional<NetworkFilterRule> FilterListParser::ParseRule(
    const std::string& line,
    int line_number) {
  std::string body = line;
  const bool is_exception = base::StartsWith(body, "@@");
  if (is_exception) {
    body = body.substr(2);
  }

  FilterOptions options;
  bool unsupported = false;
  const size_t dollar = body.rfind('$');
  // `dollar == 0` means the pattern part is empty ("$script" options-only
  // body) — an invalid rule; the empty-body check below rejects it.
  if (dollar != std::string::npos) {
    OptionsResult result = ParseOptions(body.substr(dollar + 1));
    if (result.kind == OptionsResultKind::kInvalid) {
      return std::nullopt;
    }
    if (result.kind == OptionsResultKind::kUnsupported) {
      unsupported = true;
    } else {
      options = std::move(result.options);
    }
    body = body.substr(0, dollar);
  }
  if (body.empty()) {
    return std::nullopt;
  }

  bool domain_anchor = false;
  bool anchor_start = false;
  bool anchor_end = false;
  if (base::StartsWith(body, "||")) {
    domain_anchor = true;
    body = body.substr(2);
  } else if (base::StartsWith(body, "|")) {
    anchor_start = true;
    body = body.substr(1);
  }
  if (body.size() > 1 && body.back() == '|') {
    anchor_end = true;
    body.pop_back();
  }
  if (body.empty()) {
    return std::nullopt;
  }

  NetworkFilterRule rule;
  rule.raw = line;
  rule.pattern = body;
  rule.is_exception = is_exception;
  rule.domain_anchor = domain_anchor;
  rule.anchor_start = anchor_start;
  rule.anchor_end = anchor_end;
  rule.options = std::move(options);
  rule.unsupported = unsupported;
  rule.line = line_number;
  rule.regex =
      PatternCompiler::Compile(body, domain_anchor, anchor_start, anchor_end);
  if (!rule.regex) {
    return std::nullopt;
  }
  return rule;
}

// static
const std::map<std::string, ResourceType>& FilterListParser::TypeTokens() {
  static const base::NoDestructor<std::map<std::string, ResourceType>> tokens(
      std::map<std::string, ResourceType>{
          {"script", ResourceType::kScript},
          {"image", ResourceType::kImage},
          {"stylesheet", ResourceType::kStylesheet},
          {"xhr", ResourceType::kXhr},
          {"xmlhttprequest", ResourceType::kXhr},
          {"subdocument", ResourceType::kSubdocument},
          {"object", ResourceType::kObject},
          {"websocket", ResourceType::kWebsocket},
          {"popup", ResourceType::kPopup},
          {"document", ResourceType::kDocument},
          {"media", ResourceType::kMedia},
          {"font", ResourceType::kFont},
          {"other", ResourceType::kOther},
      });
  return *tokens;
}

// static
FilterListParser::OptionsResult FilterListParser::ParseOptions(
    const std::string& text) {
  OptionsResult result;
  if (IsBlank(text)) {
    return result;  // kInvalid
  }

  std::set<ResourceType>& types = result.options.types;
  std::set<ResourceType>& negated_types = result.options.negated_types;
  std::optional<bool>& third_party = result.options.third_party;
  std::map<std::string, bool>& domains = result.options.domains;

  // WANT_ALL mirrors the Kotlin split(','): empty tokens fall through to
  // the unsupported branch.
  std::vector<std::string> tokens =
      base::SplitString(text, ",", base::TRIM_WHITESPACE, base::SPLIT_WANT_ALL);
  for (const std::string& token : tokens) {
    const std::string t = base::ToLowerASCII(token);
    if (t == "third-party") {
      third_party = true;
    } else if (t == "first-party") {
      third_party = false;
    } else if (t == "~third-party") {
      third_party = false;
    } else if (t == "~first-party") {
      third_party = true;
    } else if (base::StartsWith(t, "domain=")) {
      if (!ParseDomains(t.substr(7), &domains)) {
        return OptionsResult();  // kInvalid
      }
    } else if (TypeTokens().count(t) != 0) {
      types.insert(TypeTokens().at(t));
    } else if (base::StartsWith(t, "~") && TypeTokens().count(t.substr(1)) != 0) {
      negated_types.insert(TypeTokens().at(t.substr(1)));
    } else if (t == "match-case") {
      // Ignored by design (Kotlin parity).
    } else {
      result.kind = OptionsResultKind::kUnsupported;
      return result;
    }
  }
  result.kind = OptionsResultKind::kOk;
  return result;
}

// static
bool FilterListParser::ParseDomains(const std::string& text,
                                    std::map<std::string, bool>* into) {
  if (IsBlank(text)) {
    return false;
  }
  std::vector<std::string> entries =
      base::SplitString(text, "|", base::TRIM_WHITESPACE, base::SPLIT_WANT_ALL);
  for (const std::string& entry : entries) {
    const std::string e = base::ToLowerASCII(entry);
    if (e.empty()) {
      return false;
    }
    if (e[0] == '~') {
      const std::string domain = e.substr(1);
      if (domain.empty()) {
        return false;
      }
      (*into)[domain] = false;
    } else {
      (*into)[e] = true;
    }
  }
  return true;
}

}  // namespace inweb::adblock
