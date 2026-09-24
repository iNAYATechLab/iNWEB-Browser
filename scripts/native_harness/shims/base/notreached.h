// Shim for base/notreached.h.
// Upstream @154: NOTREACHED(...) is [[noreturn]] (NOTREACHED_NORETURN was
// removed); keep the shim a faithful one-macro mirror.
#pragma once
#include <cstdlib>

#define NOTREACHED(...) std::abort()
