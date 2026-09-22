#pragma once
#include <cstdio>
#include <vector>
namespace shim_gtest {
struct TestCase { const char* name; void (*fn)(); };
inline std::vector<TestCase>& Tests() { static std::vector<TestCase> t; return t; }
inline int& Failures() { static int f = 0; return f; }
inline void Report(const char* file, int line, const char* what) {
  ++Failures();
  std::printf("  FAIL %s:%d %s\n", file, line, what);
}
}  // namespace shim_gtest
#define TEST(suite, name)                                                    \
  static void suite##_##name##_impl();                                       \
  static const bool suite##_##name##_reg = [] {                              \
    shim_gtest::Tests().push_back({#suite "." #name, &suite##_##name##_impl}); \
    return true;                                                             \
  }();                                                                       \
  static void suite##_##name##_impl()
#define EXPECT_EQ(a, b) do { if (!((a) == (b))) shim_gtest::Report(__FILE__, __LINE__, "EXPECT_EQ"); } while (0)
#define EXPECT_NE(a, b) do { if (!((a) != (b))) shim_gtest::Report(__FILE__, __LINE__, "EXPECT_NE"); } while (0)
#define EXPECT_TRUE(a) do { if (!(a)) shim_gtest::Report(__FILE__, __LINE__, "EXPECT_TRUE"); } while (0)
#define EXPECT_FALSE(a) do { if (a) shim_gtest::Report(__FILE__, __LINE__, "EXPECT_FALSE"); } while (0)
#define ASSERT_EQ(a, b) do { if (!((a) == (b))) { shim_gtest::Report(__FILE__, __LINE__, "ASSERT_EQ"); return; } } while (0)
#define ASSERT_NE(a, b) do { if (!((a) != (b))) { shim_gtest::Report(__FILE__, __LINE__, "ASSERT_NE"); return; } } while (0)
#define ASSERT_TRUE(a) do { if (!(a)) { shim_gtest::Report(__FILE__, __LINE__, "ASSERT_TRUE"); return; } } while (0)
