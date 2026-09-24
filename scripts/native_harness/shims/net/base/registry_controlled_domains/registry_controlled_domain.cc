#include "net/base/registry_controlled_domains/registry_controlled_domain.h"
#include <set>
#include <sstream>
#include <vector>
namespace net::registry_controlled_domains {
std::string GetDomainAndRegistry(const std::string& host, PrivateRegistryFilter) {
  static const std::set<std::string> kMulti = {
      "co.uk","org.uk","ac.uk","gov.uk","me.uk","net.uk",
      "com.bd","net.bd","org.bd","ac.bd","gov.bd","edu.bd","mil.bd",
      "co.jp","or.jp","ne.jp","ac.jp","go.jp","com.au","net.au","org.au","edu.au",
      "co.in","net.in","org.in","ac.in","co.nz","com.br","com.mx","com.tr","com.cn",
      "com.tw","co.kr","co.id","com.my","com.ph","com.vn","com.ar","com.co","com.pe",
      "com.eg","co.za","com.ng","com.pk","com.sa"};
  std::string h = host;
  while (!h.empty() && h.back() == '.') h.pop_back();
  if (h.empty() || h.find('.') == std::string::npos) return "";
  bool ipv4 = true;
  for (char c : h) if (!(isdigit(static_cast<unsigned char>(c)) || c == '.')) { ipv4 = false; break; }
  if (ipv4) return "";
  std::istringstream ss(h);
  std::vector<std::string> labels; std::string lab;
  while (std::getline(ss, lab, '.')) if (!lab.empty()) labels.push_back(lab);
  if (labels.size() <= 2) return h;
  std::string lastTwo = labels[labels.size() - 2] + "." + labels[labels.size() - 1];
  if (kMulti.count(lastTwo)) return labels[labels.size() - 3] + "." + lastTwo;
  return lastTwo;
}
}  // namespace net::registry_controlled_domains
