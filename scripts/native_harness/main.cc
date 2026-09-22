#include "testing/gtest/include/gtest/gtest.h"
int main() {
  int ran = 0;
  for (auto& t : shim_gtest::Tests()) {
    int before = shim_gtest::Failures();
    std::printf("[ RUN ] %s\n", t.name);
    t.fn();
    ++ran;
    if (shim_gtest::Failures() == before) std::printf("[ OK  ] %s\n", t.name);
  }
  std::printf("\n== ran %d tests, %d failure(s) ==\n", ran, shim_gtest::Failures());
  return shim_gtest::Failures() == 0 ? 0 : 1;
}
